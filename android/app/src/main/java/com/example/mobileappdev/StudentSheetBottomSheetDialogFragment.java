package com.example.mobileappdev;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class StudentSheetBottomSheetDialogFragment extends BottomSheetDialogFragment {

    public static final String TAG = "StudentSheetBottomSheet";

    private String name;
    private String number;
    private String programme;
    private String group;
    private long studentServerId;

    public static StudentSheetBottomSheetDialogFragment newInstance(String name, String number, String programme, String group, long studentServerId) {
        StudentSheetBottomSheetDialogFragment fragment = new StudentSheetBottomSheetDialogFragment();
        Bundle args = new Bundle();
        args.putString(DeleteStudentActivity.EXTRA_STUDENT_NAME, name);
        args.putString(DeleteStudentActivity.EXTRA_STUDENT_NUMBER, number);
        args.putString(DeleteStudentActivity.EXTRA_STUDENT_PROGRAMME, programme);
        args.putString("student_group", group);
        args.putLong("student_server_id", studentServerId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            name = getArguments().getString(DeleteStudentActivity.EXTRA_STUDENT_NAME, "Student");
            number = getArguments().getString(DeleteStudentActivity.EXTRA_STUDENT_NUMBER, "202412345");
            programme = getArguments().getString(DeleteStudentActivity.EXTRA_STUDENT_PROGRAMME, "CS");
            group = getArguments().getString("student_group", "Unassigned");
            studentServerId = getArguments().getLong("student_server_id", 0);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_student, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvName = view.findViewById(R.id.tvSheetName);
        TextView tvMeta = view.findViewById(R.id.tvSheetMeta);
        TextView tvInitials = view.findViewById(R.id.tvSheetInitials);
        View btnClose = view.findViewById(R.id.btnCloseSheet);

        if (tvName != null) tvName.setText(name);
        if (tvMeta != null) tvMeta.setText(number + " • " + programme + " • " + (group != null ? group : "Unassigned"));
        if (tvInitials != null) tvInitials.setText(getInitials(name));
        if (btnClose != null) btnClose.setOnClickListener(v -> dismiss());

        View editDetails = view.findViewById(R.id.actionEditDetails);
        View changeGroup = view.findViewById(R.id.actionChangeGroup);
        View correctNumber = view.findViewById(R.id.actionCorrectNumber);
        View deleteStudent = view.findViewById(R.id.actionDeleteStudent);

        if (editDetails != null) {
            editDetails.setOnClickListener(v -> {
                dismiss();
                Intent intent = new Intent(requireContext(), EditStudentActivity.class);
                intent.putExtra(DeleteStudentActivity.EXTRA_STUDENT_NAME, name);
                intent.putExtra(DeleteStudentActivity.EXTRA_STUDENT_NUMBER, number);
                intent.putExtra(DeleteStudentActivity.EXTRA_STUDENT_PROGRAMME, programme);
                intent.putExtra("student_server_id", studentServerId);
                startActivity(intent);
            });
        }

        if (changeGroup != null) {
            changeGroup.setOnClickListener(v -> {
                dismiss();
                GroupPickerBottomSheetDialogFragment dialog = GroupPickerBottomSheetDialogFragment.newInstance(studentServerId);
                dialog.show(getParentFragmentManager(), GroupPickerBottomSheetDialogFragment.TAG);
            });
        }

        if (correctNumber != null) {
            correctNumber.setOnClickListener(v -> {
                dismiss();
                Toast.makeText(requireContext(), "Correction request opened for " + name, Toast.LENGTH_SHORT).show();
            });
        }

        if (deleteStudent != null) {
            deleteStudent.setOnClickListener(v -> {
                dismiss();
                Intent intent = new Intent(requireContext(), DeleteStudentActivity.class);
                intent.putExtra(DeleteStudentActivity.EXTRA_STUDENT_NAME, name);
                intent.putExtra(DeleteStudentActivity.EXTRA_STUDENT_NUMBER, number);
                intent.putExtra(DeleteStudentActivity.EXTRA_STUDENT_PROGRAMME, programme);
                startActivity(intent);
            });
        }
    }

    private String getInitials(String nameStr) {
        if (nameStr == null || nameStr.trim().isEmpty()) return "?";
        String[] parts = nameStr.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) sb.append(Character.toUpperCase(part.charAt(0)));
            if (sb.length() >= 2) break;
        }
        return sb.toString();
    }
}
