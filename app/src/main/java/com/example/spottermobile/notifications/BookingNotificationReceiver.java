package com.example.spottermobile.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

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

        // On device reboot: recreate the notification channel so future notifications work
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            NotificationHelper.createNotificationChannel(context);
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
}