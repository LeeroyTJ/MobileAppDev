package com.example.mobileappdev;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

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
    private TextView tvRegisterLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        if (SessionManager.getInstance(this).isLoggedIn()) {
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
            return;
        }

        bindViews();

        btnSignIn.setOnClickListener(v -> attemptSignIn());
        tvRegisterLink.setOnClickListener(v ->
                startActivity(new Intent(this, RegistrationActivity.class)));
    }

    private void bindViews() {
        etStudentNumber = findViewById(R.id.etStudentNumber);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        tvRegisterLink = findViewById(R.id.tvRegisterLink);
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

        ApiClient.getAuthService(this).login(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                btnSignIn.setEnabled(true);

                if (response.isSuccessful() && response.body() != null
                        && Boolean.TRUE.equals(response.body().get("success"))) {

                    Map<?, ?> data = (Map<?, ?>) response.body().get("data");
                    String token = String.valueOf(data.get("accessToken"));
                    String role = String.valueOf(data.get("role"));
                    long accountId = ((Number) data.get("accountId")).longValue();

                    SessionManager.getInstance(SignInActivity.this).saveSession(token, accountId, role);

                    startActivity(new Intent(SignInActivity.this, ProfileActivity.class));
                    finish();
                } else {
                    Toast.makeText(SignInActivity.this, "Invalid student number or password", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                btnSignIn.setEnabled(true);
                Toast.makeText(SignInActivity.this,
                        "Couldn't reach the server — check your connection", Toast.LENGTH_LONG).show();
            }
        });
    }

    private String safeText(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }
}