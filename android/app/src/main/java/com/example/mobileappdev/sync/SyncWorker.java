package com.example.mobileappdev.sync;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import android.content.Context;

import java.util.concurrent.TimeUnit;

/**
 •	Background worker responsible for syncing locally pending changes
 •	(e.g. offline actions) with the remote server.
 •	Currently a skeleton: doWork() is a

 placeholder until Room entities
 •	(Member 7) and the Pending Operation Queue (Member 8) are ready.
 •	Not yet wired up to run — no enqueueing code exists yet.
 */
public class SyncWorker extends Worker {

    // Required constructor signature for any Worker subclass.
// WorkManager calls this automatically when it schedules the job;
// we never construct SyncWorker ourselves.
    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

// The actual background task, run by WorkManager on a background thread

    // when constraints (e.g. network) are satisfied.
    @NonNull
    @Override
    public Result doWork() {
// Phase 5: read PendingOperation queue from Room,
// send via Retrofit, handle Success/Conflict/Error

// Placeholder result for now — always reports success
// so the skeleton compiles and can be tested/enqueued.
        return Result.success();
    }

    /**
     * Builds a one-time WorkRequest for this Worker, with:
     * - a network constraint (only runs when

     connected)
     * - exponential backoff (if the job fails and retries)
     *
     * Other classes (e.g. a repository or a "Sync" button handler)
     * will call SyncWorker.buildRequest() and pass the result to
     * WorkManager.enqueue(...) to actually schedule this job.
     */
    public static WorkRequest buildRequest() {
// Only allow this job to run when the device has network access.
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

// Wrap SyncWorker as a single (non-repeating) unit of work,
// attach the constraint above, and set a retry/backoff policy.
        return new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL, // delay grows exponentially between retries
                        WorkRequest.MIN_BACKOFF_MILLIS, // shortest allowed initial delay
                        TimeUnit.MILLISECONDS)
                .build();
    }
}






