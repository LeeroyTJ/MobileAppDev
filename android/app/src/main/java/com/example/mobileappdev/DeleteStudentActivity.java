package com.example.mobileappdev;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class DeleteStudentActivity extends AppCompatActivity {

    public static final String EXTRA_STUDENT_NAME = "student_name";
    public static final String EXTRA_STUDENT_NUMBER = "student_number";
    public static final String EXTRA_STUDENT_PROGRAMME = "student_programme";

    private ImageButton btnBack;
    private TextView tvAvatarInitials;
    private TextView tvStudentName;
    private TextView tvStudentDetails;
    private Button btnCancel;
    private Button btnDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_student);

        bindViews();
        loadStudentData();
        setupListeners();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        tvAvatarInitials = findViewById(R.id.tvAvatarInitials);
        tvStudentName = findViewById(R.id.tvStudentName);
        tvStudentDetails = findViewById(R.id.tvStudentDetails);
        btnCancel = findViewById(R.id.btnCancel);
        btnDelete = findViewById(R.id.btnDelete);
    }

    private void loadStudentData() {
        String name = getIntent().getStringExtra(EXTRA_STUDENT_NAME);
        String number = getIntent().getStringExtra(EXTRA_STUDENT_NUMBER);
        String programme = getIntent().getStringExtra(EXTRA_STUDENT_PROGRAMME);

        if (name == null || name.trim().isEmpty()) {
            name = "Thabo Jumbe";
        }
        if (number == null || number.trim().isEmpty()) {
            number = "202312345";
        }
        if (programme == null || programme.trim().isEmpty()) {
            programme = "CS";
        }

        tvStudentName.setText(name);
        tvStudentDetails.setText(number + " • " + programme);
        tvAvatarInitials.setText(getInitials(name));
    }

    private void setupListeners() {
        // Back navigation
        btnBack.setOnClickListener(v -> finish());

        // Cancel action
        btnCancel.setOnClickListener(v -> finish());

        // Delete action
        btnDelete.setOnClickListener(v -> {
            Toast.makeText(this, "Student deleted successfully", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        });
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "TJ";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) sb.append(Character.toUpperCase(part.charAt(0)));
            if (sb.length() >= 2) break;
        }
        return sb.toString();
    }
}
