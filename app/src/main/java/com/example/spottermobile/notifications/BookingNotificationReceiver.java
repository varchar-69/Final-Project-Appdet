package com.example.spottermobile.notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.spottermobile.database.FirestoreHelper;  // CHANGED: was DatabaseHelper
import com.example.spottermobile.model.Booking;
import com.example.spottermobile.utils.SlotUtils;           // CHANGED: replaces DatabaseHelper static helpers

import java.util.Calendar;
import java.util.List;

public class BookingNotificationReceiver extends BroadcastReceiver {

    // Intent extras — used when scheduling delayed notifications
    public static final String EXTRA_NOTIF_TYPE = "notif_type";
    public static final String EXTRA_DATE       = "date";
    public static final String EXTRA_TIME_SLOT  = "time_slot";
    public static final String EXTRA_POSITION   = "position";

    // Notification type values
    public static final String TYPE_CONFIRMED  = "confirmed";
    public static final String TYPE_WAITLISTED = "waitlisted";
    public static final String TYPE_PROMOTED   = "promoted";
    public static final String TYPE_CHECKIN    = "checkin";
    public static final String TYPE_CANCELLED  = "cancelled";

    private static final String TAG = "BookingNotifReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        if (action == null) return;

        // On device reboot: recreate notification channel AND reschedule any pending
        // auto-checkout alarms for bookings still in checked_in state.
        // Without this, AlarmManager alarms are wiped on reboot and members would
        // stay checked-in forever if they don't manually scan out.
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            NotificationHelper.createNotificationChannel(context);
            rescheduleAutoCheckouts(context);
            return;
        }

        // Scheduled notification fired
        if ("com.example.spottermobile.BOOKING_NOTIFICATION".equals(action)) {
            String type     = intent.getStringExtra(EXTRA_NOTIF_TYPE);
            String date     = intent.getStringExtra(EXTRA_DATE);
            String timeSlot = intent.getStringExtra(EXTRA_TIME_SLOT);
            int    position = intent.getIntExtra(EXTRA_POSITION, 1);

            if (type == null || date == null || timeSlot == null) return;

            switch (type) {
                case TYPE_CONFIRMED:
                    NotificationHelper.notifyBookingConfirmed(context, date, timeSlot);
                    break;
                case TYPE_WAITLISTED:
                    NotificationHelper.notifyWaitlisted(context, date, timeSlot, position);
                    break;
                case TYPE_PROMOTED:
                    NotificationHelper.notifyPromoted(context, date, timeSlot);
                    break;
                case TYPE_CHECKIN:
                    NotificationHelper.notifyCheckedIn(context, date, timeSlot);
                    break;
                case TYPE_CANCELLED:
                    NotificationHelper.notifyCancelled(context, date, timeSlot);
                    break;
            }
        }
    }

    /**
     * Called on BOOT_COMPLETED. Re-schedules AlarmManager alarms for any bookings
     * that are still checked_in — because AlarmManager alarms don't survive reboots.
     *
     * CHANGED: was synchronous new DatabaseHelper(context).getBookingsByStatus(STATUS_CHECKED_IN).
     * Now async via FirestoreHelper.getAllBookingsByStatus(). goAsync() keeps the
     * receiver process alive while the Firestore call completes.
     */
    private void rescheduleAutoCheckouts(Context context) {
        // CHANGED: goAsync() — without this the process can be killed before
        // the Firestore callback fires, since onReceive() returns immediately.
        final PendingResult pendingResult = goAsync();

        FirestoreHelper firestoreHelper = new FirestoreHelper();

        // CHANGED: was synchronous db.getBookingsByStatus(STATUS_CHECKED_IN)
        // Now queries all bookings with status "checked_in" across all users.
        firestoreHelper.getAllBookingsByStatus("checked_in",
                new FirestoreHelper.FirestoreCallback<List<Booking>>() {
                    @Override
                    public void onSuccess(List<Booking> checkedIn) {
                        AlarmManager am = (AlarmManager)
                                context.getSystemService(Context.ALARM_SERVICE);
                        if (am == null) {
                            pendingResult.finish();
                            return;
                        }

                        for (Booking booking : checkedIn) {
                            scheduleAlarm(context, am, booking);
                        }

                        pendingResult.finish();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "Could not fetch checked-in bookings on boot: " + errorMessage);
                        pendingResult.finish();
                    }
                });
    }

    /**
     * Schedules (or immediately fires) the auto-checkout alarm for one booking.
     *
     * CHANGED: booking.getId() was an int used directly as the PendingIntent request code.
     * Firestore IDs are Strings, so we use booking.getFirestoreId().hashCode() as a
     * stable int request code — hashCode() on the same String always returns the same int,
     * so FLAG_UPDATE_CURRENT will correctly replace any existing alarm for that booking.
     *
     * CHANGED: intent.putExtra now passes the String Firestore ID (not an int),
     * matching the updated AutoCheckoutReceiver.EXTRA_BOOKING_ID contract.
     *
     * CHANGED: DatabaseHelper.getSlotEndCalendar() → SlotUtils.getSlotEndCalendar()
     * (pure time helper, no database access).
     */
    private void scheduleAlarm(Context context, AlarmManager am, Booking booking) {
        String bookingId = booking.getFirestoreId();
        if (bookingId == null || bookingId.isEmpty()) return;

        // CHANGED: SlotUtils replaces DatabaseHelper.getSlotEndCalendar()
        Calendar endCal = SlotUtils.getSlotEndCalendar(
                booking.getSelectedDate(), booking.getTimeSlot());
        if (endCal == null) return;

        // If slot end is already past, fire in 30 seconds
        if (endCal.getTimeInMillis() < System.currentTimeMillis()) {
            endCal = Calendar.getInstance();
            endCal.add(Calendar.SECOND, 30);
        }

        Intent intent = new Intent(context, AutoCheckoutReceiver.class);
        intent.setAction(AutoCheckoutReceiver.ACTION_AUTO_CHECKOUT);
        // CHANGED: String Firestore ID (was int booking.getId())
        intent.putExtra(AutoCheckoutReceiver.EXTRA_BOOKING_ID, bookingId);

        // CHANGED: hashCode() gives a stable int request code from the String Firestore ID.
        // Same bookingId → same hashCode → FLAG_UPDATE_CURRENT replaces the old alarm.
        int requestCode = bookingId.hashCode();
        PendingIntent pi = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                    endCal.getTimeInMillis(), pi);
        } catch (SecurityException e) {
            am.set(AlarmManager.RTC_WAKEUP, endCal.getTimeInMillis(), pi);
        }

        Log.d(TAG, "Rescheduled auto-checkout for booking " + bookingId);
    }
}