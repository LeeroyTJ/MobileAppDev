package com.example.mobileappdev;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mobileappdev.data.local.AppDatabase;
import com.example.mobileappdev.data.local.entity.PendingOperationEntity;
import com.example.mobileappdev.data.remote.ApiClient;
import com.example.mobileappdev.session.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvAvatarInitials;
    private TextView tvProfileName;
    private TextView tvProfileStudentNumber;
    private TextView tvProfileProgramme;
    private TextView tvLabGroupValue;

    private long myStudentId = 0;
    private int myBaseVersion = 0;

    private long lastBackPressTime = 0;
    private static final long BACK_PRESS_INTERVAL = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager sessionManager = SessionManager.getInstance(this);
        if (!sessionManager.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        // Role Guard: Student only screen
        if (sessionManager.isLecturer()) {
            Intent intent = new Intent(this, RosterActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_profile);
        bindViews();
        setupBottomNav();
        loadProfile();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackPress();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.navHome);
        }
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.navHome);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.navHome) {
                    return true;
                } else if (id == R.id.navGroup) {
                    Intent intent = new Intent(this, StudentGroupActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                } else if (id == R.id.navSync) {
                    Intent intent = new Intent(this, SyncActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                }
                return false;
            });
        }
    }

    private void bindViews() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> handleBackPress());
        }

        tvAvatarInitials = findViewById(R.id.tvAvatarInitials);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileStudentNumber = findViewById(R.id.tvProfileStudentNumber);
        tvProfileProgramme = findViewById(R.id.tvProfileProgramme);
        tvLabGroupValue = findViewById(R.id.tvLabGroupValue);
        MaterialButton btnLogout = findViewById(R.id.btnLogout);

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> performLogout());
        }

        View btnEditProfile = findViewById(R.id.btnEditProfile);
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> showEditDetailsSheet());
        }

        View rowEditDetails = findViewById(R.id.rowEditDetails);
        if (rowEditDetails != null) {
            rowEditDetails.setOnClickListener(v -> showEditDetailsSheet());
        }

        View rowRequestGroupChange = findViewById(R.id.rowRequestGroupChange);
        if (rowRequestGroupChange != null) {
            rowRequestGroupChange.setOnClickListener(v -> {
                if (myStudentId <= 0) {
                    Toast.makeText(this, "Profile not loaded yet — try again in a moment", Toast.LENGTH_SHORT).show();
                    return;
                }

                GroupPickerBottomSheetDialogFragment dialog =
                        GroupPickerBottomSheetDialogFragment.newInstance(myStudentId);
                dialog.setOnGroupSelectedListener(groupCode -> tvLabGroupValue.setText(groupCode));
                dialog.show(getSupportFragmentManager(), GroupPickerBottomSheetDialogFragment.TAG);
            });
        }

        View rowRequestNumberCorrection = findViewById(R.id.rowRequestNumberCorrection);
        if (rowRequestNumberCorrection != null) {
            rowRequestNumberCorrection.setOnClickListener(v -> showNumberCorrectionDialog());
        }
    }

    private void loadProfile() {
        ApiClient.getStudentService(this).getMyProfile().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (!response.isSuccessful() || response.body() == null
                        || !Boolean.TRUE.equals(response.body().get("success"))) {
                    handleFailure(response.code());
                    return;
                }

                Map<?, ?> data = (Map<?, ?>) response.body().get("data");
                if (data == null) {
                    handleFailure(response.code());
                    return;
                }

                Object idObj = data.get("student_id");
                myStudentId = idObj instanceof Number ? ((Number) idObj).longValue() : 0;
                Object versionObj = data.get("version");
                myBaseVersion = versionObj instanceof Number ? ((Number) versionObj).intValue() : 0;

                String name = safeStr(data.get("student_name"));
                String number = safeStr(data.get("student_number"));
                String programmeName = safeStr(data.get("programme_name"));
                Object groupCodeObj = data.get("group_code");
                String groupCode = groupCodeObj != null ? groupCodeObj.toString() : "Unassigned";

                tvProfileName.setText(name);
                tvProfileStudentNumber.setText(number);
                tvProfileProgramme.setText(programmeName);
                tvLabGroupValue.setText(groupCode);
                tvAvatarInitials.setText(initialsOf(name));
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                Toast.makeText(ProfileActivity.this,
                        "Couldn't reach the server — check your connection", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleFailure(int code) {
        if (code == 401 || code == 403) {
            new SessionExpiredDialogFragment().show(getSupportFragmentManager(), SessionExpiredDialogFragment.TAG);
        } else {
            Toast.makeText(this, "Couldn't load your profile", Toast.LENGTH_LONG).show();
        }
    }

    private void performLogout() {
        new SignOutConfirmationDialogFragment().show(getSupportFragmentManager(), SignOutConfirmationDialogFragment.TAG);
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, SignInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void handleBackPress() {
        long now = System.currentTimeMillis();
        if (now - lastBackPressTime < BACK_PRESS_INTERVAL) {
            moveTaskToBack(true);
        } else {
            lastBackPressTime = now;
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
        }
    }

    private String safeStr(Object obj) {
        if (obj == null) return "N/A";
        String s = String.valueOf(obj);
        return "null".equals(s) ? "N/A" : s;
    }

    private void showEditDetailsSheet() {
        if (myStudentId <= 0) {
            Toast.makeText(this, "Profile not loaded yet — try again in a moment", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_student_editor, null);
        EditText etName = dialogView.findViewById(R.id.et_student_name);
        EditText etNumber = dialogView.findViewById(R.id.et_student_number);

        if (etName != null) etName.setText(tvProfileName.getText());
        if (etNumber != null) {
            etNumber.setText(tvProfileStudentNumber.getText());
            etNumber.setEnabled(false);
        }

        // Hide programme and lab group fields — students can only edit their name
        View tilProgramme = dialogView.findViewById(R.id.til_programme);
        if (tilProgramme != null) tilProgramme.setVisibility(View.GONE);
        View tilLabGroup = dialogView.findViewById(R.id.til_lab_group);
        if (tilLabGroup != null) tilLabGroup.setVisibility(View.GONE);

        new AlertDialog.Builder(this)
                .setTitle("Edit My Details")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = etName != null && etName.getText() != null
                            ? etName.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(newName)) {
                        Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String, Object> body = new HashMap<>();
                    body.put("studentName", newName);
                    body.put("baseVersion", myBaseVersion);

                    ApiClient.getStudentService(this).updateStudent(myStudentId, body)
                            .enqueue(new Callback<>() {
                                @Override
                                public void onResponse(@NonNull Call<Map<String, Object>> call,
                                                       @NonNull Response<Map<String, Object>> response) {
                                    if (response.isSuccessful() && response.body() != null
                                            && Boolean.TRUE.equals(response.body().get("success"))) {
                                        tvProfileName.setText(newName);
                                        tvAvatarInitials.setText(initialsOf(newName));
                                        Object responseData = response.body().get("data");
                                        Object version = responseData instanceof Map
                                                ? ((Map<?, ?>) responseData).get("version") : null;
                                        if (version instanceof Number) {
                                            myBaseVersion = ((Number) version).intValue();
                                        }
                                        Toast.makeText(ProfileActivity.this,
                                                "Details updated", Toast.LENGTH_SHORT).show();
                                    } else if (response.code() == 409) {
                                        Toast.makeText(ProfileActivity.this,
                                                "Record was modified on the server — pull to refresh",
                                                Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(ProfileActivity.this,
                                                "Failed to update details", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(@NonNull Call<Map<String, Object>> call,
                                                      @NonNull Throwable t) {
                                    Toast.makeText(ProfileActivity.this,
                                            "Couldn't reach the server — try again later",
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showNumberCorrectionDialog() {
        if (myStudentId <= 0) {
            Toast.makeText(this, "Profile not loaded yet — try again in a moment", Toast.LENGTH_SHORT).show();
            return;
        }

        final EditText etCorrected = new EditText(this);
        etCorrected.setInputType(InputType.TYPE_CLASS_NUMBER);
        etCorrected.setHint("Enter corrected student number");
        LinearLayout container = new LinearLayout(this);
        container.setPadding(56, 32, 56, 8);
        container.addView(etCorrected);

        new AlertDialog.Builder(this)
                .setTitle("Request Student Number Correction")
                .setView(container)
                .setPositiveButton("Submit Request", (dialog, which) -> {
                    String corrected = etCorrected.getText() != null
                            ? etCorrected.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(corrected)) {
                        Toast.makeText(this, "Enter a corrected number", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    PendingOperationEntity op = new PendingOperationEntity();
                    op.operationId = UUID.randomUUID().toString();
                    op.entityId = myStudentId;
                    op.studentLocalId = null;
                    op.accountId = SessionManager.getInstance(this).getAccountId();
                    op.operationType = PendingOperationEntity.TYPE_CORRECT_STUDENT_NUMBER;
                    op.payloadJson = new JSONObject().toString();
                    try {
                        op.payloadJson = new JSONObject()
                                .put("studentId", myStudentId)
                                .put("correctedStudentNumber", corrected)
                                .put("baseVersion", myBaseVersion)
                                .toString();
                    } catch (Exception ignored) {
                        op.payloadJson = "{\"correctedStudentNumber\":\"" + corrected + "\"}";
                    }
                    op.baseVersion = myBaseVersion;
                    op.status = PendingOperationEntity.STATUS_PENDING;
                    op.createdAt = System.currentTimeMillis();

                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    executor.execute(() -> {
                        AppDatabase.getInstance(this).pendingOperationDao().insert(op);
                        runOnUiThread(() -> Toast.makeText(this,
                                "Correction request submitted — will sync when online",
                                Toast.LENGTH_SHORT).show());
                        executor.shutdown();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String initialsOf(String name) {
        if (name == null || name.trim().isEmpty() || "null".equalsIgnoreCase(name.trim())) return "?";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) sb.append(Character.toUpperCase(part.charAt(0)));
            if (sb.length() >= 2) break;
        }
        return sb.toString();
    }
}
