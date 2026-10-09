package com.example.mobileappdev;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mobileappdev.session.SessionManager;

public class DeleteStudentActivity extends AppCompatActivity {

    public static final String EXTRA_STUDENT_NAME = "student_name";
    public static final String EXTRA_STUDENT_NUMBER = "student_number";
    public static final String EXTRA_STUDENT_PROGRAMME = "student_programme";

    private TextView tvAvatarInitials;
    private TextView tvStudentName;
    private TextView tvStudentDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Role Guard: Lecturer only
        SessionManager sessionManager = SessionManager.getInstance(this);
        if (!sessionManager.isLoggedIn()) {
            Intent intent = new Intent(this, SignInActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        if (!sessionManager.isLecturer()) {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_delete_student);
        bindViews();
        loadStudentData();
        setupListeners();
    }

    private void bindViews() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        tvAvatarInitials = findViewById(R.id.tvAvatarInitials);
        tvStudentName = findViewById(R.id.tvStudentName);
        tvStudentDetails = findViewById(R.id.tvStudentDetails);
    }

    private void loadStudentData() {
        String name = getIntent().getStringExtra(EXTRA_STUDENT_NAME);
        String number = getIntent().getStringExtra(EXTRA_STUDENT_NUMBER);
        String programme = getIntent().getStringExtra(EXTRA_STUDENT_PROGRAMME);

        if (name == null || name.trim().isEmpty()) name = "Student Record";
        if (number == null || number.trim().isEmpty()) number = "202412345";
        if (programme == null || programme.trim().isEmpty()) programme = "CS";

        tvStudentName.setText(name);
        tvStudentDetails.setText(number + " • " + programme);
        tvAvatarInitials.setText(getInitials(name));
    }

    private void setupListeners() {
        Button btnCancel = findViewById(R.id.btnCancel);
        if (btnCancel != null) btnCancel.setOnClickListener(v -> finish());

        Button btnDelete = findViewById(R.id.btnDelete);
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                Toast.makeText(this, "Student deleted successfully", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            });
        }
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) sb.append(Character.toUpperCase(part.charAt(0)));
            if (sb.length() >= 2) break;
        }
        return sb.toString();
    }
}
