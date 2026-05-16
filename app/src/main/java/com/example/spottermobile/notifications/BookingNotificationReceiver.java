package com.example.spottermobile.notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.example.spottermobile.database.DatabaseHelper;
import com.example.spottermobile.model.Booking;
import java.util.Calendar;
import java.util.List;

public class BookingNotificationReceiver extends BroadcastReceiver {

    // Intent extras — used when scheduling delayed notifications (e.g. grace period expiry)
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

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        if (action == null) return;

        // On device reboot: recreate notification channel AND reschedule any pending
        // auto-checkout alarms for bookings still in CHECKED_IN state.
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
     * that are still CHECKED_IN — because AlarmManager alarms don't survive reboots.
     * Without this, a reboot while someone is checked in would leave them stuck forever.
     */
    private void rescheduleAutoCheckouts(Context context) {
        DatabaseHelper db = new DatabaseHelper(context);
        List<Booking> checkedIn = db.getBookingsByStatus(DatabaseHelper.STATUS_CHECKED_IN);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        for (Booking booking : checkedIn) {
            Calendar endCal = DatabaseHelper.getSlotEndCalendar(
                    booking.getSelectedDate(), booking.getTimeSlot());
            if (endCal == null) continue;

            // If slot end is already past, fire in 30 seconds
            if (endCal.getTimeInMillis() < System.currentTimeMillis()) {
                endCal = Calendar.getInstance();
                endCal.add(Calendar.SECOND, 30);
            }

            Intent intent = new Intent(context, AutoCheckoutReceiver.class);
            intent.setAction(AutoCheckoutReceiver.ACTION_AUTO_CHECKOUT);
            intent.putExtra(AutoCheckoutReceiver.EXTRA_BOOKING_ID, booking.getId());

            PendingIntent pi = PendingIntent.getBroadcast(context, booking.getId(), intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            try {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                        endCal.getTimeInMillis(), pi);
            } catch (SecurityException e) {
                am.set(AlarmManager.RTC_WAKEUP, endCal.getTimeInMillis(), pi);
            }

            Log.d("BookingNotifReceiver", "Rescheduled auto-checkout for booking #" + booking.getId());
        }
    }

}