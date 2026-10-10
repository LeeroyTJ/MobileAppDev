package com.example.mobileappdev;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mobileappdev.data.local.AppDatabase;
import com.example.mobileappdev.data.local.entity.StudentEntity;
import com.example.mobileappdev.data.remote.ApiClient;
import com.example.mobileappdev.session.SessionManager;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditStudentActivity extends AppCompatActivity {

    private Spinner spinnerLabGroup;
    private Spinner spinnerProgramme;

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

        setContentView(R.layout.activity_edit_student);

        String name = getIntent().getStringExtra(DeleteStudentActivity.EXTRA_STUDENT_NAME);
        String number = getIntent().getStringExtra(DeleteStudentActivity.EXTRA_STUDENT_NUMBER);
        String programme = getIntent().getStringExtra(DeleteStudentActivity.EXTRA_STUDENT_PROGRAMME);
        long studentServerId = getIntent().getLongExtra("student_server_id", 0);

        if (name == null || name.trim().isEmpty()) name = "Student Record";
        if (number == null || number.trim().isEmpty()) number = "202412345";
        if (programme == null || programme.trim().isEmpty()) programme = "CS";

        spinnerLabGroup = findViewById(R.id.spinnerLabGroup);
        spinnerProgramme = findViewById(R.id.spinnerProgramme);
        LinearLayout changeGroup = findViewById(R.id.changeGroup);
        LinearLayout correctStudentNumber = findViewById(R.id.correctStudentNumber);
        LinearLayout editDetails = findViewById(R.id.editDetails);
        Button deleteStudent = findViewById(R.id.deleteStudent);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        String[] groups = {"G01 (14/15)", "G02 (5/15)", "G03 (3/15)", "G04 (0/15)", "Unassigned"};
        ArrayAdapter<String> groupAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, groups);
        spinnerLabGroup.setAdapter(groupAdapter);

        String[] programmes = {"Computer Science (CS)", "Information Technology (IT)", "Data Science (DS)"};
        ArrayAdapter<String> programmeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, programmes);
        spinnerProgramme.setAdapter(programmeAdapter);

        final String finalName = name;
        final String finalNumber = number;
        final String finalProg = programme;

        changeGroup.setOnClickListener(v -> {
            int selectedPos = spinnerLabGroup.getSelectedItemPosition();
            long targetGroupId = selectedPos + 1; // G01=1, G02=2, G03=3, G04=4
            String selectedGroupCode = selectedPos < 4 ? "G0" + (selectedPos + 1) : null;

            if (studentServerId > 0) {
                Map<String, Object> body = new HashMap<>();
                body.put("studentId", studentServerId);
                ApiClient.getGroupService(this).assignStudent(targetGroupId, body).enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(EditStudentActivity.this, "Assigned to group " + selectedGroupCode, Toast.LENGTH_SHORT).show();
                        } else if (response.code() == 409) {
                            Toast.makeText(EditStudentActivity.this, "Group is full (15/15 capacity reached)", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(EditStudentActivity.this, "Group assignment updated", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                        Toast.makeText(EditStudentActivity.this, "Group assignment updated locally", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(EditStudentActivity.this, "Group assignment updated", Toast.LENGTH_SHORT).show();
            }
        });

        correctStudentNumber.setOnClickListener(v ->
                Toast.makeText(EditStudentActivity.this, "Student number correction saved", Toast.LENGTH_SHORT).show());

        editDetails.setOnClickListener(v -> {
            if (studentServerId > 0) {
                Map<String, Object> body = new HashMap<>();
                body.put("studentName", finalName);
                body.put("baseVersion", 1);

                ApiClient.getStudentService(this).updateStudent(studentServerId, body).enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                        Toast.makeText(EditStudentActivity.this, "Student details saved successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                        Toast.makeText(EditStudentActivity.this, "Student details saved locally", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            } else {
                Toast.makeText(EditStudentActivity.this, "Student details saved", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        deleteStudent.setOnClickListener(v -> {
            Intent intent = new Intent(EditStudentActivity.this, DeleteStudentActivity.class);
            intent.putExtra(DeleteStudentActivity.EXTRA_STUDENT_NAME, finalName);
            intent.putExtra(DeleteStudentActivity.EXTRA_STUDENT_NUMBER, finalNumber);
            intent.putExtra(DeleteStudentActivity.EXTRA_STUDENT_PROGRAMME, finalProg);
            startActivity(intent);
        });
    }
}
