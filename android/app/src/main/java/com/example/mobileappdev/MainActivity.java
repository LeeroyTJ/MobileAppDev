package com.example.mobileappdev;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mobileappdev.data.remote.ApiClient;
import com.example.mobileappdev.session.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private TextView tvStudentName;
    private TextView tvStudentNumberProgramme;
    private TextView tvLabGroupInfo;
    private MaterialButton btnLogout;
    private MaterialButton btnViewProfile;
    private MaterialButton btnViewRoster;

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionManager = SessionManager.getInstance(this);
        if (!sessionManager.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        bindViews();
        setupClickListeners();
        loadStudentData();
    }

    private void bindViews() {
        tvStudentName = findViewById(R.id.tvStudentName);
        tvStudentNumberProgramme = findViewById(R.id.tvStudentNumberProgramme);
        tvLabGroupInfo = findViewById(R.id.tvLabGroupInfo);
        btnLogout = findViewById(R.id.btnLogout);
        btnViewProfile = findViewById(R.id.btnViewProfile);
        btnViewRoster = findViewById(R.id.btnViewRoster);
    }

    private void setupClickListeners() {
        btnLogout.setOnClickListener(v -> performLogout());

        btnViewProfile.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ProfileActivity.class)));

        btnViewRoster.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, RosterActivity.class)));
    }

    private void loadStudentData() {
        if (sessionManager.isLecturer()) {
            tvStudentName.setText("Lecturer Account");
            tvStudentNumberProgramme.setText("Account ID: " + sessionManager.getAccountId() + " • Faculty");
            tvLabGroupInfo.setText("Access Level: Full Roster & Group Admin");
            return;
        }

        ApiClient.getStudentService(this).getMyProfile().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null
                        && Boolean.TRUE.equals(response.body().get("success"))) {

                    Map<?, ?> data = (Map<?, ?>) response.body().get("data");
                    if (data != null) {
                        String name = String.valueOf(data.get("student_name"));
                        String number = String.valueOf(data.get("student_number"));
                        String programme = String.valueOf(data.get("programme_name"));
                        Object groupObj = data.get("group_code");
                        String group = groupObj != null ? groupObj.toString() : "Unassigned";

                        tvStudentName.setText(name);
                        tvStudentNumberProgramme.setText(number + " • " + programme);
                        tvLabGroupInfo.setText("Lab Group: " + group);
                    }
                } else if (response.code() == 401 || response.code() == 403) {
                    performLogout();
                } else {
                    tvStudentName.setText("Student");
                    tvLabGroupInfo.setText("Lab Group: --");
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                tvStudentName.setText("Offline Mode");
                tvStudentNumberProgramme.setText("Cached Session");
                tvLabGroupInfo.setText("Lab Group: Check connection");
            }
        });
    }

    private void performLogout() {
        sessionManager.clearSession();
        Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show();
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
