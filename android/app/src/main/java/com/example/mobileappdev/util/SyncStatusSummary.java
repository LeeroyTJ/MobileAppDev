package com.example.mobileappdev.util;

import com.example.mobileappdev.R;
import com.example.mobileappdev.data.local.entity.PendingOperationEntity;

import java.util.List;

public final class SyncStatusSummary {
    public final int labelRes;
    public final int iconRes;

    private SyncStatusSummary(int labelRes, int iconRes) {
        this.labelRes = labelRes;
        this.iconRes = iconRes;
    }

    public static SyncStatusSummary from(List<PendingOperationEntity> queue) {
        boolean action = false, syncing = false, pending = false;
        if (queue != null) {
            for (PendingOperationEntity op : queue) {
                switch (op.status) {
                    case PendingOperationEntity.STATUS_CONFLICT:
                    case PendingOperationEntity.STATUS_REJECTED:
                        action = true; break;
                    case PendingOperationEntity.STATUS_SYNCING:
                        syncing = true; break;
                    case PendingOperationEntity.STATUS_PENDING:
                    case PendingOperationEntity.STATUS_FAILED:
                        pending = true; break;
                    default:
                        break;   // SYNCED rows are history, they don't count
                }
            }
        }
        if (action)  return new SyncStatusSummary(R.string.sync_action_required, R.drawable.ic_status_error);
        if (syncing) return new SyncStatusSummary(R.string.sync_syncing, R.drawable.ic_status_pending);
        if (pending) return new SyncStatusSummary(R.string.sync_pending, R.drawable.ic_status_pending);
        return new SyncStatusSummary(R.string.sync_status_synced, R.drawable.ic_check_circle);
    }
}
