package com.example.mobileappdev.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.mobileappdev.data.local.AppDatabase;
import com.example.mobileappdev.data.local.entity.PendingOperationEntity;
import com.example.mobileappdev.data.remote.ApiClient;
import com.example.mobileappdev.data.remote.SyncApiModels.SyncOperation;
import com.example.mobileappdev.data.remote.SyncApiModels.SyncRequest;
import com.example.mobileappdev.data.remote.SyncApiModels.SyncResponseEnvelope;
import com.example.mobileappdev.data.remote.SyncApiModels.SyncResult;
import com.example.mobileappdev.data.repository.StudentSyncRepository;
import com.example.mobileappdev.session.SessionManager;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit2.Response;

/**
 * Background worker responsible for syncing locally pending changes
 * (offline creates/edits/deletes) with the remote server.
 */
public class SyncWorker extends Worker {

    public static final String UNIQUE_PERIODIC_NAME = "cohorthub_periodic_sync";
    public static final String UNIQUE_MANUAL_NAME = "cohorthub_manual_sync";

    private final Gson gson = new Gson();

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        long accountId = SessionManager.getInstance(getApplicationContext()).getAccountId();
        if (accountId <= 0) {
            return Result.success();
        }

        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        StudentSyncRepository repository = new StudentSyncRepository(db);

        List<PendingOperationEntity> queue = db.pendingOperationDao().getReplayQueue(accountId);
        if (queue.isEmpty()) {
            return Result.success();
        }

        List<SyncOperation> operations = new ArrayList<>();
        for (PendingOperationEntity op : queue) {
            db.pendingOperationDao().updateStatus(op.operationId, PendingOperationEntity.STATUS_SYNCING);

            SyncOperation dto = new SyncOperation();
            dto.operationId = op.operationId;
            dto.operationType = op.operationType;
            dto.entityType = "STUDENT";
            dto.entityId = op.entityId;
            dto.accountId = op.accountId;
            dto.baseVersion = op.baseVersion;
            java.lang.reflect.Type mapType = new com.google.gson.reflect.TypeToken<java.util.Map<String, Object>>() {}.getType();
            dto.payload = gson.fromJson(op.payloadJson, mapType);
            operations.add(dto);
        }

        SyncApiService api = ApiClient.getSyncService(getApplicationContext());
        try {
            Response<SyncResponseEnvelope> response = api.pushOperations(new SyncRequest(operations)).execute();
            if (!response.isSuccessful() || response.body() == null || response.body().data == null) {
                markAllFailed(db, queue);
                return Result.retry();
            }
            for (SyncResult result : response.body().data.results) {
                String resultJson = gson.toJson(result);
                repository.applySyncResult(
                        result.operationId,
                        result.status,
                        resultJson,
                        result.entityId,
                        result.version);
            }
            return Result.success();
        } catch (Exception e) {
            markAllFailed(db, queue);
            return Result.retry();
        }
    }

    private void markAllFailed(AppDatabase db, List<PendingOperationEntity> queue) {
        for (PendingOperationEntity op : queue) {
            db.pendingOperationDao().updateStatus(op.operationId, PendingOperationEntity.STATUS_FAILED);
        }
    }

    public static void schedulePeriodic(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                SyncWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request);
    }

    public static void triggerManualSync(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_MANUAL_NAME, ExistingWorkPolicy.KEEP, request);
    }
}
