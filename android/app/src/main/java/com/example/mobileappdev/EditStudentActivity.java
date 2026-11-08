package com.example.mobileappdev;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mobileappdev.data.local.AppDatabase;
import com.example.mobileappdev.data.local.entity.StudentEntity;
import com.example.mobileappdev.data.remote.ApiClient;
import com.example.mobileappdev.data.repository.StudentSyncRepository;
import com.example.mobileappdev.session.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditStudentActivity extends AppCompatActivity {

    private TextInputEditText etCurrentGroup;
    private TextInputEditText etCurrentProgramme;
    private TextView tvStudentName;
    private TextView tvStudentDetails;
    private TextView tvInitials;

    private long studentServerId = 0;
    private int baseVersion = 1;

    private String name = "";
    private String number = "";
    private String programme = "";
    private String groupCode = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager sessionManager = SessionManager.getInstance(this);
        if (!sessionManager.isLoggedIn() || !sessionManager.isLecturer()) {
            finish();
            return;
        }

        setContentView(R.layout.activity_edit_student);

        readIntentExtras();
        bindViews();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void readIntentExtras() {
        Intent intent = getIntent();
        if (intent != null) {
            studentServerId = intent.getLongExtra("student_server_id", 0);

            name = intent.getStringExtra(DeleteStudentActivity.EXTRA_STUDENT_NAME);
            if (name == null) name = "Student";

            number = intent.getStringExtra(DeleteStudentActivity.EXTRA_STUDENT_NUMBER);
            if (number == null) number = "Unknown";

            programme = intent.getStringExtra(DeleteStudentActivity.EXTRA_STUDENT_PROGRAMME);
            if (programme == null) programme = "CS";

            groupCode = intent.getStringExtra("group_code");
            if (groupCode == null) groupCode = "Unassigned";

            baseVersion = intent.getIntExtra("base_version", 1);
        }
    }

    private void bindViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvInitials = findViewById(R.id.tvInitials);
        tvStudentName = findViewById(R.id.tvStudentName);
        tvStudentDetails = findViewById(R.id.tvStudentDetails);
        etCurrentGroup = findViewById(R.id.etCurrentGroup);
        etCurrentProgramme = findViewById(R.id.etCurrentProgramme);

        // Populate header
        tvStudentName.setText(name);
        tvStudentDetails.setText(number + " • " + programme);
        tvInitials.setText(getInitials(name));

        // Populate read-only fields
        etCurrentGroup.setText(groupCode);
        etCurrentProgramme.setText(fullProgrammeName(programme));

        // Action clicks
        findViewById(R.id.changeGroup).setOnClickListener(v -> performChangeGroup());
        findViewById(R.id.correctStudentNumber).setOnClickListener(v -> promptNumberCorrection());
        findViewById(R.id.editDetails).setOnClickListener(v -> promptEditDetails());
        findViewById(R.id.deleteStudent).setOnClickListener(v -> {
            Intent intent = new Intent(EditStudentActivity.this, DeleteStudentActivity.class);
            intent.putExtra(DeleteStudentActivity.EXTRA_STUDENT_NAME, name);
            intent.putExtra(DeleteStudentActivity.EXTRA_STUDENT_NUMBER, number);
            intent.putExtra(DeleteStudentActivity.EXTRA_STUDENT_PROGRAMME, programme);
            intent.putExtra("student_server_id", studentServerId);
            startActivity(intent);
        });

        // Auto-open number dialog if flagged
        if (getIntent().getBooleanExtra("trigger_number_correction", false)) {
            promptNumberCorrection();
        }
    }

    private void performChangeGroup() {
        // Since we no longer have a spinner, we open the GroupPickerDialog directly
        GroupPickerBottomSheetDialogFragment dialog = GroupPickerBottomSheetDialogFragment.newInstance(studentServerId);
        dialog.setOnGroupSelectedListener(newGroupCode -> {
            groupCode = newGroupCode;
            etCurrentGroup.setText(groupCode);
            Toast.makeText(this, "Moved to " + groupCode, Toast.LENGTH_SHORT).show();
        });
        dialog.show(getSupportFragmentManager(), GroupPickerBottomSheetDialogFragment.TAG);
    }

    private void promptNumberCorrection() {
        final EditText etNumberInput = new EditText(this);
        etNumberInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        etNumberInput.setHint("Enter 9-digit student number");
        etNumberInput.setText(number);

        LinearLayout container = new LinearLayout(this);
        container.setPadding(56, 32, 56, 8);
        container.addView(etNumberInput);

        new AlertDialog.Builder(this)
                .setTitle("Correct Student Number")
                .setView(container)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newNumber = etNumberInput.getText().toString().trim();
                    if (!newNumber.matches("^\\d{9}$")) {
                        Toast.makeText(this, "Must be 9 digits", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (studentServerId <= 0) {
                        number = newNumber;
                        Toast.makeText(this, "Local number updated", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Map<String, Object> body = new HashMap<>();
                    body.put("studentNumber", newNumber);
                    body.put("baseVersion", baseVersion);

                    ApiClient.getStudentService(this).patchStudentNumber(studentServerId, body).enqueue(new Callback<>() {
                        @Override
                        public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                            if (response.isSuccessful()) {
                                number = newNumber;
                                tvStudentDetails.setText(number + " • " + programme);
                                Toast.makeText(EditStudentActivity.this, "Number updated", Toast.LENGTH_SHORT).show();
                            } else {
                                queueNumberCorrectionOffline(newNumber);
                                Toast.makeText(EditStudentActivity.this, "Saved locally", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                            queueNumberCorrectionOffline(newNumber);
                            Toast.makeText(EditStudentActivity.this, "Saved locally", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void promptEditDetails() {
        final EditText etNameInput = new EditText(this);
        etNameInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        etNameInput.setText(name);

        LinearLayout container = new LinearLayout(this);
        container.setPadding(56, 32, 56, 8);
        container.addView(etNameInput);

        new AlertDialog.Builder(this)
                .setTitle("Edit Name")
                .setView(container)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = etNameInput.getText().toString().trim();
                    if (TextUtils.isEmpty(newName)) return;

                    if (studentServerId <= 0) {
                        name = newName;
                        updateHeader();
                        Toast.makeText(this, "Local name updated", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String, Object> body = new HashMap<>();
                    body.put("studentName", newName);
                    body.put("baseVersion", baseVersion);

                    ApiClient.getStudentService(this).updateStudent(studentServerId, body).enqueue(new Callback<>() {
                        @Override
                        public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                            if (response.isSuccessful()) {
                                name = newName;
                                updateHeader();
                                Toast.makeText(EditStudentActivity.this, "Name updated", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                queueNameEditOffline(newName);
                                Toast.makeText(EditStudentActivity.this, "Saved locally", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        }
                        @Override
                        public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                            queueNameEditOffline(newName);
                            Toast.makeText(EditStudentActivity.this, "Saved locally", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void queueNumberCorrectionOffline(String newNumber) {
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        long accountId = SessionManager.getInstance(this).getAccountId();
        Executors.newSingleThreadExecutor().execute(() -> {
            StudentEntity student = db.studentDao().findByServerId(studentServerId);
            if (student != null) {
                StudentSyncRepository repo = new StudentSyncRepository(db);
                repo.submitNumberCorrection(student.localId, accountId, studentServerId, newNumber, baseVersion);
            }
        });
    }

    private void queueNameEditOffline(String newName) {
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        long accountId = SessionManager.getInstance(this).getAccountId();
        Executors.newSingleThreadExecutor().execute(() -> {
            StudentEntity student = db.studentDao().findByServerId(studentServerId);
            if (student != null) {
                StudentSyncRepository repo = new StudentSyncRepository(db);
                repo.submitNameEdit(student.localId, accountId, newName, baseVersion);
            }
        });
    }

    private void updateHeader() {
        tvStudentName.setText(name);
        tvStudentDetails.setText(getString(R.string.student_meta_format, number, programme));
        tvInitials.setText(getInitials(name));
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

    private String fullProgrammeName(String code) {
        if ("CS".equalsIgnoreCase(code)) return "Computer Science";
        if ("IT".equalsIgnoreCase(code)) return "Information Technology";
        if ("DS".equalsIgnoreCase(code)) return "Data Science";
        return code;
    }
}