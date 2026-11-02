package com.example.mobileappdev;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.mobileappdev.data.remote.ApiClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddGroupDialogFragment extends DialogFragment {

    public static final String TAG = "AddGroupDialogFragment";
    public static final String RESULT_KEY = "add_group_result";
    public static final String EXTRA_SUCCESS = "extra_success";

    private TextInputEditText etGroupCode;
    private MaterialButton btnSaveGroup;
    private MaterialButton btnCancelGroup;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_add_group, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etGroupCode = view.findViewById(R.id.etGroupCode);
        btnSaveGroup = view.findViewById(R.id.btnSaveGroup);
        btnCancelGroup = view.findViewById(R.id.btnCancelGroup);

        btnCancelGroup.setOnClickListener(v -> dismiss());
        btnSaveGroup.setOnClickListener(v -> attemptCreateGroup());
    }

    private void attemptCreateGroup() {
        String code = etGroupCode.getText() != null ? etGroupCode.getText().toString().trim().toUpperCase() : "";

        if (TextUtils.isEmpty(code)) {
            etGroupCode.setError("Group code is required");
            return;
        }

        // Basic format check: one letter followed by digits (e.g., G14)
        if (!code.matches("^[A-Z]\\d{1,3}$")) {
            etGroupCode.setError("Format must be like G14 or G05");
            return;
        }

        btnSaveGroup.setEnabled(false);
        btnSaveGroup.setText("Creating...");

        Map<String, Object> body = new HashMap<>();
        body.put("groupCode", code);

        ApiClient.getGroupService(requireContext()).createGroup(body).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null
                        && Boolean.TRUE.equals(response.body().get("success"))) {
                    Toast.makeText(requireContext(), code + " created successfully", Toast.LENGTH_SHORT).show();
                    notifyRosterToRefresh();
                    dismiss();
                } else {
                    String msg = "Failed to create group";
                    if (response.body() != null) {
                        Object err = response.body().get("error");
                        if (err instanceof Map) {
                            Object m = ((Map<?, ?>) err).get("message");
                            if (m != null) msg = m.toString();
                        }
                    } else if (response.code() == 409) {
                        msg = "A group with this code already exists";
                    }
                    resetSaveButton();
                    etGroupCode.setError(msg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                resetSaveButton();
                Toast.makeText(requireContext(),
                        "Network error — check your connection", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void resetSaveButton() {
        btnSaveGroup.setEnabled(true);
        btnSaveGroup.setText("Create");
    }

    private void notifyRosterToRefresh() {
        Bundle result = new Bundle();
        result.putBoolean(EXTRA_SUCCESS, true);
        getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
    }
}