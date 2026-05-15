package com.example.spottermobile.notifications;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.spottermobile.database.DatabaseHelper;

/**
 * WorkManager background job that automatically marks no-shows.
 *
 * WHY: DatabaseHelper.markNoShows() is well-written but was never called
 * automatically — it only ran when an admin happened to open the bookings screen.
 * This Worker runs every 30 minutes and calls markNoShowsForAllLockedSlots(),
 * which scans all BOOKED entries whose grace period has expired and marks them
 * as NO_SHOW. No admin action required.
 *
 * SCHEDULING: Register this in your Application class or MainActivity:
 *
 *   PeriodicWorkRequest noShowWork =
 *       new PeriodicWorkRequest.Builder(NoShowWorker.class, 30, TimeUnit.MINUTES)
 *           .build();
 *   WorkManager.getInstance(context)
 *       .enqueueUniquePeriodicWork("no_show_check",
 *           ExistingPeriodicWorkPolicy.KEEP, noShowWork);
 *
 * Add to build.gradle (app):
 *   implementation "androidx.work:work-runtime:2.9.0"
 */
public class NoShowWorker extends Worker {

    private static final String TAG = "NoShowWorker";

    public NoShowWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            DatabaseHelper db = new DatabaseHelper(getApplicationContext());
            int marked = db.markNoShowsForAllLockedSlots();
            Log.d(TAG, "NoShowWorker ran — marked " + marked + " no-shows.");
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "NoShowWorker failed", e);
            return Result.retry(); // WorkManager will retry with backoff
        }
    }
}
