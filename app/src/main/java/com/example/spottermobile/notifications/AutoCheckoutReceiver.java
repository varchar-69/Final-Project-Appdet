package com.example.spottermobile.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.spottermobile.database.DatabaseHelper;

/**
 * BroadcastReceiver fired by AlarmManager at the end of a booking slot.
 * If the booking is still CHECKED_IN (member forgot to scan out), it
 * automatically checks them out so occupancy counts stay accurate and
 * their workout history is still recorded.
 *
 * Scheduled in QRScanActivity.scheduleAutoCheckout() on every successful check-in.
 */
public class AutoCheckoutReceiver extends BroadcastReceiver {

    public static final String EXTRA_BOOKING_ID = "booking_id";
    public static final String ACTION_AUTO_CHECKOUT =
            "com.example.spottermobile.AUTO_CHECKOUT";

    private static final String TAG = "AutoCheckoutReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_AUTO_CHECKOUT.equals(intent.getAction())) return;

        int bookingId = intent.getIntExtra(EXTRA_BOOKING_ID, -1);
        if (bookingId == -1) return;

        DatabaseHelper db = new DatabaseHelper(context);

        // Only checkout if still CHECKED_IN — if they already scanned out, skip silently
        if (!DatabaseHelper.STATUS_CHECKED_IN.equals(db.getBookingStatus(bookingId))) {
            Log.d(TAG, "Booking #" + bookingId + " already resolved — skipping auto-checkout.");
            return;
        }

        boolean success = db.checkOutBooking(bookingId);
        Log.d(TAG, "Auto-checkout booking #" + bookingId + " → " + (success ? "done" : "failed"));
    }
}
