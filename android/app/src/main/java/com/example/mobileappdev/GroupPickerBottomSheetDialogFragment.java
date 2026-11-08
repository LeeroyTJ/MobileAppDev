package com.example.mobileappdev;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.mobileappdev.data.local.AppDatabase;
import com.example.mobileappdev.data.remote.ApiClient;
import com.example.mobileappdev.session.SessionManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.HashMap;
import java.util.List;
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
    private boolean performAssign = true;

    public static GroupPickerBottomSheetDialogFragment newInstance(long studentServerId) {
        return newInstance(studentServerId, true);
    }

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

        LinearLayout container = view.findViewById(R.id.groupOptionsContainer);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancelPicker);

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dismiss());

        fetchAndBuildGroups(container);
    }

    private void fetchAndBuildGroups(LinearLayout container) {
        if (getContext() == null) return;

        ApiClient.getGroupService(requireContext()).listGroups().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    List<?> groupList = (List<?>) response.body().get("data");
                    if (groupList != null && !groupList.isEmpty()) {
                        buildOnlineGroupCards(container, groupList);
                        return;
                    }
                }
                // Fallback if response is bad
                loadCachedGroups(container);
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                // Offline! Build from cached Room data.
                loadCachedGroups(container);
            }
        });
    }

    private void buildOnlineGroupCards(LinearLayout container, List<?> groupList) {
        container.removeAllViews();
        for (Object item : groupList) {
            if (item instanceof Map) {
                Map<?, ?> groupMap = (Map<?, ?>) item;
                String code = groupMap.get("group_code") != null ? groupMap.get("group_code").toString() : "";
                long capacity = groupMap.get("capacity") instanceof Number ? ((Number) groupMap.get("capacity")).longValue() : 15;
                long occupied = groupMap.get("occupied") instanceof Number ? ((Number) groupMap.get("occupied")).longValue() : 0;
                long groupId = groupMap.get("group_id") instanceof Number ? ((Number) groupMap.get("group_id")).longValue() : 0;

                if (code.isEmpty()) continue;
                addRow(container, code, capacity, occupied, groupId, true);
            }
        }
    }

    private void loadCachedGroups(LinearLayout container) {
        AppDatabase db = AppDatabase.getInstance(requireContext());
        long accountId = SessionManager.getInstance(requireContext()).getAccountId();

        Executors.newSingleThreadExecutor().execute(() -> {
            // Query Room for the distinct groups that currently have students
            List<String> cachedGroups = db.studentDao().getDistinctGroupsForAccount(accountId);

            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                container.removeAllViews();
                if (cachedGroups == null || cachedGroups.isEmpty()) {
                    Toast.makeText(requireContext(), "No groups found in cache", Toast.LENGTH_SHORT).show();
                    return;
                }

                for (String code : cachedGroups) {
                    // We don't know the capacity/occupancy offline, so we pass 0 for both.
                    // The user can still select the group.
                    addRow(container, code, 15, 0, 0, false);
                }
            });
        });
    }

    private void addRow(LinearLayout container, String code, long capacity, long occupied, long groupId, boolean isOnline) {
        View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_group_picker_row, container, false);
        TextView tvName = row.findViewById(R.id.tvGroupName);
        TextView tvOccupancy = row.findViewById(R.id.tvGroupOccupancy);
        MaterialCardView card = (MaterialCardView) row;

        tvName.setText("Lab Group " + code);

        if (isOnline) {
            tvOccupancy.setText(occupied + " / " + capacity);
            boolean isFull = occupied >= capacity;
            if (isFull) {
                card.setEnabled(false);
                card.setAlpha(0.5f);
                card.setClickable(false);
            } else {
                card.setOnClickListener(v -> selectGroup(code, groupId));
            }
        } else {
            // Offline view
            tvOccupancy.setText("Offline");
            card.setOnClickListener(v -> selectGroup(code, groupId));
        }

        container.addView(row);
    }

    private void selectGroup(String groupCode, long groupId) {
        if (!performAssign) {
            if (listener != null) listener.onGroupSelected(groupCode);
            dismiss();
            return;
        }

        if (studentServerId <= 0) {
            if (listener != null) listener.onGroupSelected(groupCode);
            dismiss();
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("studentId", studentServerId);

        ApiClient.getGroupService(requireContext()).assignStudent(groupId, body).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null && Boolean.TRUE.equals(response.body().get("success"))) {
                    Toast.makeText(requireContext(), "Successfully assigned to " + groupCode, Toast.LENGTH_SHORT).show();
                    if (listener != null) listener.onGroupSelected(groupCode);
                } else {
                    Toast.makeText(requireContext(), "Failed to assign to " + groupCode, Toast.LENGTH_SHORT).show();
                }
                dismiss();
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
                dismiss();
            }
        });
    }
}