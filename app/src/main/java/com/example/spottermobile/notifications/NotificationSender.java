package com.example.spottermobile.notifications;

import android.content.Context;

public class NotificationSender {

    public static void confirmed(Context context, String date, String timeSlot) {
        NotificationHelper.notifyBookingConfirmed(context, date, timeSlot);
    }

    public static void waitlisted(Context context, String date, String timeSlot, int position) {
        NotificationHelper.notifyWaitlisted(context, date, timeSlot, position);
    }

    public static void promoted(Context context, String date, String timeSlot) {
        NotificationHelper.notifyPromoted(context, date, timeSlot);
    }

    public static void checkedIn(Context context, String date, String timeSlot) {
        NotificationHelper.notifyCheckedIn(context, date, timeSlot);
    }

    public static void cancelled(Context context, String date, String timeSlot) {
        NotificationHelper.notifyCancelled(context, date, timeSlot);
    }
}