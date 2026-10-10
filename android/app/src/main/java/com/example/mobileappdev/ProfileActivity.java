package com.example.mobileappdev;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mobileappdev.data.remote.ApiClient;
import com.example.mobileappdev.session.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvAvatarInitials;
    private TextView tvProfileName;
    private TextView tvProfileStudentNumber;
    private TextView tvProfileProgramme;
    private TextView tvLabGroupValue;

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
                    startActivity(new Intent(this, StudentGroupActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.navSync) {
                    startActivity(new Intent(this, SyncActivity.class));
                    finish();
                    return true;
                }
                return false;
            });
        }
    }

    private void bindViews() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
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
            btnEditProfile.setOnClickListener(v ->
                    Toast.makeText(this, "Profile edit requested", Toast.LENGTH_SHORT).show());
        }

        View rowEditDetails = findViewById(R.id.rowEditDetails);
        if (rowEditDetails != null) {
            rowEditDetails.setOnClickListener(v ->
                    Toast.makeText(this, "Edit details requested", Toast.LENGTH_SHORT).show());
        }

        View rowRequestGroupChange = findViewById(R.id.rowRequestGroupChange);
        if (rowRequestGroupChange != null) {
            rowRequestGroupChange.setOnClickListener(v -> {
                GroupPickerBottomSheetDialogFragment dialog = new GroupPickerBottomSheetDialogFragment();
                dialog.show(getSupportFragmentManager(), GroupPickerBottomSheetDialogFragment.TAG);
            });
        }

        View rowRequestNumberCorrection = findViewById(R.id.rowRequestNumberCorrection);
        if (rowRequestNumberCorrection != null) {
            rowRequestNumberCorrection.setOnClickListener(v ->
                    Toast.makeText(this, "Student number correction request submitted", Toast.LENGTH_SHORT).show());
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

                String name = String.valueOf(data.get("student_name"));
                String number = String.valueOf(data.get("student_number"));
                String programmeName = String.valueOf(data.get("programme_name"));
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
