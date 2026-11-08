package com.example.mobileappdev;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mobileappdev.data.remote.ApiClient;
import com.example.mobileappdev.session.SessionManager;
import com.example.mobileappdev.util.NetworkUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegistrationActivity extends AppCompatActivity {

    private TextInputEditText etFullName;
    private TextInputEditText etStudentNumber;
    private TextInputEditText etPassword;
    private TextInputEditText etClaimCode;
    private AutoCompleteTextView actProgramme;
    private MaterialButton btnSaveRegistration;
    private TextInputLayout tilClaimCode;
    private View layoutDraftNotice;

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;

    // Programme code -> database id
    private static final Map<String, Integer> PROGRAMME_IDS = new LinkedHashMap<>();

    static {
        PROGRAMME_IDS.put("Computer Science (CS)", 1);
        PROGRAMME_IDS.put("Data Science (DS)", 2);
        PROGRAMME_IDS.put("Information Technology (IT)", 3);
    }

    private Integer selectedProgrammeId = null;

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

        setContentView(R.layout.activity_registration);
        bindViews();
        setupProgrammeDropdown();
        setupProgressiveClaimCode();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateButtonBasedOnNetwork();
    }

    private void bindViews() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> navigateToSignIn());
        }

        etFullName = findViewById(R.id.etFullName);
        etStudentNumber = findViewById(R.id.etStudentNumber);
        etPassword = findViewById(R.id.etPassword);
        etClaimCode = findViewById(R.id.etClaimCode);
        actProgramme = findViewById(R.id.actProgramme);
        btnSaveRegistration = findViewById(R.id.btnSaveRegistration);
        tilClaimCode = findViewById(R.id.tilClaimCode);
        layoutDraftNotice = findViewById(R.id.layoutDraftNotice);

        TextView tvSignIn = findViewById(R.id.tvSignIn);

        btnSaveRegistration.setOnClickListener(v -> attemptRegistration());

        if (tvSignIn != null) {
            tvSignIn.setOnClickListener(v -> navigateToSignIn());
        }

        // Ensure claim code is hidden by default
        if (tilClaimCode != null) {
            tilClaimCode.setVisibility(View.GONE);
        }
    }

    private void updateButtonBasedOnNetwork() {
        boolean online = NetworkUtils.isOnline(this);

        if (online) {
            btnSaveRegistration.setText("Register");
            if (layoutDraftNotice != null) layoutDraftNotice.setVisibility(View.GONE);
        } else {
            btnSaveRegistration.setText("Save Registration");
            if (layoutDraftNotice != null) layoutDraftNotice.setVisibility(View.VISIBLE);
        }
    }

    private void setupProgressiveClaimCode() {
        etStudentNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (debounceRunnable != null) {
                    debounceHandler.removeCallbacks(debounceRunnable);
                }

                String num = s != null ? s.toString().trim() : "";

                debounceRunnable = () -> {
                    if (num.length() == 9 && NetworkUtils.isOnline(RegistrationActivity.this)) {
                        checkIfClaimCodeRequired(num);
                    } else if (num.length() < 9) {
                        if (tilClaimCode != null) tilClaimCode.setVisibility(View.GONE);
                    }
                };
                debounceHandler.postDelayed(debounceRunnable, 500);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void checkIfClaimCodeRequired(String studentNumber) {
        ApiClient.getAuthService(this).checkStudent(studentNumber).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null
                        && Boolean.TRUE.equals(response.body().get("success"))) {
                    Map<?, ?> data = (Map<?, ?>) response.body().get("data");
                    if (data != null) {
                        boolean requiresClaimCode = Boolean.TRUE.equals(data.get("requiresClaimCode"));

                        if (requiresClaimCode) {
                            if (tilClaimCode != null) tilClaimCode.setVisibility(View.VISIBLE);
                        } else {
                            if (tilClaimCode != null) tilClaimCode.setVisibility(View.GONE);
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                // Fail silently, claim code stays hidden
            }
        });
    }

    private void setupProgrammeDropdown() {
        String[] items = PROGRAMME_IDS.keySet().toArray(new String[0]);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, items);
        actProgramme.setAdapter(adapter);
        actProgramme.setOnItemClickListener((parent, view, position, id) -> {
            Object selected = parent.getItemAtPosition(position);
            if (selected != null) {
                selectedProgrammeId = PROGRAMME_IDS.get(selected.toString());
            }
        });
    }

    private void attemptRegistration() {
        String name = safeText(etFullName);
        String studentNumber = safeText(etStudentNumber);
        String password = safeText(etPassword);
        String claimCode = safeText(etClaimCode);

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(studentNumber) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!studentNumber.matches("\\d{9}")) {
            Toast.makeText(this, "Student number must be exactly 9 digits", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedProgrammeId == null) {
            Toast.makeText(this, "Please select a programme", Toast.LENGTH_SHORT).show();
            return;
        }
        if (tilClaimCode != null && tilClaimCode.getVisibility() == View.VISIBLE && TextUtils.isEmpty(claimCode)) {
            Toast.makeText(this, "Claim code is required for this student number", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSaveRegistration.setEnabled(false);

        Map<String, Object> body = new HashMap<>();
        body.put("studentNumber", studentNumber);
        body.put("name", name);
        body.put("password", password);
        body.put("programmeId", selectedProgrammeId);
        if (!TextUtils.isEmpty(claimCode)) {
            body.put("claimCode", claimCode);
        }

        ApiClient.getAuthService(this).register(body).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                btnSaveRegistration.setEnabled(true);

                if (response.isSuccessful() && response.body() != null
                        && Boolean.TRUE.equals(response.body().get("success"))) {

                    // If the server returns a token, auto-login the user
                    Map<?, ?> data = (Map<?, ?>) response.body().get("data");
                    if (data != null && data.get("accessToken") != null) {
                        String token = data.get("accessToken").toString();
                        long accountId = data.get("accountId") instanceof Number ? ((Number) data.get("accountId")).longValue() : 0;
                        String role = data.get("role") != null ? data.get("role").toString() : "STUDENT";

                        SessionManager sessionManager = SessionManager.getInstance(RegistrationActivity.this);
                        sessionManager.saveSession(token, accountId, role);

                        Toast.makeText(RegistrationActivity.this, "Registration successful!", Toast.LENGTH_LONG).show();

                        Class<?> target = sessionManager.isLecturer() ? RosterActivity.class : ProfileActivity.class;
                        Intent intent = new Intent(RegistrationActivity.this, target);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(RegistrationActivity.this, "Registration successful — please sign in", Toast.LENGTH_LONG).show();
                        navigateToSignIn();
                    }
                } else {
                    Toast.makeText(RegistrationActivity.this,
                            extractErrorMessage(response), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                btnSaveRegistration.setEnabled(true);
                Toast.makeText(RegistrationActivity.this,
                        "Couldn't reach the server — check your connection", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void navigateToSignIn() {
        Intent intent = new Intent(this, SignInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private String extractErrorMessage(Response<Map<String, Object>> response) {
        try {
            if (response.body() != null) {
                Object error = response.body().get("error");
                if (error instanceof Map) {
                    Object msg = ((Map<?, ?>) error).get("message");
                    if (msg != null) return msg.toString();
                }
            }
        } catch (Exception ignored) { }
        return "Registration failed. Please try again.";
    }

    private String safeText(TextInputEditText field) {
        return field != null && field.getText() != null ? field.getText().toString().trim() : "";
    }
}