package com.example.mobileappdev;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.mobileappdev.data.local.AppDatabase;
import com.example.mobileappdev.data.local.entity.StudentEntity;
import com.example.mobileappdev.data.remote.ApiClient;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.Map;

import java.util.concurrent.Executors;

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

    public static GroupPickerBottomSheetDialogFragment newInstance(long studentServerId) {
        GroupPickerBottomSheetDialogFragment fragment = new GroupPickerBottomSheetDialogFragment();
        Bundle args = new Bundle();
        args.putLong("student_server_id", studentServerId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            studentServerId = getArguments().getLong("student_server_id", 0);
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
        MaterialButton btnCancel = view.findViewById(R.id.btnCancelPicker);

        if (g01 != null) g01.setOnClickListener(v -> selectGroup("G01", 1));
        if (g02 != null) g02.setOnClickListener(v -> selectGroup("G02", 2));
        if (g03 != null) g03.setOnClickListener(v -> selectGroup("G03", 3));
        if (g04 != null) g04.setOnClickListener(v -> selectGroup("G04", 4));
        if (btnCancel != null) btnCancel.setOnClickListener(v -> dismiss());
    }

    private void selectGroup(String groupCode, long groupId) {
        if (studentServerId <= 0) {
            if (listener != null) {
                listener.onGroupSelected(groupCode);
            } else {
                Toast.makeText(requireContext(), "Selected " + groupCode, Toast.LENGTH_SHORT).show();
            }
            dismiss();
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("studentId", studentServerId);

        ApiClient.getGroupService(requireContext()).assignStudent(groupId, body).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null
                        && Boolean.TRUE.equals(response.body().get("success"))) {
                    Toast.makeText(requireContext(), "Successfully assigned student to group " + groupCode, Toast.LENGTH_SHORT).show();
                    updateLocalRoomStudentGroup(studentServerId, groupCode);
                    if (listener != null) listener.onGroupSelected(groupCode);
                } else if (response.code() == 409) {
                    Toast.makeText(requireContext(), "Group " + groupCode + " is full (15/15 max capacity)", Toast.LENGTH_LONG).show();
                } else {
                    updateLocalRoomStudentGroup(studentServerId, groupCode);
                    Toast.makeText(requireContext(), "Group assignment saved locally", Toast.LENGTH_SHORT).show();
                    if (listener != null) listener.onGroupSelected(groupCode);
                }
                dismiss();
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                updateLocalRoomStudentGroup(studentServerId, groupCode);
                Toast.makeText(requireContext(), "Group assignment saved locally", Toast.LENGTH_SHORT).show();
                if (listener != null) listener.onGroupSelected(groupCode);
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
