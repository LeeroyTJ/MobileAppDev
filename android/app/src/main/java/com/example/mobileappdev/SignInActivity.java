package com.example.mobileappdev;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mobileappdev.data.remote.ApiClient;
import com.example.mobileappdev.session.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignInActivity extends AppCompatActivity {

    private TextInputEditText etStudentNumber;
    private TextInputEditText etPassword;
    private MaterialButton btnSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager sessionManager = SessionManager.getInstance(this);
        if (sessionManager.isLoggedIn()) {
            Class<?> target = sessionManager.isLecturer() ? RosterActivity.class : ProfileActivity.class;
            Intent intent = new Intent(this, target);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_signin);
        bindViews();
    }

    private void bindViews() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        etStudentNumber = findViewById(R.id.etStudentNumber);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        TextView tvRegisterLink = findViewById(R.id.tvRegisterLink);

        btnSignIn.setOnClickListener(v -> attemptSignIn());
        if (tvRegisterLink != null) {
            tvRegisterLink.setOnClickListener(v ->
                    startActivity(new Intent(this, RegistrationActivity.class)));
        }
    }

    private void attemptSignIn() {
        String studentNumber = safeText(etStudentNumber);
        String password = safeText(etPassword);

        if (TextUtils.isEmpty(studentNumber) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Enter your student number and password", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSignIn.setEnabled(false);

        Map<String, Object> body = new HashMap<>();
        body.put("username", studentNumber);
        body.put("password", password);

        ApiClient.getAuthService(this).login(body).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                btnSignIn.setEnabled(true);

                if (response.isSuccessful() && response.body() != null
                        && Boolean.TRUE.equals(response.body().get("success"))) {

                    Map<?, ?> data = (Map<?, ?>) response.body().get("data");
                    if (data == null) {
                        Toast.makeText(SignInActivity.this, "Sign in response invalid", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String token = String.valueOf(data.get("accessToken"));
                    String role = String.valueOf(data.get("role"));
                    long accountId = data.get("accountId") instanceof Number ?
                            ((Number) data.get("accountId")).longValue() : 0;

                    SessionManager sessionManager = SessionManager.getInstance(SignInActivity.this);
                    sessionManager.saveSession(token, accountId, role);

                    Class<?> target = sessionManager.isLecturer() ? RosterActivity.class : ProfileActivity.class;
                    Intent intent = new Intent(SignInActivity.this, target);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(SignInActivity.this, "Invalid student number or password", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                btnSignIn.setEnabled(true);
                Toast.makeText(SignInActivity.this,
                        "Couldn't reach the server — check your connection", Toast.LENGTH_LONG).show();
            }
        });
    }

    private String safeText(TextInputEditText field) {
        return field != null && field.getText() != null ? field.getText().toString().trim() : "";
    }
}
