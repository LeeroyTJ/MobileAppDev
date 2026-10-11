package com.example.mobileappdev;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
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

        if (sessionManager.isLecturer()) {
            bottomNav.inflateMenu(R.menu.menu_bottom_nav);
            bottomNav.setSelectedItemId(R.id.nav_more);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_students) {
                    startActivity(new Intent(this, RosterActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_unassigned) {
                    startActivity(new Intent(this, UnassignedActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_more) {
                    MoreBottomSheetDialogFragment sheet = new MoreBottomSheetDialogFragment();
                    sheet.show(getSupportFragmentManager(), MoreBottomSheetDialogFragment.TAG);
                    return true;
                }
                return false;
            });
        } else {
            bottomNav.inflateMenu(R.menu.bottom_nav_menu);
            bottomNav.setSelectedItemId(R.id.navSync);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.navHome) {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.navGroup) {
                    startActivity(new Intent(this, ProfileActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.navSync) {
                    return true;
                }
                return false;
            });
        }
    }
}
