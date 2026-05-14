package com.example.spottermobile.notifications;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class NotificationSender {

    /**
     * Sends a booking notification regardless of whether the app is in the foreground.
     * On Android 8+, routes through NotificationSchedulerService so it survives background kill.
     */
    public static void send(Context context, String type,
                            String date, String timeSlot, int position) {
        Intent intent = new Intent(context, NotificationSchedulerService.class);
        intent.putExtra(BookingNotificationReceiver.EXTRA_NOTIF_TYPE, type);
        intent.putExtra(BookingNotificationReceiver.EXTRA_DATE, date);
        intent.putExtra(BookingNotificationReceiver.EXTRA_TIME_SLOT, timeSlot);
        intent.putExtra(BookingNotificationReceiver.EXTRA_POSITION, position);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    // ── CONVENIENCE OVERLOADS ──────────────────────────────────────────────────

    public static void confirmed(Context context, String date, String timeSlot) {
        send(context, BookingNotificationReceiver.TYPE_CONFIRMED, date, timeSlot, 0);
    }

    public static void waitlisted(Context context, String date, String timeSlot, int position) {
        send(context, BookingNotificationReceiver.TYPE_WAITLISTED, date, timeSlot, position);
    }

    public static void promoted(Context context, String date, String timeSlot) {
        send(context, BookingNotificationReceiver.TYPE_PROMOTED, date, timeSlot, 0);
    }

    public static void checkedIn(Context context, String date, String timeSlot) {
        send(context, BookingNotificationReceiver.TYPE_CHECKIN, date, timeSlot, 0);
    }

    public static void cancelled(Context context, String date, String timeSlot) {
        send(context, BookingNotificationReceiver.TYPE_CANCELLED, date, timeSlot, 0);
    }
}