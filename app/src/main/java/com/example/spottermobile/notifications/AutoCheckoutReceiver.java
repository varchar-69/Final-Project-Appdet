package com.example.spottermobile.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.spottermobile.database.FirestoreHelper;   // CHANGED: was DatabaseHelper

/**
 * BroadcastReceiver fired by AlarmManager at the end of a booking slot.
 * If the booking is still checked_in (member forgot to scan out), it
 * automatically checks them out so occupancy counts stay accurate and
 * their workout history is still recorded.
 *
 * Scheduled in QRScanActivity.scheduleAutoCheckout() on every successful check-in.
 *
 * MIGRATION NOTE:
 * - EXTRA_BOOKING_ID is now a String (Firestore doc ID), not an int.
 *   Any caller that previously did putExtra(EXTRA_BOOKING_ID, intId) must be
 *   updated to putExtra(EXTRA_BOOKING_ID, stringId).
 * - getBookingStatus() + checkOutBooking() are now async — we use goAsync() to
 *   keep the receiver alive past onReceive() while the Firestore calls complete.
 */
public class AutoCheckoutReceiver extends BroadcastReceiver {

    public static final String EXTRA_BOOKING_ID   = "booking_id";   // now String
    public static final String ACTION_AUTO_CHECKOUT =
            "com.example.spottermobile.AUTO_CHECKOUT";

    private static final String TAG              = "AutoCheckoutReceiver";
    private static final String STATUS_CHECKED_IN = "checked_in";   // CHANGED: was DatabaseHelper.STATUS_CHECKED_IN

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_AUTO_CHECKOUT.equals(intent.getAction())) return;

        // CHANGED: was getIntExtra(EXTRA_BOOKING_ID, -1) — now String Firestore doc ID
        String bookingId = intent.getStringExtra(EXTRA_BOOKING_ID);
        if (bookingId == null || bookingId.isEmpty()) return;

        // CHANGED: goAsync() keeps the receiver process alive while async Firestore
        // calls complete. Without this the process could be killed before callbacks fire.
        final PendingResult pendingResult = goAsync();

        FirestoreHelper firestoreHelper = new FirestoreHelper();

        // Step 1 — fetch current status; only proceed if still checked_in
        // CHANGED: was synchronous db.getBookingStatus(int) — now async callback
        firestoreHelper.getBookingStatus(bookingId, new FirestoreHelper.FirestoreCallback<String>() {
            @Override
            public void onSuccess(String status) {
                if (!STATUS_CHECKED_IN.equals(status)) {
                    Log.d(TAG, "Booking " + bookingId + " already resolved — skipping auto-checkout.");
                    pendingResult.finish();
                    return;
                }

                // Step 2 — check out the booking
                // CHANGED: was synchronous db.checkOutBooking(int) — now async callback
                firestoreHelper.checkOutBooking(bookingId, new FirestoreHelper.FirestoreCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.d(TAG, "Auto-checkout booking " + bookingId + " → done");
                        pendingResult.finish();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "Auto-checkout booking " + bookingId + " → failed: " + errorMessage);
                        pendingResult.finish();
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Could not fetch status for booking " + bookingId + ": " + errorMessage);
                pendingResult.finish();
            }
        });
    }
}