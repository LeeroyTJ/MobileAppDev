package com.example.mobileappdev;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mobileappdev.data.local.AppDatabase;
import com.example.mobileappdev.data.local.entity.StudentEntity;
import com.example.mobileappdev.data.remote.ApiClient;
import com.example.mobileappdev.data.remote.SyncApiModels.SyncOperation;
import com.example.mobileappdev.data.remote.SyncApiModels.SyncRequest;
import com.example.mobileappdev.data.remote.SyncApiModels.SyncResponseEnvelope;
import com.example.mobileappdev.data.remote.SyncApiModels.SyncResult;
import com.example.mobileappdev.data.repository.StudentSyncRepository;
import com.example.mobileappdev.session.SessionManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeleteStudentActivity extends AppCompatActivity {

    public static final String EXTRA_STUDENT_NAME = "student_name";
    public static final String EXTRA_STUDENT_NUMBER = "student_number";
    public static final String EXTRA_STUDENT_PROGRAMME = "student_programme";

    private TextView tvAvatarInitials;
    private TextView tvStudentName;
    private TextView tvStudentDetails;
    private ProgressBar progressLoading;
    private Button btnCancel;
    private Button btnDelete;

    private SessionManager sessionManager;
    private long studentServerId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Role Guard: Lecturer only
        sessionManager = SessionManager.getInstance(this);
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
        studentServerId = getIntent().getLongExtra("student_server_id", 0);

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
        progressLoading = findViewById(R.id.progressLoading);
        btnCancel = findViewById(R.id.btnCancel);
        btnDelete = findViewById(R.id.btnDelete);
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
        if (btnCancel != null) btnCancel.setOnClickListener(v -> finish());

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> executeDelete());
        }
    }

    private void executeDelete() {
        if (studentServerId <= 0) {
            Toast.makeText(this, "Student record not synced with server", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
            return;
        }

        if (progressLoading != null) progressLoading.setVisibility(View.VISIBLE);
        if (btnDelete != null) btnDelete.setEnabled(false);
        if (btnCancel != null) btnCancel.setEnabled(false);

        long accountId = sessionManager.getAccountId();
        String operationId = UUID.randomUUID().toString();

        SyncOperation op = new SyncOperation();
        op.operationId = operationId;
        op.operationType = "DELETE_STUDENT";
        op.entityType = "STUDENT";
        op.entityId = studentServerId;
        op.accountId = accountId;
        op.baseVersion = 1;

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", studentServerId);
        op.payload = payload;

        SyncRequest syncRequest = new SyncRequest(Collections.singletonList(op));

        ApiClient.getSyncService(this).pushOperations(syncRequest).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<SyncResponseEnvelope> call, @NonNull Response<SyncResponseEnvelope> response) {
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful() && response.body() != null && response.body().data != null
                        && response.body().data.results != null && !response.body().data.results.isEmpty()) {

                    SyncResult result = response.body().data.results.get(0);
                    String status = result.status != null ? result.status.toUpperCase() : "";

                    if ("APPLIED".equals(status) || "APPROVED".equals(status)) {
                        removeLocalStudent(studentServerId);
                        Toast.makeText(DeleteStudentActivity.this, "Student deleted successfully", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                        return;
                    } else {
                        String errorMsg = result.message;
                        if (TextUtils.isEmpty(errorMsg) && result.data != null) {
                            Object msgObj = result.data.get("message");
                            if (msgObj != null) errorMsg = msgObj.toString();
                        }
                        if (TextUtils.isEmpty(errorMsg)) {
                            errorMsg = "Failed to delete student";
                        }

                        Toast.makeText(DeleteStudentActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        if (progressLoading != null) progressLoading.setVisibility(View.GONE);
                        if (btnDelete != null) btnDelete.setEnabled(true);
                        if (btnCancel != null) btnCancel.setEnabled(true);
                        return;
                    }
                }

                removeLocalStudentOffline(studentServerId);
                Toast.makeText(DeleteStudentActivity.this, "Saved locally — delete will sync when online", Toast.LENGTH_LONG).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onFailure(@NonNull Call<SyncResponseEnvelope> call, @NonNull Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                removeLocalStudentOffline(studentServerId);
                Toast.makeText(DeleteStudentActivity.this, "Saved locally — delete will sync when online", Toast.LENGTH_LONG).show();
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    private void removeLocalStudent(long serverId) {
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        Executors.newSingleThreadExecutor().execute(() -> {
            StudentEntity student = db.studentDao().findByServerId(serverId);
            if (student != null) {
                db.studentDao().delete(student);
            }
        });
    }

    private void removeLocalStudentOffline(long serverId) {
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        StudentSyncRepository repository = new StudentSyncRepository(db);

        Executors.newSingleThreadExecutor().execute(() -> {
            StudentEntity student = db.studentDao().findByServerId(serverId);
            if (student != null) {
                long accountId = sessionManager.getAccountId();
                repository.submitDelete(student.localId, accountId);
            }
        });
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
