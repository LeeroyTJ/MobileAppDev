package com.example.mobileappdev;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobileappdev.data.local.entity.PendingOperationEntity;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PendingOperationAdapter extends ListAdapter<PendingOperationEntity, PendingOperationAdapter.ViewHolder> {

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("MMM d, h:mm a", Locale.US);
    private final Gson gson = new Gson();

    public PendingOperationAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<PendingOperationEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull PendingOperationEntity oldItem, @NonNull PendingOperationEntity newItem) {
                    return TextUtils.equals(oldItem.operationId, newItem.operationId);
                }

                @Override
                public boolean areContentsTheSame(@NonNull PendingOperationEntity oldItem, @NonNull PendingOperationEntity newItem) {
                    return TextUtils.equals(oldItem.status, newItem.status) &&
                            oldItem.createdAt == newItem.createdAt &&
                            TextUtils.equals(oldItem.payloadJson, newItem.payloadJson);
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pending_operation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PendingOperationEntity item = getItem(position);
        if (item != null) {
            holder.bind(item, gson);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivOpIcon;
        private final TextView tvOpType;
        private final TextView tvOpTime;
        private final TextView tvOpDetails;
        private final TextView tvOpStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivOpIcon = itemView.findViewById(R.id.ivOpIcon);
            tvOpType = itemView.findViewById(R.id.tvOpType);
            tvOpTime = itemView.findViewById(R.id.tvOpTime);
            tvOpDetails = itemView.findViewById(R.id.tvOpDetails);
            tvOpStatus = itemView.findViewById(R.id.tvOpStatus);
        }

        public void bind(@NonNull PendingOperationEntity item, Gson gson) {
            Context context = itemView.getContext();

            // Operation Type Title
            String title = formatOpType(item.operationType);
            tvOpType.setText(title);

            // Operation Time
            if (item.createdAt > 0) {
                tvOpTime.setText(TIME_FORMAT.format(new Date(item.createdAt)));
            } else {
                tvOpTime.setText("");
            }

            // Extract payload details
            String details = parsePayloadDetails(item, gson);
            tvOpDetails.setText(details);

            // Status Pill
            String status = item.status != null ? item.status : "PENDING";
            tvOpStatus.setText(status);

            if ("SYNCED".equalsIgnoreCase(status)) {
                tvOpStatus.setTextColor(ContextCompat.getColor(context, R.color.green_complete));
                if (ivOpIcon != null) ivOpIcon.setImageResource(R.drawable.ic_check_circle);
            } else if ("FAILED".equalsIgnoreCase(status) || "REJECTED".equalsIgnoreCase(status) || "CONFLICT".equalsIgnoreCase(status)) {
                tvOpStatus.setTextColor(ContextCompat.getColor(context, R.color.error_red));
                if (ivOpIcon != null) ivOpIcon.setImageResource(R.drawable.ic_status_action_required);
            } else if ("SYNCING".equalsIgnoreCase(status)) {
                tvOpStatus.setTextColor(ContextCompat.getColor(context, R.color.dodger_blue));
                if (ivOpIcon != null) ivOpIcon.setImageResource(R.drawable.ic_refresh);
            } else {
                tvOpStatus.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
                if (ivOpIcon != null) ivOpIcon.setImageResource(R.drawable.ic_status_pending);
            }
        }

        private String formatOpType(String type) {
            if (type == null) return "Operation";
            switch (type) {
                case PendingOperationEntity.TYPE_CREATE_STUDENT:
                    return "Create Student";
                case PendingOperationEntity.TYPE_UPDATE_STUDENT:
                    return "Update Student";
                case PendingOperationEntity.TYPE_DELETE_STUDENT:
                    return "Delete Student";
                case PendingOperationEntity.TYPE_ASSIGN_GROUP:
                    return "Assign Lab Group";
                case PendingOperationEntity.TYPE_TRANSFER_GROUP:
                    return "Transfer Lab Group";
                case PendingOperationEntity.TYPE_REQUEST_GROUP_CHANGE:
                    return "Request Group Change";
                case PendingOperationEntity.TYPE_CORRECT_STUDENT_NUMBER:
                    return "Correct Student Number";
                default:
                    return type.replace("_", " ");
            }
        }

        private String parsePayloadDetails(PendingOperationEntity item, Gson gson) {
            if (TextUtils.isEmpty(item.payloadJson)) {
                return "Operation ID: " + item.operationId;
            }
            try {
                JsonObject obj = gson.fromJson(item.payloadJson, JsonObject.class);
                StringBuilder sb = new StringBuilder();

                if (obj.has("name")) {
                    sb.append(obj.get("name").getAsString());
                }
                if (obj.has("studentNumber")) {
                    if (sb.length() > 0) sb.append(" (");
                    sb.append(obj.get("studentNumber").getAsString());
                    if (sb.length() > 0 && sb.toString().contains("(")) sb.append(")");
                }
                if (obj.has("labGroup") && !obj.get("labGroup").isJsonNull()) {
                    if (sb.length() > 0) sb.append(" • ");
                    sb.append("Group: ").append(obj.get("labGroup").getAsString());
                }
                if (obj.has("requestedGroup") && !obj.get("requestedGroup").isJsonNull()) {
                    if (sb.length() > 0) sb.append(" • ");
                    sb.append("Requested: ").append(obj.get("requestedGroup").getAsString());
                }
                if (obj.has("correctedNumber") && !obj.get("correctedNumber").isJsonNull()) {
                    if (sb.length() > 0) sb.append(" • ");
                    sb.append("New No: ").append(obj.get("correctedNumber").getAsString());
                }

                if (sb.length() == 0) {
                    return item.payloadJson;
                }
                return sb.toString();
            } catch (Exception e) {
                return item.payloadJson;
            }
        }
    }
}
