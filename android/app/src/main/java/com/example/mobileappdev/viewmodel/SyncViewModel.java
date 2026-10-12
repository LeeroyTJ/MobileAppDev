package com.example.mobileappdev.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.mobileappdev.data.local.AppDatabase;
import com.example.mobileappdev.data.local.entity.PendingOperationEntity;
import com.example.mobileappdev.session.SessionManager;

import java.util.List;

public class SyncViewModel extends AndroidViewModel {

    private final MutableLiveData<Boolean> isSyncing = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> syncSuccessful = new MutableLiveData<>(false);
    private final MutableLiveData<String> syncMessage = new MutableLiveData<>("");

    private final LiveData<List<PendingOperationEntity>> pendingQueueLiveData;

    public SyncViewModel(@NonNull Application application) {
        super(application);
        long accountId = SessionManager.getInstance(application).getAccountId();
        AppDatabase db = AppDatabase.getInstance(application);
        if (accountId > 0) {
            pendingQueueLiveData = db.pendingOperationDao().observeQueueForAccount(accountId);
        } else {
            pendingQueueLiveData = new MutableLiveData<>(null);
        }
    }

    public void setSyncing(boolean syncing) {
        isSyncing.setValue(syncing);
    }

    public void setSyncSuccessful(boolean successful) {
        syncSuccessful.setValue(successful);
    }

    public void setSyncMessage(String message) {
        syncMessage.setValue(message);
    }

    public LiveData<Boolean> getIsSyncing() {
        return isSyncing;
    }

    public LiveData<Boolean> getSyncSuccessful() {
        return syncSuccessful;
    }

    public LiveData<String> getSyncMessage() {
        return syncMessage;
    }

    public LiveData<List<PendingOperationEntity>> getPendingQueueLiveData() {
        return pendingQueueLiveData;
    }

    /**
     * Exposes human-readable sync status for UI badges:
     * "Saved locally", "Pending", "Syncing", "Synced", "Action required"
     */
    public LiveData<String> getSyncStatusSummary() {
        return Transformations.map(pendingQueueLiveData, queue -> {
            if (queue == null || queue.isEmpty()) {
                return "Synced";
            }
            boolean hasSyncing = false;
            boolean hasFailedOrConflict = false;
            for (PendingOperationEntity op : queue) {
                if (PendingOperationEntity.STATUS_SYNCING.equals(op.status)) {
                    hasSyncing = true;
                }
                if (PendingOperationEntity.STATUS_CONFLICT.equals(op.status) ||
                        PendingOperationEntity.STATUS_REJECTED.equals(op.status) ||
                        PendingOperationEntity.STATUS_FAILED.equals(op.status)) {
                    hasFailedOrConflict = true;
                }
            }
            if (hasFailedOrConflict) {
                return "Action required";
            }
            if (hasSyncing) {
                return "Syncing";
            }
            return "Pending";
        });
    }
}
