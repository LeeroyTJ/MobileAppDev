package com.example.mobileappdev;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mobileappdev.data.remote.ApiClient;
import com.example.mobileappdev.session.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditStudentActivity extends AppCompatActivity {

    public static final String EXTRA_STUDENT_SERVER_ID = "student_server_id";

    private ImageButton btnClose;
    private TextInputLayout tilStudentName;
    private TextInputEditText etStudentName;
    private TextInputLayout tilStudentNumber;
    private TextInputEditText etStudentNumber;
    private TextInputLayout tilProgramme;
    private MaterialAutoCompleteTextView actvProgramme;
    private TextInputLayout tilLabGroup;
    private MaterialAutoCompleteTextView actvLabGroup;
    private ProgressBar progressSaving;
    private MaterialButton btnCancel;
    private MaterialButton btnSave;

    private long studentServerId = -1;
    private int baseVersion = 1;

    private String initialName = "";
    private String initialNumber = "";
    private String initialProgramme = "";
    private String initialGroupCode = "";
    private long initialGroupId = -1;

    private final List<GroupSpinnerItem> groupSpinnerItems = new ArrayList<>();
    private GroupSpinnerItem selectedGroupItem = null;

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
        setContentView(R.layout.dialog_student_editor);

        if (!SessionManager.getInstance(this).isLoggedIn()) {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }

        readIntentExtras();
        bindViews();
        setupProgrammeDropdown();
        fetchGroupsAndPopulateSpinner();
        populateInitialFields();
    }

    private void readIntentExtras() {
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(EXTRA_STUDENT_SERVER_ID)) {
                studentServerId = intent.getLongExtra(EXTRA_STUDENT_SERVER_ID, -1);
            } else if (intent.hasExtra("student_id")) {
                studentServerId = intent.getLongExtra("student_id", -1);
            } else if (intent.hasExtra("id")) {
                studentServerId = intent.getLongExtra("id", -1);
            } else if (intent.hasExtra("studentId")) {
                studentServerId = intent.getLongExtra("studentId", -1);
            }

            initialName = intent.getStringExtra("student_name");
            if (initialName == null) initialName = intent.getStringExtra("name");
            if (initialName == null) initialName = "";

            initialNumber = intent.getStringExtra("student_number");
            if (initialNumber == null) initialNumber = intent.getStringExtra("studentNumber");
            if (initialNumber == null) initialNumber = "";

            initialProgramme = intent.getStringExtra("programme");
            if (initialProgramme == null) initialProgramme = "";

            initialGroupCode = intent.getStringExtra("group_code");
            if (initialGroupCode == null) initialGroupCode = intent.getStringExtra("groupCode");
            if (initialGroupCode == null) initialGroupCode = "";

            initialGroupId = intent.getLongExtra("group_id", -1);

            baseVersion = intent.getIntExtra("base_version", 1);
            if (baseVersion <= 0) baseVersion = intent.getIntExtra("version", 1);
        }
    }

    private void bindViews() {
        btnClose = findViewById(R.id.btn_close_dialog);
        tilStudentName = findViewById(R.id.til_student_name);
        etStudentName = findViewById(R.id.et_student_name);
        tilStudentNumber = findViewById(R.id.til_student_number);
        etStudentNumber = findViewById(R.id.et_student_number);
        tilProgramme = findViewById(R.id.til_programme);
        actvProgramme = findViewById(R.id.actv_programme);
        tilLabGroup = findViewById(R.id.til_lab_group);
        actvLabGroup = findViewById(R.id.actv_lab_group);
        progressSaving = findViewById(R.id.progress_saving);
        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save);

        if (btnClose != null) btnClose.setOnClickListener(v -> finish());
        if (btnCancel != null) btnCancel.setOnClickListener(v -> finish());
        if (btnSave != null) btnSave.setOnClickListener(v -> performSave());
    }

    private void populateInitialFields() {
        if (etStudentName != null) etStudentName.setText(initialName);
        if (etStudentNumber != null) etStudentNumber.setText(initialNumber);
        if (actvProgramme != null && !TextUtils.isEmpty(initialProgramme)) {
            actvProgramme.setText(initialProgramme, false);
        }
    }

    private void setupProgrammeDropdown() {
        String[] programmes = new String[]{
                "BSc Computer Science",
                "BSc Information Technology",
                "BSc Software Engineering",
                "BEng Computer Engineering",
                "Other"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, programmes);
        actvProgramme.setAdapter(adapter);
    }

    /**
     * Fix 3C: Populate group spinner dynamically from GroupApiService.listGroups() showing real occupancy
     */
    private void fetchGroupsAndPopulateSpinner() {
        ApiClient.getGroupService(this).listGroups().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (!response.isSuccessful() || response.body() == null
                        || !Boolean.TRUE.equals(response.body().get("success"))) {
                    return;
                }

                List<?> groupsData = (List<?>) response.body().get("data");
                if (groupsData == null) return;

                groupSpinnerItems.clear();
                int initialSelectedPosition = -1;

                for (int i = 0; i < groupsData.size(); i++) {
                    Object item = groupsData.get(i);
                    if (item instanceof Map) {
                        Map<?, ?> groupMap = (Map<?, ?>) item;
                        long gId = groupMap.get("group_id") instanceof Number ?
                                ((Number) groupMap.get("group_id")).longValue() : 0;
                        String code = groupMap.get("group_code") != null ? groupMap.get("group_code").toString() : "";
                        String name = groupMap.get("name") != null ? groupMap.get("name").toString() : "";
                        long capacity = groupMap.get("capacity") instanceof Number ?
                                ((Number) groupMap.get("capacity")).longValue() : 15;
                        long occupied = groupMap.get("occupied") instanceof Number ?
                                ((Number) groupMap.get("occupied")).longValue() : 0;

                        GroupSpinnerItem spinnerItem = new GroupSpinnerItem(gId, code, name, capacity, occupied);
                        groupSpinnerItems.add(spinnerItem);

                        if (gId == initialGroupId || code.equalsIgnoreCase(initialGroupCode)) {
                            initialSelectedPosition = i;
                            selectedGroupItem = spinnerItem;
                        }
                    }
                }

                ArrayAdapter<GroupSpinnerItem> groupAdapter = new ArrayAdapter<>(
                        EditStudentActivity.this,
                        android.R.layout.simple_dropdown_item_1line,
                        groupSpinnerItems
                );
                actvLabGroup.setAdapter(groupAdapter);

                if (initialSelectedPosition >= 0 && initialSelectedPosition < groupSpinnerItems.size()) {
                    actvLabGroup.setText(groupSpinnerItems.get(initialSelectedPosition).toString(), false);
                }

                actvLabGroup.setOnItemClickListener((parent, view, position, id) -> {
                    if (position >= 0 && position < groupSpinnerItems.size()) {
                        selectedGroupItem = groupSpinnerItems.get(position);
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                Toast.makeText(EditStudentActivity.this, "Failed to load live groups", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performSave() {
        if (studentServerId <= 0) {
            Toast.makeText(this, "Invalid student server ID", Toast.LENGTH_SHORT).show();
            return;
        }

        tilStudentName.setError(null);
        tilStudentNumber.setError(null);

        String newName = etStudentName.getText() != null ? etStudentName.getText().toString().trim() : "";
        String newNumber = etStudentNumber.getText() != null ? etStudentNumber.getText().toString().trim() : "";

        // Fix 3C: Wire correctStudentNumber with local 9-digit regex validation
        if (!TextUtils.isEmpty(newNumber) && !newNumber.matches("^\\d{9}$")) {
            tilStudentNumber.setError("Enter a valid 9-digit student number");
            return;
        }

        setSavingState(true);

        boolean nameChanged = !newName.equals(initialName);
        boolean numberChanged = !newNumber.isEmpty() && !newNumber.equals(initialNumber);
        boolean groupChanged = selectedGroupItem != null && selectedGroupItem.groupId != initialGroupId;

        if (nameChanged) {
            updateStudentDetails(newName, () -> {
                if (numberChanged) {
                    patchStudentNumber(newNumber, () -> {
                        if (groupChanged) {
                            changeGroup(selectedGroupItem.groupId, this::finishSuccess);
                        } else {
                            finishSuccess();
                        }
                    });
                } else if (groupChanged) {
                    changeGroup(selectedGroupItem.groupId, this::finishSuccess);
                } else {
                    finishSuccess();
                }
            });
        } else if (numberChanged) {
            patchStudentNumber(newNumber, () -> {
                if (groupChanged) {
                    changeGroup(selectedGroupItem.groupId, this::finishSuccess);
                } else {
                    finishSuccess();
                }
            });
        } else if (groupChanged) {
            changeGroup(selectedGroupItem.groupId, this::finishSuccess);
        } else {
            setSavingState(false);
            finish();
        }
    }

    /**
     * Fix 3C: Wire editDetails to StudentApiService.updateStudent(), handling 409 VERSION_CONFLICT
     */
    private void updateStudentDetails(String newName, Runnable onNext) {
        Map<String, Object> body = new HashMap<>();
        body.put("studentName", newName);
        body.put("baseVersion", baseVersion);

        ApiClient.getStudentService(this).updateStudent(studentServerId, body).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null
                        && Boolean.TRUE.equals(response.body().get("success"))) {
                    onNext.run();
                } else if (response.code() == 409) {
                    setSavingState(false);
                    Toast.makeText(EditStudentActivity.this,
                            "VERSION_CONFLICT: Record modified on server. Please refresh.", Toast.LENGTH_LONG).show();
                } else {
                    setSavingState(false);
                    Toast.makeText(EditStudentActivity.this, "Failed to update details", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                setSavingState(false);
                Toast.makeText(EditStudentActivity.this, "Network error updating details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Fix 3C: Wire correctStudentNumber to StudentApiService.patchStudentNumber()
     */
    private void patchStudentNumber(String newNumber, Runnable onNext) {
        Map<String, Object> body = new HashMap<>();
        body.put("studentNumber", newNumber);
        body.put("baseVersion", baseVersion);

        ApiClient.getStudentService(this).patchStudentNumber(studentServerId, body).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null
                        && Boolean.TRUE.equals(response.body().get("success"))) {
                    onNext.run();
                } else if (response.code() == 409) {
                    setSavingState(false);
                    Toast.makeText(EditStudentActivity.this,
                            "Student number already exists or version conflict", Toast.LENGTH_LONG).show();
                } else {
                    setSavingState(false);
                    Toast.makeText(EditStudentActivity.this, "Failed to correct student number", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                setSavingState(false);
                Toast.makeText(EditStudentActivity.this, "Network error correcting student number", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Fix 3C: Wire changeGroup to GroupApiService.assignStudent() / transferStudent(), handling 409 GROUP_FULL
     */
    private void changeGroup(long targetGroupId, Runnable onNext) {
        Map<String, Object> body = new HashMap<>();
        body.put("studentId", studentServerId);

        boolean isUnassigned = initialGroupId <= 0 || "Unassigned".equalsIgnoreCase(initialGroupCode);

        Call<Map<String, Object>> call = isUnassigned
                ? ApiClient.getGroupService(this).assignStudent(targetGroupId, body)
                : ApiClient.getGroupService(this).transferStudent(targetGroupId, body);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null
                        && Boolean.TRUE.equals(response.body().get("success"))) {
                    onNext.run();
                } else if (response.code() == 409) {
                    setSavingState(false);
                    Toast.makeText(EditStudentActivity.this,
                            "GROUP_FULL: Target group is full — previous group retained.", Toast.LENGTH_LONG).show();
                } else {
                    setSavingState(false);
                    Toast.makeText(EditStudentActivity.this, "Failed to change lab group", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                setSavingState(false);
                Toast.makeText(EditStudentActivity.this, "Network error changing lab group", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setSavingState(boolean saving) {
        if (progressSaving != null) progressSaving.setVisibility(saving ? View.VISIBLE : View.GONE);
        if (btnSave != null) btnSave.setEnabled(!saving);
        if (btnCancel != null) btnCancel.setEnabled(!saving);
    }

    private void finishSuccess() {
        setSavingState(false);
        Toast.makeText(this, "Student updated successfully", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }
}
