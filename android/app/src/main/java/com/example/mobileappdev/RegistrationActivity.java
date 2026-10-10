package com.example.mobileappdev;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mobileappdev.data.remote.ApiClient;
import com.example.mobileappdev.session.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

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
    private AutoCompleteTextView actProgramme;
    private AutoCompleteTextView actLabGroup;
    private MaterialButton btnSaveRegistration;

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
    }

    private void bindViews() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> navigateToSignIn());
        }

        etFullName = findViewById(R.id.etFullName);
        etStudentNumber = findViewById(R.id.etStudentNumber);
        etPassword = findViewById(R.id.etPassword);
        actProgramme = findViewById(R.id.actProgramme);
        actLabGroup = findViewById(R.id.actLabGroup);
        btnSaveRegistration = findViewById(R.id.btnSaveRegistration);
        TextView tvSignIn = findViewById(R.id.tvSignIn);

        btnSaveRegistration.setOnClickListener(v -> attemptRegistration());
        if (tvSignIn != null) {
            tvSignIn.setOnClickListener(v -> navigateToSignIn());
        }
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

        actLabGroup.setEnabled(false);
        actLabGroup.setHint("Assign after registering");
    }

    private void attemptRegistration() {
        String name = safeText(etFullName);
        String studentNumber = safeText(etStudentNumber);
        String password = safeText(etPassword);

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

        btnSaveRegistration.setEnabled(false);

        Map<String, Object> body = new HashMap<>();
        body.put("studentNumber", studentNumber);
        body.put("name", name);
        body.put("password", password);
        body.put("programmeId", selectedProgrammeId);

        ApiClient.getAuthService(this).register(body).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                btnSaveRegistration.setEnabled(true);

                if (response.isSuccessful() && response.body() != null
                        && Boolean.TRUE.equals(response.body().get("success"))) {

                    Toast.makeText(RegistrationActivity.this,
                            "Registration successful — please sign in", Toast.LENGTH_LONG).show();

                    navigateToSignIn();
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
