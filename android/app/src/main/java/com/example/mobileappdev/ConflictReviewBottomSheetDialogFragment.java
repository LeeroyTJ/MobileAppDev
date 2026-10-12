package com.example.mobileappdev;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.mobileappdev.data.local.AppDatabase;
import com.example.mobileappdev.data.local.entity.PendingOperationEntity;
import com.example.mobileappdev.data.remote.ApiClient;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConflictReviewBottomSheetDialogFragment extends BottomSheetDialogFragment {

    public static final String TAG = "ConflictReviewBottomSheet";

    private static final String ARG_OP_ID = "operationId";
    private static final String ARG_PAYLOAD = "payloadJson";
    private static final String ARG_BASE_VERSION = "baseVersion";
    private static final String ARG_ENTITY_ID = "entityId";
    private static final String ARG_OP_TYPE = "operationType";

    private String operationId;
    private String payloadJson;
    private int baseVersion;
    private long entityId;
    private String operationType;

    public static ConflictReviewBottomSheetDialogFragment newInstance(PendingOperationEntity op) {
        ConflictReviewBottomSheetDialogFragment fragment = new ConflictReviewBottomSheetDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_OP_ID, op.operationId);
        args.putString(ARG_PAYLOAD, op.payloadJson != null ? op.payloadJson : "{}");
        args.putInt(ARG_BASE_VERSION, op.baseVersion);
        args.putLong(ARG_ENTITY_ID, op.entityId != null ? op.entityId : 0);
        args.putString(ARG_OP_TYPE, op.operationType != null ? op.operationType : "");
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            operationId = getArguments().getString(ARG_OP_ID, "");
            payloadJson = getArguments().getString(ARG_PAYLOAD, "{}");
            baseVersion = getArguments().getInt(ARG_BASE_VERSION, 0);
            entityId = getArguments().getLong(ARG_ENTITY_ID, 0);
            operationType = getArguments().getString(ARG_OP_TYPE, "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_conflict_review, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvClientProposal = view.findViewById(R.id.tvClientProposal);
        TextView tvServerRecord = view.findViewById(R.id.tvServerRecord);
        MaterialButton btnKeepServer = view.findViewById(R.id.btnKeepServer);
        MaterialButton btnRetryMine = view.findViewById(R.id.btnRetryMine);

        String clientSummary = summarizePayload();
        if (tvClientProposal != null) tvClientProposal.setText(clientSummary);

        if (tvServerRecord != null) {
            tvServerRecord.setText("Loading server record...");
            fetchServerRecord(tvServerRecord);
        }

        if (btnKeepServer != null) {
            btnKeepServer.setOnClickListener(v -> {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> {
                    AppDatabase db = AppDatabase.getInstance(requireContext());
                    PendingOperationEntity op = db.pendingOperationDao().findById(operationId);
                    if (op != null) {
                        op.status = PendingOperationEntity.STATUS_SYNCED;
                        db.pendingOperationDao().update(op);
                    }
                    executor.shutdown();
                });
                Toast.makeText(requireContext(), "Server record accepted", Toast.LENGTH_SHORT).show();
                dismiss();
            });
        }

        if (btnRetryMine != null) {
            btnRetryMine.setOnClickListener(v -> retryLocalEdit());
        }
    }

    private void fetchServerRecord(TextView tvServerRecord) {
        if (entityId <= 0) {
            tvServerRecord.setText("Server record unavailable — no student ID");
            return;
        }

        ApiClient.getStudentService(requireContext()).getMyProfile()
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<Map<String, Object>> call,
                                           @NonNull Response<Map<String, Object>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && Boolean.TRUE.equals(response.body().get("success"))) {
                            Map<?, ?> data = (Map<?, ?>) response.body().get("data");
                            if (data != null) {
                                StringBuilder sb = new StringBuilder();
                                String name = data.get("student_name") != null
                                        ? data.get("student_name").toString() : "N/A";
                                String number = data.get("student_number") != null
                                        ? data.get("student_number").toString() : "N/A";
                                String group = data.get("group_code") != null
                                        ? data.get("group_code").toString() : "Unassigned";
                                Object versionObj = data.get("version");
                                String version = versionObj != null ? versionObj.toString() : "?";

                                sb.append("Name: ").append(name).append("\n");
                                sb.append("Number: ").append(number).append("\n");
                                sb.append("Group: ").append(group).append("\n");
                                sb.append("Version: ").append(version);
                                tvServerRecord.setText(sb.toString());
                            } else {
                                tvServerRecord.setText("Server record unavailable");
                            }
                        } else if (response.code() == 401 || response.code() == 403) {
                            tvServerRecord.setText("Session expired — sign in again");
                        } else {
                            tvServerRecord.setText("Server record unavailable");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Map<String, Object>> call,
                                          @NonNull Throwable t) {
                        tvServerRecord.setText("Couldn't reach server — showing version "
                                + baseVersion);
                    }
                });
    }

    private String summarizePayload() {
        try {
            JSONObject payload = new JSONObject(payloadJson);
            StringBuilder sb = new StringBuilder();
            String[] keys = {"studentName", "student_name", "student_number",
                    "programme", "group_code", "correctedStudentNumber", "labGroup"};
            String[] labels = {"Name", "Name", "Number", "Programme",
                    "Group", "Corrected Number", "Lab Group"};
            for (int i = 0; i < keys.length; i++) {
                if (payload.has(keys[i])) {
                    if (sb.length() > 0) sb.append("\n");
                    sb.append(labels[i]).append(": ").append(payload.getString(keys[i]));
                }
            }
            return sb.length() > 0 ? sb.toString() : "Your local edit (version " + baseVersion + ")";
        } catch (Exception e) {
            return "Your local edit (version " + baseVersion + ")";
        }
    }

    private void retryLocalEdit() {
        if (entityId <= 0) {
            Toast.makeText(requireContext(), "Cannot retry — missing student ID", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject payload = new JSONObject(payloadJson);
            Map<String, Object> body = new HashMap<>();

            if (payload.has("studentName")) {
                body.put("studentName", payload.get("studentName"));
            } else if (payload.has("student_name")) {
                body.put("studentName", payload.get("student_name"));
            }

            if (payload.has("correctedStudentNumber")) {
                body.put("student_number", payload.get("correctedStudentNumber"));
            }

            body.put("baseVersion", baseVersion + 1);

            ApiClient.getStudentService(requireContext()).updateStudent(entityId, body)
                    .enqueue(new Callback<>() {
                        @Override
                        public void onResponse(@NonNull Call<Map<String, Object>> call,
                                               @NonNull Response<Map<String, Object>> response) {
                            if (response.isSuccessful() && response.body() != null
                                    && Boolean.TRUE.equals(response.body().get("success"))) {
                                ExecutorService executor = Executors.newSingleThreadExecutor();
                                executor.execute(() -> {
                                    AppDatabase db = AppDatabase.getInstance(requireContext());
                                    PendingOperationEntity op = db.pendingOperationDao().findById(operationId);
                                    if (op != null) {
                                        op.status = PendingOperationEntity.STATUS_SYNCED;
                                        db.pendingOperationDao().update(op);
                                    }
                                    executor.shutdown();
                                });
                                Toast.makeText(requireContext(),
                                        "Local edit applied successfully", Toast.LENGTH_SHORT).show();
                                dismiss();
                            } else if (response.code() == 409) {
                                Toast.makeText(requireContext(),
                                        "Still conflicting — server has a newer version. Accept server record instead.",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(requireContext(),
                                        "Retry failed — try again later", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Map<String, Object>> call,
                                              @NonNull Throwable t) {
                            ExecutorService executor = Executors.newSingleThreadExecutor();
                            executor.execute(() -> {
                                AppDatabase db = AppDatabase.getInstance(requireContext());
                                PendingOperationEntity op = db.pendingOperationDao().findById(operationId);
                                if (op != null) {
                                    op.status = PendingOperationEntity.STATUS_PENDING;
                                    db.pendingOperationDao().update(op);
                                }
                                executor.shutdown();
                            });
                            Toast.makeText(requireContext(),
                                    "Couldn't reach server — marked for retry on next sync",
                                    Toast.LENGTH_LONG).show();
                            dismiss();
                        }
                    });
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Could not parse local edit data", Toast.LENGTH_SHORT).show();
        }
    }
}
