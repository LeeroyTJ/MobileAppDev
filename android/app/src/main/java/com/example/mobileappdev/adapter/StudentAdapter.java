package com.example.mobileappdev.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobileappdev.R;
import com.example.mobileappdev.data.local.entity.StudentEntity;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for item_student.xml with card formatting,
 * avatar initials rendering, TalkBack accessibility, and group badge colors.
 */
public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {

    // Callback interface fired when a student row itself is tapped (e.g. open profile/editor)
    public interface OnStudentClickListener {
        void onStudentClick(StudentEntity student);
        default void onStudentOptionsClick(StudentEntity student, View anchorView) {}
    }

    // Callback interface fired when the row's action button (3-dot menu, edit icon, etc.) is tapped
    public interface OnStudentActionListener {
        void onStudentActionClick(StudentEntity student, View view);
    }

    private final List<StudentEntity> students = new ArrayList<>();
    private OnStudentClickListener clickListener;
    private OnStudentActionListener actionListener;

    public StudentAdapter() {
    }

    public StudentAdapter(OnStudentClickListener listener) {
        this.clickListener = listener;
    }

    public StudentAdapter(@Nullable List<StudentEntity> initialStudents) {
        if (initialStudents != null) {
            this.students.addAll(initialStudents);
        }
    }

    public void setOnStudentClickListener(OnStudentClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnStudentActionListener(OnStudentActionListener listener) {
        this.actionListener = listener;
    }

    public void setStudents(@Nullable List<StudentEntity> newStudents) {
        this.students.clear();
        if (newStudents != null) {
            this.students.addAll(newStudents);
        }
        notifyDataSetChanged();
    }

    public void submitList(@Nullable List<StudentEntity> newStudents) {
        setStudents(newStudents);
    }

    public List<StudentEntity> getStudents() {
        return students;
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        StudentEntity student = students.get(position);
        holder.bind(student, clickListener, actionListener);
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    public static class StudentViewHolder extends RecyclerView.ViewHolder {
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

        public void bind(
                StudentEntity student,
                @Nullable OnStudentClickListener clickListener,
                @Nullable OnStudentActionListener actionListener
        ) {
            Context context = itemView.getContext();

            // 1. Name & Student Number
            String name = student.name != null ? student.name : "";
            String number = student.studentNumber != null ? student.studentNumber : "";
            tvName.setText(name);
            tvNumber.setText(number);

            // 2. Avatar Initials (e.g. "Thabo Jumbe" -> "TJ")
            tvInitials.setText(extractInitials(name));

            // 3. Programme
            String programme = student.programme != null ? student.programme : "";
            tvProgramme.setText(programme);

            // 4. Lab Group badge & color styling
            String group = student.labGroup;
            boolean isUnassigned = TextUtils.isEmpty(group)
                    || "Unassigned".equalsIgnoreCase(group)
                    || "NULL".equalsIgnoreCase(group);
            String groupDisplay = isUnassigned ? context.getString(R.string.group_unassigned) : group;

            tvGroup.setText(groupDisplay);
            applyGroupBadgeColor(tvGroup, isUnassigned ? "UNASSIGNED" : group);

            // 5. Accessibility TalkBack Description
            String syncLabel = getSyncStatusLabel(context, student.syncStatus);
            if (isUnassigned) {
                itemView.setContentDescription(context.getString(
                        R.string.student_row_description_unassigned,
                        name, number, programme, syncLabel
                ));
            } else {
                itemView.setContentDescription(context.getString(
                        R.string.student_row_description,
                        name, number, programme, groupDisplay, syncLabel
                ));
            }

            // 6. Click Listeners
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onStudentClick(student);
                }
            });

            if (btnAction != null) {
                btnAction.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onStudentActionClick(student, v);
                    } else if (clickListener != null) {
                        clickListener.onStudentOptionsClick(student, v);
                    }
                });
            }
        }

        private static String extractInitials(String name) {
            if (TextUtils.isEmpty(name) || "null".equalsIgnoreCase(name.trim())) {
                return "?";
            }
            String[] parts = name.trim().split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                if (!part.isEmpty()) {
                    sb.append(Character.toUpperCase(part.charAt(0)));
                }
                if (sb.length() >= 2) {
                    break;
                }
            }
            return sb.length() > 0 ? sb.toString() : "?";
        }

        private static void applyGroupBadgeColor(TextView groupView, String groupCode) {
            Context context = groupView.getContext();
            int bgColor;
            int textColor;

            if (groupCode == null) {
                groupCode = "";
            }

            switch (groupCode.toUpperCase().trim()) {
                case "G01":
                    bgColor = Color.parseColor("#E3F2FD"); // Light blue
                    textColor = Color.parseColor("#1565C0"); // Dark blue
                    break;
                case "G02":
                    bgColor = Color.parseColor("#E8F5E9"); // Light green
                    textColor = Color.parseColor("#2E7D32"); // Dark green
                    break;
                case "G03":
                    bgColor = Color.parseColor("#FFF3E0"); // Light orange
                    textColor = Color.parseColor("#E65100"); // Dark orange
                    break;
                case "G04":
                    bgColor = Color.parseColor("#F3E5F5"); // Light purple
                    textColor = Color.parseColor("#6A1B9A"); // Dark purple
                    break;
                case "UNASSIGNED":
                default:
                    bgColor = Color.parseColor("#EEEEEE"); // Light grey
                    textColor = Color.parseColor("#616161"); // Dark grey
                    break;
            }

            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.RECTANGLE);
            float density = context.getResources().getDisplayMetrics().density;
            drawable.setCornerRadius(12 * density);
            drawable.setColor(bgColor);

            groupView.setBackground(drawable);
            groupView.setTextColor(textColor);

            int paddingH = (int) (8 * density);
            int paddingV = (int) (3 * density);
            groupView.setPadding(paddingH, paddingV, paddingH, paddingV);
        }

        private static String getSyncStatusLabel(Context context, String syncStatus) {
            if (syncStatus == null) {
                return context.getString(R.string.sync_saved_locally);
            }
            switch (syncStatus) {
                case StudentEntity.STATUS_PENDING:
                    return context.getString(R.string.sync_pending);
                case StudentEntity.STATUS_SYNCING:
                    return context.getString(R.string.sync_syncing);
                case StudentEntity.STATUS_SYNCED:
                    return context.getString(R.string.sync_synced);
                case StudentEntity.STATUS_ACTION_REQUIRED:
                    return context.getString(R.string.sync_action_required);
                case StudentEntity.STATUS_SAVED_LOCALLY:
                default:
                    return context.getString(R.string.sync_saved_locally);
            }
        }
    }
}