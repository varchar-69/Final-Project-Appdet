package com.example.spottermobile.notifications;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.spottermobile.database.FirestoreHelper;   // CHANGED: was DatabaseHelper

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WorkManager Worker that auto-checks-out a booking at slot end.
 *
 * MIGRATION NOTES:
 * - Input data key "booking_id" is now a String (Firestore doc ID).
 *   Any caller building the WorkRequest must switch from:
 *       new Data.Builder().putInt("booking_id", intId)
 *   to:
 *       new Data.Builder().putString("booking_id", stringId)
 *
 * - Firestore calls are async but Worker.doWork() must return a Result synchronously.
 *   We bridge this with a CountDownLatch — doWork() blocks until the callback fires
 *   or a 10-second timeout elapses (after which WorkManager will retry).
 */
public class AutoCheckoutWorker extends Worker {

    private static final String TAG               = "AutoCheckoutWorker";
    private static final String STATUS_CHECKED_IN = "checked_in"; // CHANGED: was DatabaseHelper.STATUS_CHECKED_IN
    private static final long   TIMEOUT_MS        = 10_000L;

    public AutoCheckoutWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        // CHANGED: was getInputData().getInt("booking_id", -1) — now String Firestore doc ID
        String bookingId = getInputData().getString("booking_id");
        if (bookingId == null || bookingId.isEmpty()) return Result.failure();

        FirestoreHelper firestoreHelper = new FirestoreHelper();

        // Latch + result holder to bridge async → sync for WorkManager
        CountDownLatch latch        = new CountDownLatch(1);
        AtomicBoolean  shouldRetry  = new AtomicBoolean(false);
        AtomicBoolean  workFailed   = new AtomicBoolean(false);

        // Step 1 — fetch status
        // CHANGED: was synchronous db.getBookingStatus(int) — now async callback
        firestoreHelper.getBookingStatus(bookingId, new FirestoreHelper.FirestoreCallback<String>() {
            @Override
            public void onSuccess(String status) {
                if (!STATUS_CHECKED_IN.equals(status)) {
                    // Already checked out — nothing to do
                    latch.countDown();
                    return;
                }

                // Step 2 — check out
                // CHANGED: was synchronous db.checkOutBooking(int) — now async callback
                firestoreHelper.checkOutBooking(bookingId, new FirestoreHelper.FirestoreCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.d(TAG, "Auto-checkout done for booking " + bookingId);
                        latch.countDown();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "checkOutBooking failed: " + errorMessage);
                        shouldRetry.set(true);
                        latch.countDown();
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "getBookingStatus failed: " + errorMessage);
                shouldRetry.set(true);
                latch.countDown();
            }
        });

        // Block until async work completes or timeout
        try {
            latch.await(TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.retry();
        }

        if (workFailed.get()) return Result.failure();
        if (shouldRetry.get()) return Result.retry();
        return Result.success();
    }
}