package com.example.mobileappdev;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobileappdev.adapter.PendingOperationAdapter;
import com.example.mobileappdev.data.local.AppDatabase;
import com.example.mobileappdev.data.local.entity.PendingOperationEntity;
import com.example.mobileappdev.session.SessionManager;
import com.example.mobileappdev.sync.SyncWorker;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class SyncActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private AppDatabase db;

    private TextView tvLastSynced;
    private RecyclerView rvStudentRequests;
    private RecyclerView rvPendingQueue;
    private TextView tvEmptyRequests;
    private TextView tvEmptyQueue;

    private PendingOperationAdapter adapterRequests;
    private PendingOperationAdapter adapterQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = SessionManager.getInstance(this);
        if (!sessionManager.isLoggedIn()) {
            Intent intent = new Intent(this, SignInActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        db = AppDatabase.getInstance(this);
        SyncWorker.schedulePeriodic(this);

        setContentView(R.layout.activity_sync);
        bindViews();
        observeSyncQueue();
        setupListeners();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateToHome();
            }
        });
    }

    private void navigateToHome() {
        Intent intent;

        if (sessionManager.isLecturer()) {
            intent = new Intent(this, RosterActivity.class);
        } else {
            intent = new Intent(this, ProfileActivity.class);
        }

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private void bindViews() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setClickable(true);
            btnBack.setFocusable(true);
            btnBack.setOnClickListener(view -> navigateToHome());
        }

        tvLastSynced = findViewById(R.id.tvLastSynced);
        rvStudentRequests = findViewById(R.id.rvStudentRequests);
        rvPendingQueue = findViewById(R.id.rvPendingQueue);
        tvEmptyRequests = findViewById(R.id.tvEmptyRequests);
        tvEmptyQueue = findViewById(R.id.tvEmptyQueue);

        if (rvStudentRequests != null) {
            rvStudentRequests.setLayoutManager(new LinearLayoutManager(this));
            adapterRequests = new PendingOperationAdapter();
            rvStudentRequests.setAdapter(adapterRequests);
        }

        if (rvPendingQueue != null) {
            rvPendingQueue.setLayoutManager(new LinearLayoutManager(this));
            adapterQueue = new PendingOperationAdapter();
            rvPendingQueue.setAdapter(adapterQueue);
        }

        PendingOperationAdapter.OnItemClickListener listener = op -> {
            if (PendingOperationEntity.STATUS_CONFLICT.equals(op.status)
                    || PendingOperationEntity.STATUS_ACTION_REQUIRED.equals(op.status)
                    || PendingOperationEntity.STATUS_REJECTED.equals(op.status)) {
                ConflictReviewBottomSheetDialogFragment dialog =
                        ConflictReviewBottomSheetDialogFragment.newInstance(op);
                dialog.show(getSupportFragmentManager(), ConflictReviewBottomSheetDialogFragment.TAG);
            } else {
                Toast.makeText(this,
                        PendingOperationAdapter.getOperationTypeLabel(op.operationType) + " — " + PendingOperationAdapter.getStatusLabel(op.status),
                        Toast.LENGTH_SHORT).show();
            }
        };

        if (adapterRequests != null) adapterRequests.setOnItemClickListener(listener);
        if (adapterQueue != null) adapterQueue.setOnItemClickListener(listener);

        Button btnRetryAll = findViewById(R.id.btnRetryAll);
        if (btnRetryAll != null) {
            btnRetryAll.setOnClickListener(v -> {
                SyncWorker.triggerManualSync(this);
                Toast.makeText(this, "Retrying all pending sync operations...", Toast.LENGTH_SHORT).show();
                updateLastSyncedDisplay();
            });
        }
    }

    private void observeSyncQueue() {
        long accountId = sessionManager.getAccountId();
        db.pendingOperationDao().observeQueueForAccount(accountId).observe(this, operations -> {
            updateLastSyncedDisplay();

            List<PendingOperationEntity> studentRequests = new ArrayList<>();
            List<PendingOperationEntity> pendingQueue = new ArrayList<>();

            if (operations != null) {
                boolean hasConflict = false;
                for (PendingOperationEntity op : operations) {
                    if (isStudentRequest(op)) {
                        studentRequests.add(op);
                    } else {
                        pendingQueue.add(op);
                    }

                    if (PendingOperationEntity.STATUS_CONFLICT.equals(op.status)) {
                        hasConflict = true;
                    }
                }

                if (hasConflict) {
                    Toast.makeText(SyncActivity.this, "Sync conflict detected — review action required", Toast.LENGTH_SHORT).show();
                }
            }

            if (adapterRequests != null) adapterRequests.setItems(studentRequests);
            if (adapterQueue != null) adapterQueue.setItems(pendingQueue);

            if (tvEmptyRequests != null) {
                tvEmptyRequests.setVisibility(studentRequests.isEmpty() ? View.VISIBLE : View.GONE);
            }
            if (tvEmptyQueue != null) {
                tvEmptyQueue.setVisibility(pendingQueue.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    private boolean isStudentRequest(PendingOperationEntity op) {
        if (op == null || op.operationType == null) return false;
        switch (op.operationType) {
            case PendingOperationEntity.TYPE_REQUEST_GROUP_CHANGE:
            case PendingOperationEntity.TYPE_CORRECT_STUDENT_NUMBER:
            case PendingOperationEntity.TYPE_TRANSFER_GROUP:
            case PendingOperationEntity.TYPE_ASSIGN_GROUP:
                return true;
            default:
                return false;
        }
    }

    private void updateLastSyncedDisplay() {
        if (tvLastSynced == null) return;
        long lastSynced = sessionManager.getLastSyncedTime();
        tvLastSynced.setText(getFormattedLastSynced(lastSynced));
    }

    private String getFormattedLastSynced(long lastSyncedTime) {
        if (lastSyncedTime <= 0) {
            return "Last synced: Never";
        }
        long now = System.currentTimeMillis();
        CharSequence relative = DateUtils.getRelativeTimeSpanString(
                lastSyncedTime, now, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE
        );
        return "Last synced: " + relative;
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLastSyncedDisplay();
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(sessionManager.isLecturer() ? R.id.nav_more : R.id.navSync);
        }
    }

    private void setupListeners() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav == null) return;

        if (sessionManager.isLecturer()) {
            bottomNav.inflateMenu(R.menu.menu_bottom_nav);
            bottomNav.setSelectedItemId(R.id.nav_more);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_students) {
                    Intent intent = new Intent(this, RosterActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                } else if (id == R.id.nav_unassigned) {
                    Intent intent = new Intent(this, UnassignedActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                } else if (id == R.id.nav_more) {
                    MoreBottomSheetDialogFragment sheet = new MoreBottomSheetDialogFragment();
                    sheet.show(getSupportFragmentManager(), MoreBottomSheetDialogFragment.TAG);
                    return false;
                }
                return false;
            });
        } else {
            bottomNav.inflateMenu(R.menu.bottom_nav_menu);
            bottomNav.setSelectedItemId(R.id.navSync);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.navHome) {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                } else if (id == R.id.navGroup) {
                    Intent intent = new Intent(this, ProfileActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                } else if (id == R.id.navSync) {
                    return true;
                }
                return false;
            });
        }
    }
}
