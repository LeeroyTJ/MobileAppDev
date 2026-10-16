package com.example.mobileappdev;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditStudentActivity extends AppCompatActivity {

    private Spinner spinnerLabGroup;
    private Spinner spinnerProgramme;

    private long studentServerId = 0;
    private int baseVersion = 1;

    private String name = "";
    private String number = "";
    private String programme = "";
    private String groupCode = "";

    private final List<GroupSpinnerItem> groupList = new ArrayList<>();

    private static class GroupSpinnerItem {
        long groupId;
        String groupCode;
        String name;
        long capacity;
        long occupied;

        GroupSpinnerItem(long groupId, String groupCode, String name, long capacity, long occupied) {
            this.groupId = groupId;
            this.groupCode = groupCode;
            this.name = name;
            this.capacity = capacity;
            this.occupied = occupied;
        }

        @NonNull
        @Override
        public String toString() {
            return groupCode + " - " + name + " (" + occupied + "/" + capacity + " filled)";
        }
    }

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

        readIntentExtras();
        bindViews();
        fetchGroupsAndPopulateSpinner();

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
            if (studentServerId <= 0) {
                studentServerId = intent.getLongExtra("student_id", 0);
            }
            if (studentServerId <= 0) {
                studentServerId = intent.getLongExtra("id", 0);
            }

            name = intent.getStringExtra(DeleteStudentActivity.EXTRA_STUDENT_NAME);
            if (name == null) name = intent.getStringExtra("name");
            if (name == null) name = "Student Record";

            number = intent.getStringExtra(DeleteStudentActivity.EXTRA_STUDENT_NUMBER);
            if (number == null) number = intent.getStringExtra("student_number");
            if (number == null) number = "202412345";

            programme = intent.getStringExtra(DeleteStudentActivity.EXTRA_STUDENT_PROGRAMME);
            if (programme == null) programme = intent.getStringExtra("programme");
            if (programme == null) programme = "CS";

            groupCode = intent.getStringExtra("group_code");
            if (groupCode == null) groupCode = intent.getStringExtra("lab_group");

            baseVersion = intent.getIntExtra("base_version", 1);
            if (baseVersion <= 0) baseVersion = intent.getIntExtra("version", 1);
        }
    }

    private void bindViews() {
        spinnerLabGroup = findViewById(R.id.spinnerLabGroup);
        spinnerProgramme = findViewById(R.id.spinnerProgramme);
        LinearLayout changeGroup = findViewById(R.id.changeGroup);
        LinearLayout correctStudentNumber = findViewById(R.id.correctStudentNumber);
        LinearLayout editDetails = findViewById(R.id.editDetails);
        Button deleteStudent = findViewById(R.id.deleteStudent);
        ImageButton btnBack = findViewById(R.id.btnBack);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Programme Spinner Initial Setup
        String[] programmes = {"Computer Science (CS)", "Information Technology (IT)", "Data Science (DS)"};
        ArrayAdapter<String> programmeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, programmes);
        if (spinnerProgramme != null) {
            spinnerProgramme.setAdapter(programmeAdapter);
            if ("IT".equalsIgnoreCase(programme)) spinnerProgramme.setSelection(1);
            else if ("DS".equalsIgnoreCase(programme)) spinnerProgramme.setSelection(2);
            else spinnerProgramme.setSelection(0);
        }

        // Change Group Click
        if (changeGroup != null) {
            changeGroup.setOnClickListener(v -> performChangeGroup());
        }

        // Correct Student Number Click
        if (correctStudentNumber != null) {
            correctStudentNumber.setOnClickListener(v -> promptNumberCorrection());
        }

        // Edit Details Click
        if (editDetails != null) {
            editDetails.setOnClickListener(v -> promptEditDetails());
        }

        // Delete Student Click
        if (deleteStudent != null) {
            deleteStudent.setOnClickListener(v -> {
                Intent intent = new Intent(EditStudentActivity.this, DeleteStudentActivity.class);
                intent.putExtra(DeleteStudentActivity.EXTRA_STUDENT_NAME, name);
                intent.putExtra(DeleteStudentActivity.EXTRA_STUDENT_NUMBER, number);
                intent.putExtra(DeleteStudentActivity.EXTRA_STUDENT_PROGRAMME, programme);
                intent.putExtra("student_server_id", studentServerId);
                startActivity(intent);
            });
        }
    }

    private void fetchGroupsAndPopulateSpinner() {
        ApiClient.getGroupService(this).listGroups().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (!response.isSuccessful() || response.body() == null
                        || !Boolean.TRUE.equals(response.body().get("success"))) {
                    useFallbackGroupSpinner();
                    return;
                }

                List<?> groupsData = (List<?>) response.body().get("data");
                if (groupsData == null || groupsData.isEmpty()) {
                    useFallbackGroupSpinner();
                    return;
                }

                groupList.clear();
                int selectedIndex = 0;

                for (int i = 0; i < groupsData.size(); i++) {
                    Object item = groupsData.get(i);
                    if (item instanceof Map) {
                        Map<?, ?> gMap = (Map<?, ?>) item;
                        long gId = gMap.get("group_id") instanceof Number ? ((Number) gMap.get("group_id")).longValue() : 0;
                        String code = gMap.get("group_code") != null ? gMap.get("group_code").toString() : "";
                        String gName = gMap.get("name") != null ? gMap.get("name").toString() : "";
                        long capacity = gMap.get("capacity") instanceof Number ? ((Number) gMap.get("capacity")).longValue() : 15;
                        long occupied = gMap.get("occupied") instanceof Number ? ((Number) gMap.get("occupied")).longValue() : 0;

                        groupList.add(new GroupSpinnerItem(gId, code, gName, capacity, occupied));

                        if (code.equalsIgnoreCase(groupCode)) {
                            selectedIndex = i;
                        }
                    }
                }

                ArrayAdapter<GroupSpinnerItem> groupAdapter = new ArrayAdapter<>(
                        EditStudentActivity.this,
                        android.R.layout.simple_spinner_dropdown_item,
                        groupList
                );
                if (spinnerLabGroup != null) {
                    spinnerLabGroup.setAdapter(groupAdapter);
                    spinnerLabGroup.setSelection(selectedIndex);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                useFallbackGroupSpinner();
            }
        });
    }

    private void useFallbackGroupSpinner() {
        String[] groups = {"G01", "G02", "G03", "G04", "Unassigned"};
        ArrayAdapter<String> groupAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, groups);
        if (spinnerLabGroup != null) {
            spinnerLabGroup.setAdapter(groupAdapter);
        }
    }

    private void performChangeGroup() {
        if (spinnerLabGroup == null || groupList.isEmpty()) {
            Toast.makeText(this, "Group list loading...", Toast.LENGTH_SHORT).show();
            return;
        }

        int pos = spinnerLabGroup.getSelectedItemPosition();
        if (pos < 0 || pos >= groupList.size()) return;

        GroupSpinnerItem targetGroup = groupList.get(pos);
        long targetGroupId = targetGroup.groupId;

        Map<String, Object> body = new HashMap<>();
        body.put("studentId", studentServerId);

        boolean isUnassigned = groupCode == null || TextUtils.isEmpty(groupCode) || "Unassigned".equalsIgnoreCase(groupCode);

        Call<Map<String, Object>> apiCall = isUnassigned
                ? ApiClient.getGroupService(this).assignStudent(targetGroupId, body)
                : ApiClient.getGroupService(this).transferStudent(targetGroupId, body);

        apiCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null
                        && Boolean.TRUE.equals(response.body().get("success"))) {
                    groupCode = targetGroup.groupCode;
                    updateLocalRoomStudentGroup(studentServerId, groupCode);
                    Toast.makeText(EditStudentActivity.this, "Assigned to group " + targetGroup.groupCode, Toast.LENGTH_SHORT).show();
                } else if (response.code() == 409) {
                    Toast.makeText(EditStudentActivity.this, "GROUP_FULL: Selected group capacity (15/15) reached", Toast.LENGTH_LONG).show();
                } else {
                    queueGroupChangeOffline(targetGroup.groupCode);
                    Toast.makeText(EditStudentActivity.this, "Group change saved locally", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                queueGroupChangeOffline(targetGroup.groupCode);
                Toast.makeText(EditStudentActivity.this, "Group change saved locally", Toast.LENGTH_SHORT).show();
            }
        });
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
                    String newNumber = etNumberInput.getText() != null ? etNumberInput.getText().toString().trim() : "";

                    if (!newNumber.matches("^\\d{9}$")) {
                        Toast.makeText(this, "Student number must be exactly 9 digits", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (studentServerId <= 0) {
                        number = newNumber;
                        Toast.makeText(this, "Student number updated", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String, Object> body = new HashMap<>();
                    body.put("studentNumber", newNumber);
                    body.put("baseVersion", baseVersion);

                    ApiClient.getStudentService(this).patchStudentNumber(studentServerId, body).enqueue(new Callback<>() {
                        @Override
                        public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                            if (response.isSuccessful() && response.body() != null
                                    && Boolean.TRUE.equals(response.body().get("success"))) {
                                number = newNumber;
                                updateLocalRoomStudentNumber(studentServerId, newNumber);
                                Toast.makeText(EditStudentActivity.this, "Student number updated successfully", Toast.LENGTH_SHORT).show();
                            } else if (response.code() == 409) {
                                Toast.makeText(EditStudentActivity.this, "Conflict: Student number already exists or version conflict", Toast.LENGTH_LONG).show();
                            } else {
                                queueNumberCorrectionOffline(newNumber);
                                Toast.makeText(EditStudentActivity.this, "Correction saved locally", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                            queueNumberCorrectionOffline(newNumber);
                            Toast.makeText(EditStudentActivity.this, "Correction saved locally — will sync when online", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void promptEditDetails() {
        final EditText etNameInput = new EditText(this);
        etNameInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        etNameInput.setHint("Enter full student name");
        etNameInput.setText(name);

        LinearLayout container = new LinearLayout(this);
        container.setPadding(56, 32, 56, 8);
        container.addView(etNameInput);

        new AlertDialog.Builder(this)
                .setTitle("Edit Student Details")
                .setView(container)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = etNameInput.getText() != null ? etNameInput.getText().toString().trim() : "";

                    if (TextUtils.isEmpty(newName)) {
                        Toast.makeText(this, "Student name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (studentServerId <= 0) {
                        name = newName;
                        Toast.makeText(this, "Details updated", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    Map<String, Object> body = new HashMap<>();
                    body.put("studentName", newName);
                    body.put("baseVersion", baseVersion);

                    ApiClient.getStudentService(this).updateStudent(studentServerId, body).enqueue(new Callback<>() {
                        @Override
                        public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                            if (response.isSuccessful() && response.body() != null
                                    && Boolean.TRUE.equals(response.body().get("success"))) {
                                name = newName;
                                Object dataObj = response.body().get("data");
                                if (dataObj instanceof Map) {
                                    Object verObj = ((Map<?, ?>) dataObj).get("version");
                                    if (verObj instanceof Number) baseVersion = ((Number) verObj).intValue();
                                }
                                updateLocalRoomStudentName(studentServerId, newName);
                                Toast.makeText(EditStudentActivity.this, "Student details updated successfully", Toast.LENGTH_SHORT).show();
                                finish();
                            } else if (response.code() == 409) {
                                Toast.makeText(EditStudentActivity.this, "VERSION_CONFLICT: Record modified on server. Please refresh.", Toast.LENGTH_LONG).show();
                            } else {
                                queueNameEditOffline(newName);
                                Toast.makeText(EditStudentActivity.this, "Details saved locally", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                            queueNameEditOffline(newName);
                            Toast.makeText(EditStudentActivity.this, "Details saved locally", Toast.LENGTH_SHORT).show();
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
            if (student == null) return;
            StudentSyncRepository repo = new StudentSyncRepository(db);
            repo.submitNumberCorrection(student.localId, accountId, studentServerId, newNumber, baseVersion);
        });
    }

    private void queueNameEditOffline(String newName) {
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        long accountId = SessionManager.getInstance(this).getAccountId();
        Executors.newSingleThreadExecutor().execute(() -> {
            StudentEntity student = db.studentDao().findByServerId(studentServerId);
            if (student == null) return;
            StudentSyncRepository repo = new StudentSyncRepository(db);
            repo.submitNameEdit(student.localId, accountId, newName, baseVersion);
        });
    }

    private void queueGroupChangeOffline(String newGroup) {
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        long accountId = SessionManager.getInstance(this).getAccountId();
        String previous = groupCode; // capture the pre-change value
        Executors.newSingleThreadExecutor().execute(() -> {
            StudentEntity student = db.studentDao().findByServerId(studentServerId);
            if (student == null) return;
            StudentSyncRepository repo = new StudentSyncRepository(db);
            repo.submitGroupChange(student.localId, accountId, studentServerId,
                    newGroup, previous, baseVersion);
        });
        groupCode = newGroup;
    }

    private void updateLocalRoomStudentGroup(long serverId, String newGroup) {
        if (serverId <= 0) return;
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        Executors.newSingleThreadExecutor().execute(() -> {
            StudentEntity student = db.studentDao().findByServerId(serverId);
            if (student != null) {
                student.labGroup = newGroup;
                db.studentDao().update(student);
            }
        });
    }

    private void updateLocalRoomStudentNumber(long serverId, String newNumber) {
        if (serverId <= 0) return;
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        Executors.newSingleThreadExecutor().execute(() -> {
            StudentEntity student = db.studentDao().findByServerId(serverId);
            if (student != null) {
                student.studentNumber = newNumber;
                db.studentDao().update(student);
            }
        });
    }

    private void updateLocalRoomStudentName(long serverId, String newName) {
        if (serverId <= 0) return;
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        Executors.newSingleThreadExecutor().execute(() -> {
            StudentEntity student = db.studentDao().findByServerId(serverId);
            if (student != null) {
                student.name = newName;
                db.studentDao().update(student);
            }
        });
    }
}
