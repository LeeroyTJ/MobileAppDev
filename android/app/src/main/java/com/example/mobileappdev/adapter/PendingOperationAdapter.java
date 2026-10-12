package com.example.mobileappdev.adapter;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobileappdev.R;
import com.example.mobileappdev.data.local.entity.PendingOperationEntity;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PendingOperationAdapter extends RecyclerView.Adapter<PendingOperationAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(PendingOperationEntity operation);
    }

    private final List<PendingOperationEntity> operations = new ArrayList<>();
    private OnItemClickListener listener;

    public PendingOperationAdapter() {
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<PendingOperationEntity> newOperations) {
        operations.clear();
        if (newOperations != null) {
            operations.addAll(newOperations);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pending_operation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PendingOperationEntity op = operations.get(position);
        Context context = holder.itemView.getContext();

        holder.tvOpType.setText(getOperationTypeLabel(op.operationType));
        holder.iconOpType.setImageResource(getOperationIconRes(op.operationType));
        holder.tvStudentInfo.setText(extractStudentInfo(op));

        String statusLabel = getStatusLabel(op.status);
        holder.tvStatus.setText(statusLabel);
        holder.tvStatus.setTextColor(getStatusColor(context, op.status));

        holder.tvTimestamp.setText(getFormattedTimestamp(op.createdAt));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(op);
            }
        });
    }

    @Override
    public int getItemCount() {
        return operations.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView iconOpType;
        TextView tvOpType;
        TextView tvStudentInfo;
        TextView tvStatus;
        TextView tvTimestamp;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconOpType = itemView.findViewById(R.id.iconOpType);
            tvOpType = itemView.findViewById(R.id.tvOpType);
            tvStudentInfo = itemView.findViewById(R.id.tvStudentInfo);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }
    }

    public static String getOperationTypeLabel(String type) {
        if (type == null) return "Pending Operation";
        switch (type) {
            case PendingOperationEntity.TYPE_CREATE_STUDENT:
                return "New Student";
            case PendingOperationEntity.TYPE_UPDATE_STUDENT:
                return "Student Edit";
            case PendingOperationEntity.TYPE_DELETE_STUDENT:
                return "Delete Request";
            case PendingOperationEntity.TYPE_CORRECT_STUDENT_NUMBER:
                return "Student Number Correction";
            case PendingOperationEntity.TYPE_REQUEST_GROUP_CHANGE:
                return "Group Change Request";
            case PendingOperationEntity.TYPE_TRANSFER_GROUP:
                return "Group Transfer";
            case PendingOperationEntity.TYPE_ASSIGN_GROUP:
                return "Group Assignment";
            default:
                return type.replace("_", " ");
        }
    }

    public static int getOperationIconRes(String type) {
        if (type == null) return R.drawable.ic_refresh;
        switch (type) {
            case PendingOperationEntity.TYPE_CREATE_STUDENT:
                return R.drawable.ic_add;
            case PendingOperationEntity.TYPE_UPDATE_STUDENT:
                return R.drawable.ic_refresh;
            case PendingOperationEntity.TYPE_DELETE_STUDENT:
                return R.drawable.ic_check_circle;
            case PendingOperationEntity.TYPE_CORRECT_STUDENT_NUMBER:
                return R.drawable.ic_edit;
            case PendingOperationEntity.TYPE_REQUEST_GROUP_CHANGE:
            case PendingOperationEntity.TYPE_TRANSFER_GROUP:
                return R.drawable.ic_group;
            case PendingOperationEntity.TYPE_ASSIGN_GROUP:
                return R.drawable.ic_person;
            default:
                return R.drawable.ic_info;
        }
    }

    public static String extractStudentInfo(PendingOperationEntity op) {
        if (op == null) return "Student Info";
        String name = "";
        String number = "";
        String correctedNumber = "";
        String group = "";

        if (op.payloadJson != null && !op.payloadJson.isEmpty()) {
            try {
                JSONObject json = new JSONObject(op.payloadJson);
                if (json.has("studentName")) name = json.optString("studentName", "");
                else if (json.has("student_name")) name = json.optString("student_name", "");
                else if (json.has("name")) name = json.optString("name", "");
                else if (json.has("firstName") || json.has("lastName")) {
                    String fn = json.optString("firstName", "");
                    String ln = json.optString("lastName", "");
                    name = (fn + " " + ln).trim();
                }

                if (json.has("studentNumber")) number = json.optString("studentNumber", "");
                else if (json.has("student_number")) number = json.optString("student_number", "");

                if (json.has("correctedStudentNumber")) {
                    correctedNumber = json.optString("correctedStudentNumber", "");
                }

                if (json.has("group_code")) group = json.optString("group_code", "");
                else if (json.has("labGroup")) group = json.optString("labGroup", "");
                else if (json.has("requestedGroup")) group = json.optString("requestedGroup", "");
            } catch (Exception ignored) {
            }
        }

        if (!correctedNumber.isEmpty()) {
            return "Corrected #: " + correctedNumber + (!name.isEmpty() ? " (" + name + ")" : "");
        }

        StringBuilder sb = new StringBuilder();
        if (!name.isEmpty()) {
            sb.append(name);
        }
        if (!number.isEmpty()) {
            if (sb.length() > 0) sb.append(" (").append(number).append(")");
            else sb.append("Student #: ").append(number);
        }
        if (!group.isEmpty()) {
            if (sb.length() > 0) sb.append(" • Group: ").append(group);
            else sb.append("Group: ").append(group);
        }

        if (sb.length() > 0) {
            return sb.toString();
        }

        if (op.studentLocalId != null) {
            return "Student Local ID: " + op.studentLocalId;
        } else if (op.entityId != null) {
            return "Student Server ID: " + op.entityId;
        }

        return "Student Info Pending";
    }

    public static String getStatusLabel(String status) {
        if (status == null) return "Pending";
        switch (status) {
            case PendingOperationEntity.STATUS_PENDING:
                return "Pending";
            case PendingOperationEntity.STATUS_SYNCING:
                return "Syncing...";
            case PendingOperationEntity.STATUS_SYNCED:
                return "Synced";
            case PendingOperationEntity.STATUS_FAILED:
                return "Failed";
            case PendingOperationEntity.STATUS_CONFLICT:
            case PendingOperationEntity.STATUS_REJECTED:
            case PendingOperationEntity.STATUS_ACTION_REQUIRED:
                return "Action Required";
            default:
                return status;
        }
    }

    public static int getStatusColor(Context context, String status) {
        if (status == null) return ContextCompat.getColor(context, R.color.dark_gray);
        switch (status) {
            case PendingOperationEntity.STATUS_SYNCING:
                return ContextCompat.getColor(context, R.color.dodger_blue);
            case PendingOperationEntity.STATUS_SYNCED:
                return 0xFF2E7D32; // Green
            case PendingOperationEntity.STATUS_FAILED:
            case PendingOperationEntity.STATUS_CONFLICT:
            case PendingOperationEntity.STATUS_REJECTED:
            case PendingOperationEntity.STATUS_ACTION_REQUIRED:
                return ContextCompat.getColor(context, R.color.error_red);
            case PendingOperationEntity.STATUS_PENDING:
            default:
                return ContextCompat.getColor(context, R.color.dark_gray);
        }
    }

    public static String getFormattedTimestamp(long createdAt) {
        if (createdAt <= 0) return "Just now";
        long now = System.currentTimeMillis();
        CharSequence relative = DateUtils.getRelativeTimeSpanString(
                createdAt, now, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE
        );
        return relative.toString();
    }
}
