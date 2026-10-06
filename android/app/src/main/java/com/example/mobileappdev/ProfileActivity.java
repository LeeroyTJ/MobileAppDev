package com.example.mobileappdev;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mobileappdev.data.remote.ApiClient;
import com.example.mobileappdev.session.SessionManager;

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
        setContentView(R.layout.activity_profile);

        if (!SessionManager.getInstance(this).isLoggedIn()) {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }

        bindViews();
        loadProfile();
    }

    private void bindViews() {
        tvAvatarInitials = findViewById(R.id.tvAvatarInitials);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileStudentNumber = findViewById(R.id.tvProfileStudentNumber);
        tvProfileProgramme = findViewById(R.id.tvProfileProgramme);
        tvLabGroupValue = findViewById(R.id.tvLabGroupValue);
    }

    private void loadProfile() {
        ApiClient.getStudentService(this).getMyProfile().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
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
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(ProfileActivity.this,
                        "Couldn't reach the server — check your connection", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleFailure(int code) {
        if (code == 401 || code == 403) {
            SessionManager.getInstance(this).clearSession();
            startActivity(new Intent(this, SignInActivity.class));
            finish();
        } else {
            Toast.makeText(this, "Couldn't load your profile", Toast.LENGTH_LONG).show();
        }
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
