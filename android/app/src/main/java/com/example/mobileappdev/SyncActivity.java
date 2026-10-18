package com.example.mobileappdev;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.mobileappdev.data.local.AppDatabase;
import com.example.mobileappdev.data.local.entity.PendingOperationEntity;
import com.example.mobileappdev.session.SessionManager;
import com.example.mobileappdev.sync.SyncWorker;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SyncActivity
 * <p>
 * Displays real-time pending operations queue, student requests, and sync status timeline.
 * Serves Member 5's requirements for Sync Timeline and Pending Operations Queue.
 * </p>
 */
public class SyncActivity extends AppCompatActivity {

    private static final String PREF_LAST_SYNC_TIME = "last_sync_timestamp";

    private MaterialToolbar syncToolbar;
    private TextView tvLastSyncedTime;
    private TextView tvPendingCountHeader;
    private MaterialButton btnSyncNow;
    private LinearProgressIndicator syncProgressIndicator;

    private View sectionRequests;
    private RecyclerView recyclerStudentRequests;
    private RecyclerView recyclerPendingQueue;
    private View layoutEmptySync;

    private PendingOperationAdapter pendingQueueAdapter;
    private PendingOperationAdapter studentRequestsAdapter;

    private long accountId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);

        initViews();
        loadAccountId();
        loadLastSyncedTimestamp();
        setupRecyclerViews();
        observePendingQueue();
        observeStudentRequests();
    }

    private void initViews() {
        syncToolbar = findViewById(R.id.syncToolbar);
        tvLastSyncedTime = findViewById(R.id.tvLastSyncedTime);
        tvPendingCountHeader = findViewById(R.id.tvPendingCountHeader);
        btnSyncNow = findViewById(R.id.btnSyncNow);
        syncProgressIndicator = findViewById(R.id.syncProgressIndicator);

        sectionRequests = findViewById(R.id.sectionRequests);
        recyclerStudentRequests = findViewById(R.id.recyclerStudentRequests);
        recyclerPendingQueue = findViewById(R.id.recyclerPendingQueue);
        layoutEmptySync = findViewById(R.id.layoutEmptySync);

        if (syncToolbar != null) {
            syncToolbar.setNavigationOnClickListener(v -> finish());
            syncToolbar.inflateMenu(R.menu.menu_sync_toolbar);
            syncToolbar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_clear_completed) {
                    confirmClearCompleted();
                    return true;
                }
                return false;
            });
        }

        if (btnSyncNow != null) {
            btnSyncNow.setOnClickListener(v -> triggerManualSync());
        }
    }

    private void loadAccountId() {
        SessionManager sessionManager = SessionManager.getInstance(this);
        accountId = sessionManager.getAccountId();
    }

    private void loadLastSyncedTimestamp() {
        SharedPreferences prefs = getSharedPreferences("cohorthub_sync_prefs", MODE_PRIVATE);
        long lastTime = prefs.getLong(PREF_LAST_SYNC_TIME, 0);

        if (lastTime > 0) {
            CharSequence formatted = DateFormat.format("MMM d, yyyy h:mm a", new Date(lastTime));
            tvLastSyncedTime.setText(formatted);
        } else {
            tvLastSyncedTime.setText("Never");
        }
    }

    private void saveLastSyncedTimestamp() {
        SharedPreferences prefs = getSharedPreferences("cohorthub_sync_prefs", MODE_PRIVATE);
        prefs.edit().putLong(PREF_LAST_SYNC_TIME, System.currentTimeMillis()).apply();
        loadLastSyncedTimestamp();
    }

    private void setupRecyclerViews() {
        pendingQueueAdapter = new PendingOperationAdapter();
        if (recyclerPendingQueue != null) {
            recyclerPendingQueue.setLayoutManager(new LinearLayoutManager(this));
            recyclerPendingQueue.setAdapter(pendingQueueAdapter);
        }

        studentRequestsAdapter = new PendingOperationAdapter();
        if (recyclerStudentRequests != null) {
            recyclerStudentRequests.setLayoutManager(new LinearLayoutManager(this));
            recyclerStudentRequests.setAdapter(studentRequestsAdapter);
        }
    }

    private void observePendingQueue() {
        AppDatabase db = AppDatabase.getInstance(this);
        db.pendingOperationDao().observeQueueForAccount(accountId).observe(this, list -> {
            if (list != null) {
                pendingQueueAdapter.submitList(list);

                int count = list.size();
                tvPendingCountHeader.setText(count == 1 ? "1 operation pending" : count + " operations pending");

                if (count == 0 && (studentRequestsAdapter.getCurrentList() == null || studentRequestsAdapter.getCurrentList().isEmpty())) {
                    if (layoutEmptySync != null) layoutEmptySync.setVisibility(View.VISIBLE);
                } else {
                    if (layoutEmptySync != null) layoutEmptySync.setVisibility(View.GONE);
                }
            }
        });
    }

    private void observeStudentRequests() {
        AppDatabase db = AppDatabase.getInstance(this);
        db.pendingOperationDao().observeRequestsForAccount(accountId).observe(this, requests -> {
            if (requests != null && !requests.isEmpty()) {
                if (sectionRequests != null) sectionRequests.setVisibility(View.VISIBLE);
                studentRequestsAdapter.submitList(requests);
            } else {
                if (sectionRequests != null) sectionRequests.setVisibility(View.GONE);
            }
        });
    }

    private void triggerManualSync() {
        if (syncProgressIndicator != null) syncProgressIndicator.setVisibility(View.VISIBLE);
        if (btnSyncNow != null) btnSyncNow.setEnabled(false);

        // Enqueue SyncWorker
        WorkManager.getInstance(getApplicationContext()).enqueue(
                new OneTimeWorkRequest.Builder(SyncWorker.class).build()
        );

        // Simulate sync completion feedback
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            saveLastSyncedTimestamp();
            if (syncProgressIndicator != null) syncProgressIndicator.setVisibility(View.GONE);
            if (btnSyncNow != null) btnSyncNow.setEnabled(true);
            Toast.makeText(SyncActivity.this, "Sync trigger sent successfully", Toast.LENGTH_SHORT).show();
        }, 1500);
    }

    private void confirmClearCompleted() {
        if (accountId <= 0) return;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            int count = AppDatabase.getInstance(getApplicationContext())
                    .pendingOperationDao()
                    .countCompletedForAccount(accountId);

            runOnUiThread(() -> {
                if (count == 0) {
                    Toast.makeText(SyncActivity.this,
                            "Nothing to clear", Toast.LENGTH_SHORT).show();
                    return;
                }

                new MaterialAlertDialogBuilder(SyncActivity.this)
                        .setTitle("Clear completed operations?")
                        .setMessage("This will remove " + count
                                + " synced or rejected operation(s) from your history."
                                + "\n\nYour server data is not affected.")
                        .setPositiveButton("Clear", (d, w) -> doClearCompleted())
                        .setNegativeButton("Cancel", null)
                        .show();
            });
            executor.shutdown();
        });
    }

    private void doClearCompleted() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            List<PendingOperationEntity> toDelete =
                    db.pendingOperationDao().findCompletedForAccount(accountId);

            if (toDelete.isEmpty()) {
                runOnUiThread(() -> Toast.makeText(SyncActivity.this,
                        "Nothing to clear", Toast.LENGTH_SHORT).show());
                return;
            }

            db.pendingOperationDao().deleteCompletedForAccount(accountId);

            runOnUiThread(() -> {
                Snackbar.make(findViewById(android.R.id.content),
                        toDelete.size() + " operation(s) cleared",
                        Snackbar.LENGTH_LONG)
                        .setAction("Undo", v ->
                                Executors.newSingleThreadExecutor().execute(() -> {
                                    for (PendingOperationEntity op : toDelete) {
                                        db.pendingOperationDao().insert(op);
                                    }
                                }))
                        .show();
            });
        });
        executor.shutdown();
    }
}
