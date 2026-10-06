package com.example.mobileappdev.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobileappdev.R;
import com.example.mobileappdev.model.Student;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {

    public interface OnStudentClickListener {
        void onStudentClick(Student student);
    }

    private final List<Student> students = new ArrayList<>();
    private final OnStudentClickListener listener;

    public StudentAdapter(OnStudentClickListener listener) {
        this.listener = listener;
    }

    public void setStudents(List<Student> newStudents) {
        this.students.clear();
        if (newStudents != null) {
            this.students.addAll(newStudents);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        Student student = students.get(position);
        holder.bind(student, listener);
    }

    @Override
    public int getItemCount() {
        return students.size();
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

        public void bind(Student student, OnStudentClickListener listener) {
            tvInitials.setText(student.getInitials());
            tvName.setText(student.getName());
            tvNumber.setText(student.getStudentNumber());
            tvProgramme.setText(student.getProgrammeName());
            tvGroup.setText(student.getGroupCode());

            View.OnClickListener clickListener = v -> {
                if (listener != null) {
                    listener.onStudentClick(student);
                }
            };

            itemView.setOnClickListener(clickListener);
            if (btnAction != null) {
                btnAction.setOnClickListener(clickListener);
            }
        }
    }
}
