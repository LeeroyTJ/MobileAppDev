package com.example.mobileappdev.sync;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import android.content.Context;

public class SyncWorker extends Worker {
    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
// Phase 5: read PendingOperation queue from Room,
// send via Retrofit, handle Success/Conflict/Error

        return Result.success();
    }
}






