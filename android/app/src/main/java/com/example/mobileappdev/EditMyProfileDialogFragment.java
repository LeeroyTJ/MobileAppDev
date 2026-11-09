package com.example.mobileappdev;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.mobileappdev.data.remote.ApiClient;
import com.example.mobileappdev.session.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditMyProfileDialogFragment extends DialogFragment {

    public static final String TAG = "EditMyProfileDialog";
    public static final String ARG_NAME = "arg_name";
    public static final String ARG_STUDENT_ID = "arg_student_id";
    public static final String ARG_BASE_VERSION = "arg_base_version";

    private String studentName;
    private long studentId;
    private int baseVersion;

    public static EditMyProfileDialogFragment newInstance(String name, long studentId, int baseVersion) {
        EditMyProfileDialogFragment f = new EditMyProfileDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_NAME, name);
        args.putLong(ARG_STUDENT_ID, studentId);
        args.putInt(ARG_BASE_VERSION, baseVersion);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            studentName = getArguments().getString(ARG_NAME, "");
            studentId = getArguments().getLong(ARG_STUDENT_ID, 0);
            baseVersion = getArguments().getInt(ARG_BASE_VERSION, 1);
        }
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

        TextInputEditText etName = view.findViewById(R.id.et_student_name);
        TextInputEditText etNumber = view.findViewById(R.id.et_student_number);
        View tilProgramme = view.findViewById(R.id.til_programme);
        View tilLabGroup = view.findViewById(R.id.til_lab_group);
        View tilNumber = view.findViewById(R.id.til_student_number);

        // Use View type for the close button to avoid ClassCastException
        View btnSave = view.findViewById(R.id.btn_save);
        View btnCancel = view.findViewById(R.id.btn_cancel);
        View btnClose = view.findViewById(R.id.btn_close_dialog);

        // Hide fields not editable by students
        if (tilProgramme != null) tilProgramme.setVisibility(View.GONE);
        if (tilLabGroup != null) tilLabGroup.setVisibility(View.GONE);
        if (tilNumber != null) tilNumber.setVisibility(View.GONE); // Hide number field entirely

        if (etName != null) etName.setText(studentName);
        if (etNumber != null) etNumber.setText(String.valueOf(studentId));

        // Setup buttons
        if (btnCancel != null) btnCancel.setOnClickListener(v -> dismiss());
        if (btnClose != null) btnClose.setOnClickListener(v -> dismiss());
        if (btnSave != null) btnSave.setOnClickListener(v -> saveChanges(etName));
    }

    private void saveChanges(TextInputEditText etName) {
        String newName = etName != null && etName.getText() != null
                ? etName.getText().toString().trim() : "";

        if (newName.isEmpty()) {
            etName.setError("Name cannot be empty");
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("studentName", newName);
        body.put("baseVersion", baseVersion);

        ApiClient.getStudentService(requireContext()).updateStudent(studentId, body).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                    Bundle result = new Bundle();
                    result.putString("new_name", newName);
                    getParentFragmentManager().setFragmentResult("profile_edit_result", result);
                    dismiss();
                } else if (response.code() == 409) {
                    Toast.makeText(requireContext(), "Record changed on server. Please refresh.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(), "Failed to update", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}