package com.example.mobileappdev;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.mobileappdev.data.local.AppDatabase;
import com.example.mobileappdev.data.local.entity.PendingOperationEntity;
import com.example.mobileappdev.data.remote.ApiClient;
import com.example.mobileappdev.session.SessionManager;
import com.example.mobileappdev.sync.SyncWorker;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


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
    private boolean isLecturer = false;

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

        // For lecturers, also fetch pending number correction requests
        if (isLecturer) {
            fetchPendingNumberRequests();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isLecturer) {
            fetchPendingNumberRequests();
        }
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
        isLecturer = sessionManager.isLecturer();
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

    /**
     * Lecturer-only: fetches PENDING number correction requests from the server.
     * These don't appear in the local Room queue because students submit them
     * from their own devices. The lecturer reviews them here.
     */
    private void fetchPendingNumberRequests() {
        ApiClient.getStudentService(this).getPendingNumberCorrections().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (!response.isSuccessful() || response.body() == null
                        || !Boolean.TRUE.equals(response.body().get("success"))) {
                    return;
                }

                List<?> data = (List<?>) response.body().get("data");
                if (data == null || data.isEmpty()) {
                    return;
                }

                // Show a dialog with all pending number correction requests
                showNumberCorrectionsDialog(data);
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                // Silent fail — the rest of the sync screen still works
            }
        });
    }

    private void showNumberCorrectionsDialog(List<?> requests) {
        StringBuilder message = new StringBuilder();
        message.append("Pending student number corrections:\n\n");

        final long[] requestIds = new long[requests.size()];
        for (int i = 0; i < requests.size(); i++) {
            Map<?, ?> req = (Map<?, ?>) requests.get(i);
            long reqId = req.get("request_id") instanceof Number
                    ? ((Number) req.get("request_id")).longValue() : 0;
            requestIds[i] = reqId;
            String name = req.get("student_name") != null ? req.get("student_name").toString() : "Unknown";
            String currentNum = req.get("current_number") != null ? req.get("current_number").toString() : "?";
            String requestedNum = req.get("requested_number") != null ? req.get("requested_number").toString() : "?";

            message.append("• ").append(name)
                    .append("\n  ").append(currentNum).append(" → ").append(requestedNum)
                    .append("\n\n");
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(requests.size() + " Pending Correction Request(s)")
                .setMessage(message.toString().trim())
                .setPositiveButton("Review", (dialog, which) -> {
                    // For simplicity, handle only the first request.
                    // A more elaborate UI would list each with Approve/Reject buttons.
                    if (requestIds.length > 0) {
                        resolveFirstRequest(requestIds[0]);
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void resolveFirstRequest(long requestId) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Approve this correction?")
                .setMessage("Tap Approve to update the student's number, or Reject to dismiss.")
                .setPositiveButton("Approve", (dialog, which) -> approveCorrection(requestId))
                .setNegativeButton("Reject", (dialog, which) -> rejectCorrection(requestId))
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void approveCorrection(long requestId) {
        ApiClient.getStudentService(this).approveNumberCorrection(requestId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(SyncActivity.this, "Approved", Toast.LENGTH_SHORT).show();
                    fetchPendingNumberRequests(); // Refresh the list
                } else {
                    Toast.makeText(SyncActivity.this, "Failed to approve", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                Toast.makeText(SyncActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void rejectCorrection(long requestId) {
        ApiClient.getStudentService(this).rejectNumberCorrection(requestId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(SyncActivity.this, "Rejected", Toast.LENGTH_SHORT).show();
                    fetchPendingNumberRequests(); // Refresh the list
                } else {
                    Toast.makeText(SyncActivity.this, "Failed to reject", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                Toast.makeText(SyncActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void triggerManualSync() {
        if (syncProgressIndicator != null) syncProgressIndicator.setVisibility(View.VISIBLE);
        if (btnSyncNow != null) btnSyncNow.setEnabled(false);

        WorkManager.getInstance(getApplicationContext()).enqueue(
                new OneTimeWorkRequest.Builder(SyncWorker.class).build()
        );

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