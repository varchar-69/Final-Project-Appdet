package com.example.spottermobile.notifications;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.spottermobile.database.FirestoreHelper;  // CHANGED: was DatabaseHelper

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WorkManager background job that automatically marks no-shows.
 *
 * WHY: markNoShowsForAllLockedSlots() was never called automatically —
 * it only ran when an admin opened the bookings screen.
 * This Worker runs every 30 minutes, queries all "booked" entries whose
 * grace period has expired, and batch-updates them to "no_show".
 * No admin action required.
 *
 * CHANGED: was synchronous DatabaseHelper.markNoShowsForAllLockedSlots().
 * Now async via FirestoreHelper — bridged to WorkManager's synchronous
 * Result using CountDownLatch (same pattern as AutoCheckoutWorker).
 *
 * SCHEDULING: Register in your Application class or MainActivity:
 *
 *   PeriodicWorkRequest noShowWork =
 *       new PeriodicWorkRequest.Builder(NoShowWorker.class, 30, TimeUnit.MINUTES)
 *           .build();
 *   WorkManager.getInstance(context)
 *       .enqueueUniquePeriodicWork("no_show_check",
 *           ExistingPeriodicWorkPolicy.KEEP, noShowWork);
 */
public class NoShowWorker extends Worker {

    private static final String TAG        = "NoShowWorker";
    private static final long   TIMEOUT_MS = 30_000L; // 30s — batch writes can be slow

    public NoShowWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        FirestoreHelper firestoreHelper = new FirestoreHelper();

        // Bridge async Firestore → sync WorkManager result
        CountDownLatch  latch       = new CountDownLatch(1);
        AtomicBoolean   shouldRetry = new AtomicBoolean(false);
        AtomicInteger   marked      = new AtomicInteger(0);

        // CHANGED: was synchronous db.markNoShowsForAllLockedSlots()
        firestoreHelper.markNoShowsForAllLockedSlots(
                new FirestoreHelper.FirestoreCallback<Integer>() {
                    @Override
                    public void onSuccess(Integer count) {
                        marked.set(count);
                        latch.countDown();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "markNoShowsForAllLockedSlots failed: " + errorMessage);
                        shouldRetry.set(true);
                        latch.countDown();
                    }
                });

        // Block until callback fires or timeout
        try {
            latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.retry();
        }

        if (shouldRetry.get()) return Result.retry();

        Log.d(TAG, "NoShowWorker ran — marked " + marked.get() + " no-shows.");
        return Result.success();
    }
}