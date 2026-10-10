package com.example.mobileappdev;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mobileappdev.data.local.AppDatabase;
import com.example.mobileappdev.data.local.entity.PendingOperationEntity;
import com.example.mobileappdev.session.SessionManager;
import com.example.mobileappdev.sync.SyncWorker;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SyncActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private AppDatabase db;

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
    }

    private void bindViews() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        Button btnRetryAll = findViewById(R.id.btnRetryAll);
        if (btnRetryAll != null) {
            btnRetryAll.setOnClickListener(v -> {
                SyncWorker.triggerManualSync(this);
                Toast.makeText(this, "Retrying all pending sync operations...", Toast.LENGTH_SHORT).show();
            });
        }

        View actionRequiredItem = findViewById(R.id.containerGroupTransfer);
        if (actionRequiredItem != null) {
            actionRequiredItem.setOnClickListener(v -> {
                ConflictReviewBottomSheetDialogFragment dialog = new ConflictReviewBottomSheetDialogFragment();
                dialog.show(getSupportFragmentManager(), ConflictReviewBottomSheetDialogFragment.TAG);
            });
        }
    }

    private void observeSyncQueue() {
        long accountId = sessionManager.getAccountId();
        db.pendingOperationDao().observeQueueForAccount(accountId).observe(this, operations -> {
            if (operations != null && !operations.isEmpty()) {
                boolean hasConflict = false;
                for (PendingOperationEntity op : operations) {
                    if (PendingOperationEntity.STATUS_CONFLICT.equals(op.status)) {
                        hasConflict = true;
                        break;
                    }
                }
                if (hasConflict) {
                    Toast.makeText(SyncActivity.this, "Sync conflict detected — review action required", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupListeners() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav == null) return;

        bottomNav.setSelectedItemId(R.id.navSync);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navHome) {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
                return true;
            } else if (id == R.id.navGroup) {
                startActivity(new Intent(this, RosterActivity.class));
                finish();
                return true;
            } else if (id == R.id.navSync) {
                return true;
            }
            return false;
        });
    }
}
