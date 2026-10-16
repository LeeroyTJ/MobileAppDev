package com.example.mobileappdev;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.mobileappdev.data.remote.ApiClient;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.example.mobileappdev.data.local.AppDatabase;
import com.example.mobileappdev.data.local.entity.StudentEntity;
import java.util.concurrent.Executors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GroupPickerBottomSheetDialogFragment extends BottomSheetDialogFragment {

    public static final String TAG = "GroupPickerBottomSheet";

    public interface OnGroupSelectedListener {
        void onGroupSelected(String groupCode);
    }

    private OnGroupSelectedListener listener;
    private long studentServerId = 0;

    /**
     * When true, picking a group performs a real assignment via
     * POST /groups/:groupId/assign (lecturer flow, capacity-checked server-side).
     * When false, picking a group only reports the choice to the listener
     * (student "request group change" flow, which queues locally).
     */
    private boolean performAssign = true;

    private TextView tvG01Occupancy;
    private TextView tvG02Occupancy;
    private TextView tvG03Occupancy;
    private TextView tvG04Occupancy;

    /** Lecturer-flow factory: picking a group assigns it. */
    public static GroupPickerBottomSheetDialogFragment newInstance(long studentServerId) {
        return newInstance(studentServerId, true);
    }

    /** Explicit factory: set performAssign = false for the student request flow. */
    public static GroupPickerBottomSheetDialogFragment newInstance(long studentServerId, boolean performAssign) {
        GroupPickerBottomSheetDialogFragment fragment = new GroupPickerBottomSheetDialogFragment();
        Bundle args = new Bundle();
        args.putLong("student_server_id", studentServerId);
        args.putBoolean("perform_assign", performAssign);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            studentServerId = getArguments().getLong("student_server_id", 0);
            performAssign = getArguments().getBoolean("perform_assign", true);
        }
    }

    public void setOnGroupSelectedListener(OnGroupSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_group_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View g01 = view.findViewById(R.id.cardGroupG01);
        View g02 = view.findViewById(R.id.cardGroupG02);
        View g03 = view.findViewById(R.id.cardGroupG03);
        View g04 = view.findViewById(R.id.cardGroupG04);

        tvG01Occupancy = view.findViewById(R.id.tvG01Occupancy);
        tvG02Occupancy = view.findViewById(R.id.tvG02Occupancy);
        tvG03Occupancy = view.findViewById(R.id.tvG03Occupancy);
        tvG04Occupancy = view.findViewById(R.id.tvG04Occupancy);

        MaterialButton btnCancel = view.findViewById(R.id.btnCancelPicker);

        if (g01 != null) g01.setOnClickListener(v -> selectGroup("G01", 1));
        if (g02 != null) g02.setOnClickListener(v -> selectGroup("G02", 2));
        if (g03 != null) g03.setOnClickListener(v -> selectGroup("G03", 3));
        if (g04 != null) g04.setOnClickListener(v -> selectGroup("G04", 4));
        if (btnCancel != null) btnCancel.setOnClickListener(v -> dismiss());

        fetchGroupCapacities();
    }

    private void fetchGroupCapacities() {
        if (getContext() == null) return;

        ApiClient.getGroupService(requireContext()).listGroups().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (!isAdded() || !response.isSuccessful() || response.body() == null
                        || !Boolean.TRUE.equals(response.body().get("success"))) {
                    return;
                }

                Object dataObj = response.body().get("data");
                if (!(dataObj instanceof List)) return;

                for (Object item : (List<?>) dataObj) {
                    if (item instanceof Map) {
                        Map<?, ?> groupMap = (Map<?, ?>) item;
                        Object codeObj = groupMap.get("group_code");
                        Object occupiedObj = groupMap.get("occupied");
                        Object capacityObj = groupMap.get("capacity");

                        String code = codeObj != null ? codeObj.toString() : "";
                        long occupied = (occupiedObj instanceof Number) ? ((Number) occupiedObj).longValue() : 0;
                        long capacity = (capacityObj instanceof Number) ? ((Number) capacityObj).longValue() : 15;

                        String text = occupied + " / " + capacity;

                        if ("G01".equalsIgnoreCase(code) && tvG01Occupancy != null) {
                            tvG01Occupancy.setText(text);
                        } else if ("G02".equalsIgnoreCase(code) && tvG02Occupancy != null) {
                            tvG02Occupancy.setText(text);
                        } else if ("G03".equalsIgnoreCase(code) && tvG03Occupancy != null) {
                            tvG03Occupancy.setText(text);
                        } else if ("G04".equalsIgnoreCase(code) && tvG04Occupancy != null) {
                            tvG04Occupancy.setText(text);
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                // If fetch fails, placeholders ("-- / --") remain visible.
            }
        });
    }

    private void selectGroup(String groupCode, long groupId) {
        // Path 1 — pick only. Used by the student's "Request group change" flow.
        if (!performAssign) {
            if (listener != null) {
                listener.onGroupSelected(groupCode);
            }
            dismiss();
            return;
        }

        // Path 2 — student has no server id, so we can't assign. Just report the choice.
        if (studentServerId <= 0) {
            if (listener != null) {
                listener.onGroupSelected(groupCode);
            } else {
                Toast.makeText(requireContext(), "Selected " + groupCode, Toast.LENGTH_SHORT).show();
            }
            dismiss();
            return;
        }

        // Path 3 — lecturer assignment. Goes through POST /groups/:id/assign,
        // which enforces the 15-cap under a row lock (SELECT ... FOR UPDATE).
        Map<String, Object> body = new HashMap<>();
        body.put("studentId", studentServerId);

        ApiClient.getGroupService(requireContext()).assignStudent(groupId, body).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null
                        && Boolean.TRUE.equals(response.body().get("success"))) {
                    Toast.makeText(requireContext(),
                            "Successfully assigned student to group " + groupCode, Toast.LENGTH_SHORT).show();
                    updateLocalRoomStudentGroup(studentServerId, groupCode);
                    if (listener != null) listener.onGroupSelected(groupCode);
                } else if (response.code() == 409) {
                    Toast.makeText(requireContext(),
                            "Group " + groupCode + " is full (15/15 max capacity)", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(),
                            "Couldn't assign this student to " + groupCode, Toast.LENGTH_SHORT).show();
                }
                dismiss();
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                Toast.makeText(requireContext(),
                        "Network error — couldn't assign this student to " + groupCode,
                        Toast.LENGTH_SHORT).show();
                dismiss();
            }
        });
    }

    private void updateLocalRoomStudentGroup(long serverId, String groupCode) {
        if (getContext() == null || serverId <= 0) return;
        AppDatabase db = AppDatabase.getInstance(getContext().getApplicationContext());
        Executors.newSingleThreadExecutor().execute(() -> {
            StudentEntity student = db.studentDao().findByServerId(serverId);
            if (student != null) {
                student.labGroup = groupCode;
                db.studentDao().update(student);
            }
        });
    }
}