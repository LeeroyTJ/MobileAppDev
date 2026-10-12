package com.example.mobileappdev;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.mobileappdev.data.local.AppDatabase;
import com.example.mobileappdev.data.local.entity.StudentEntity;
import com.example.mobileappdev.data.remote.ApiClient;
import com.example.mobileappdev.data.remote.SyncApiModels.SyncOperation;
import com.example.mobileappdev.data.remote.SyncApiModels.SyncRequest;
import com.example.mobileappdev.data.remote.SyncApiModels.SyncResponseEnvelope;
import com.example.mobileappdev.data.remote.SyncApiModels.SyncResult;
import com.example.mobileappdev.data.repository.StudentSyncRepository;
import com.example.mobileappdev.session.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddStudentDialogFragment extends DialogFragment {

    public static final String TAG = "AddStudentDialogFragment";

    private TextInputEditText etStudentName;
    private TextInputEditText etStudentNumber;
    private AutoCompleteTextView actvProgramme;
    private AutoCompleteTextView actvLabGroup;
    private ProgressBar progressSaving;
    private MaterialButton btnSave;
    private MaterialButton btnCancel;

    private static final Map<String, Integer> PROGRAMME_MAP = new LinkedHashMap<>();
    static {
        PROGRAMME_MAP.put("Computer Science", 1);
        PROGRAMME_MAP.put("Data Science", 2);
        PROGRAMME_MAP.put("Information Technology", 3);
    }

    private static final Map<String, String> PROGRAMME_CODE_MAP = new HashMap<>();
    static {
        PROGRAMME_CODE_MAP.put("Computer Science", "CS");
        PROGRAMME_CODE_MAP.put("Data Science", "DS");
        PROGRAMME_CODE_MAP.put("Information Technology", "IT");
    }

    private Integer selectedProgrammeId = null;
    private String selectedProgrammeCode = "CS";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Theme_CohortHub);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_student_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvTitle = view.findViewById(R.id.tv_dialog_title);
        TextView tvSubtitle = view.findViewById(R.id.tv_dialog_subtitle);
        ImageView ivIcon = view.findViewById(R.id.iv_editor_icon);
        ImageButton btnClose = view.findViewById(R.id.btn_close_dialog);

        if (tvTitle != null) tvTitle.setText("Add Student");
        if (tvSubtitle != null) tvSubtitle.setText("Enter student details below to create a new record.");
        if (ivIcon != null) ivIcon.setImageResource(R.id.fab_add != 0 ? R.drawable.ic_add : R.drawable.ic_edit_outline);

        etStudentName = view.findViewById(R.id.et_student_name);
        etStudentNumber = view.findViewById(R.id.et_student_number);
        actvProgramme = view.findViewById(R.id.actv_programme);
        actvLabGroup = view.findViewById(R.id.actv_lab_group);
        progressSaving = view.findViewById(R.id.progress_saving);
        btnSave = view.findViewById(R.id.btn_save);
        btnCancel = view.findViewById(R.id.btn_cancel);

        if (btnSave != null) btnSave.setText("Add Student");

        setupDropdowns();

        if (btnClose != null) btnClose.setOnClickListener(v -> dismiss());
        if (btnCancel != null) btnCancel.setOnClickListener(v -> dismiss());
        if (btnSave != null) btnSave.setOnClickListener(v -> attemptCreateStudent());
    }

    private void setupDropdowns() {
        if (actvProgramme != null) {
            String[] programmes = PROGRAMME_MAP.keySet().toArray(new String[0]);
            ArrayAdapter<String> progAdapter = new ArrayAdapter<>(
                    requireContext(), android.R.layout.simple_list_item_1, programmes);
            actvProgramme.setAdapter(progAdapter);
            actvProgramme.setOnItemClickListener((parent, view, position, id) -> {
                Object selected = parent.getItemAtPosition(position);
                if (selected != null) {
                    String progName = selected.toString();
                    selectedProgrammeId = PROGRAMME_MAP.get(progName);
                    selectedProgrammeCode = PROGRAMME_CODE_MAP.getOrDefault(progName, "CS");
                }
            });
        }

        if (actvLabGroup != null) {
            String[] groups = {"G01", "G02", "G03", "G04", "Unassigned"};
            ArrayAdapter<String> groupAdapter = new ArrayAdapter<>(
                    requireContext(), android.R.layout.simple_list_item_1, groups);
            actvLabGroup.setAdapter(groupAdapter);
        }
    }

    private void attemptCreateStudent() {
        String name = safeText(etStudentName);
        String studentNumber = safeText(etStudentNumber);
        String group = actvLabGroup != null ? safeText(actvLabGroup) : "";

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(studentNumber)) {
            Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!studentNumber.matches("\\d{9}")) {
            Toast.makeText(requireContext(), "Student number must be exactly 9 digits", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedProgrammeId == null) {
            selectedProgrammeId = 1;
            selectedProgrammeCode = "CS";
        }

        String finalGroup = ("Unassigned".equalsIgnoreCase(group) || TextUtils.isEmpty(group)) ? null : group;

        if (progressSaving != null) progressSaving.setVisibility(View.VISIBLE);
        if (btnSave != null) btnSave.setEnabled(false);

        long accountId = SessionManager.getInstance(requireContext()).getAccountId();

        String operationId = UUID.randomUUID().toString();
        SyncOperation op = new SyncOperation();
        op.operationId = operationId;
        op.operationType = "CREATE_STUDENT";
        op.entityType = "STUDENT";
        op.accountId = accountId;
        op.baseVersion = 1;

        Map<String, Object> payload = new HashMap<>();
        payload.put("studentNumber", studentNumber);
        payload.put("name", name);
        payload.put("programmeId", selectedProgrammeId);
        if (finalGroup != null) {
            payload.put("labGroup", finalGroup);
        }
        op.payload = payload;

        SyncRequest syncRequest = new SyncRequest(Collections.singletonList(op));

        ApiClient.getSyncService(requireContext()).pushOperations(syncRequest).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<SyncResponseEnvelope> call, @NonNull Response<SyncResponseEnvelope> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null && response.body().data != null
                        && response.body().data.results != null && !response.body().data.results.isEmpty()) {

                    SyncResult result = response.body().data.results.get(0);
                    String status = result.status != null ? result.status.toUpperCase() : "";

                    if ("APPLIED".equals(status) || "APPROVED".equals(status)) {
                        long serverId = result.entityId != null ? result.entityId : 0;
                        saveStudentToRoom(name, studentNumber, selectedProgrammeCode, finalGroup, serverId, StudentEntity.STATUS_SYNCED, accountId);
                        Toast.makeText(requireContext(), "Student added successfully", Toast.LENGTH_SHORT).show();
                        dismiss();
                        return;
                    } else {
                        // Operation REJECTED / CONFLICT / ERROR — Do NOT save to Room
                        String errorMsg = result.message;
                        if (TextUtils.isEmpty(errorMsg) && result.data != null) {
                            Object msgObj = result.data.get("message");
                            if (msgObj != null) errorMsg = msgObj.toString();
                        }
                        if (TextUtils.isEmpty(errorMsg)) {
                            errorMsg = "Failed to add student";
                        }

                        Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show();
                        if (progressSaving != null) progressSaving.setVisibility(View.GONE);
                        if (btnSave != null) btnSave.setEnabled(true);
                        return;
                    }
                }

                saveStudentOffline(name, studentNumber, selectedProgrammeCode, finalGroup, accountId, op);
                Toast.makeText(requireContext(), "Saved locally — will sync when online", Toast.LENGTH_LONG).show();
                dismiss();
            }

            @Override
            public void onFailure(@NonNull Call<SyncResponseEnvelope> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                saveStudentOffline(name, studentNumber, selectedProgrammeCode, finalGroup, accountId, op);
                Toast.makeText(requireContext(), "Saved locally — will sync when online", Toast.LENGTH_LONG).show();
                dismiss();
            }
        });
    }

    private void saveStudentToRoom(String name, String studentNumber, String programme, String labGroup, long serverId, String syncStatus, long accountId) {
        if (getContext() == null) return;
        AppDatabase db = AppDatabase.getInstance(getContext().getApplicationContext());
        Executors.newSingleThreadExecutor().execute(() -> {
            StudentEntity student = new StudentEntity();
            student.name = name;
            student.studentNumber = studentNumber;
            student.programme = programme;
            student.labGroup = labGroup;
            student.serverId = serverId > 0 ? serverId : null;
            student.syncStatus = syncStatus;
            student.accountId = accountId;
            student.baseVersion = 1;
            student.isDeleted = false;

            db.studentDao().insert(student);
        });
    }

    private void saveStudentOffline(String name, String studentNumber, String programme, String labGroup, long accountId, SyncOperation op) {
        if (getContext() == null) return;
        AppDatabase db = AppDatabase.getInstance(getContext().getApplicationContext());
        StudentSyncRepository repository = new StudentSyncRepository(db);

        StudentEntity student = new StudentEntity();
        student.name = name;
        student.studentNumber = studentNumber;
        student.programme = programme;
        student.labGroup = labGroup;
        student.accountId = accountId;
        student.syncStatus = StudentEntity.STATUS_PENDING;
        student.baseVersion = 1;
        student.isDeleted = false;

        String payloadJson = new Gson().toJson(op.payload);
        repository.submitCreate(student, accountId, payloadJson);
    }

    private String safeText(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }

    private String safeText(AutoCompleteTextView field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }
}
