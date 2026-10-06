package com.example.mobileappdev;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobileappdev.data.local.entity.StudentEntity;
import com.google.android.material.button.MaterialButton;

public class StudentAdapter extends ListAdapter<StudentEntity, StudentAdapter.StudentViewHolder> {

    public interface OnStudentClickListener {
        void onStudentClick(StudentEntity student);
        void onStudentOptionsClick(StudentEntity student, View anchorView);
    }

    private final OnStudentClickListener listener;

    public StudentAdapter(OnStudentClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<StudentEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<StudentEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull StudentEntity oldItem, @NonNull StudentEntity newItem) {
                    return oldItem.localId == newItem.localId;
                }

                @Override
                public boolean areContentsTheSame(@NonNull StudentEntity oldItem, @NonNull StudentEntity newItem) {
                    return oldItem.localId == newItem.localId
                            && TextUtils.equals(oldItem.name, newItem.name)
                            && TextUtils.equals(oldItem.studentNumber, newItem.studentNumber)
                            && TextUtils.equals(oldItem.programme, newItem.programme)
                            && TextUtils.equals(oldItem.labGroup, newItem.labGroup)
                            && TextUtils.equals(oldItem.syncStatus, newItem.syncStatus);
                }
            };

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        StudentEntity student = getItem(position);
        holder.bind(student, listener);
    }

    static class StudentViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvInitials;
        private final TextView tvName;
        private final TextView tvNumber;
        private final TextView tvProgramme;
        private final TextView tvGroup;
        private final MaterialButton btnAction;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitials = itemView.findViewById(R.id.student_initials);
            tvName = itemView.findViewById(R.id.student_name);
            tvNumber = itemView.findViewById(R.id.student_number);
            tvProgramme = itemView.findViewById(R.id.student_programme);
            tvGroup = itemView.findViewById(R.id.student_group);
            btnAction = itemView.findViewById(R.id.student_action);
        }

        public void bind(StudentEntity student, OnStudentClickListener listener) {
            tvName.setText(student.name);
            tvNumber.setText(student.studentNumber);
            tvProgramme.setText(student.programme);
            tvGroup.setText(student.labGroup != null ? student.labGroup : "Unassigned");
            tvInitials.setText(getInitials(student.name));

            String groupStr = student.labGroup != null ? student.labGroup : "Unassigned";
            String statusStr = student.syncStatus != null ? student.syncStatus : "";

            if (student.labGroup != null) {
                itemView.setContentDescription(
                        itemView.getContext().getString(
                                R.string.student_row_description,
                                student.name, student.studentNumber, student.programme, groupStr, statusStr
                        )
                );
            } else {
                itemView.setContentDescription(
                        itemView.getContext().getString(
                                R.string.student_row_description_unassigned,
                                student.name, student.studentNumber, student.programme, statusStr
                        )
                );
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStudentClick(student);
                }
            });

            btnAction.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStudentOptionsClick(student, btnAction);
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
}
