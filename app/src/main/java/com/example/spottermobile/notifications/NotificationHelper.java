package com.example.spottermobile.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.spottermobile.R;
import com.example.spottermobile.activities.SplashActivity;

public class NotificationHelper {

    public static final String CHANNEL_ID   = "spotter_bookings";
    public static final String CHANNEL_NAME = "Booking Notifications";

    // Notification IDs — each type gets a stable ID so it can be updated/replaced
    public static final int NOTIF_CONFIRMED  = 1001;
    public static final int NOTIF_WAITLISTED = 1002;
    public static final int NOTIF_PROMOTED   = 1003;
    public static final int NOTIF_CHECKIN    = 1004;
    public static final int NOTIF_CANCELLED  = 1005;

    // ── CHANNEL SETUP ──────────────────────────────────────────────────────────

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Spotter gym booking updates");
            channel.enableVibration(true);
            NotificationManager manager =
                    context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    // ── SEND ───────────────────────────────────────────────────────────────────

    public static void sendNotification(Context context,
                                        int notifId,
                                        String title,
                                        String message) {
        createNotificationChannel(context);

        // Tapping the notification opens MainActivity
        Intent intent = new Intent(context, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, notifId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)   // add a 24dp white icon to drawable
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(notifId, builder.build());
    }

    // ── TYPED SENDERS ─────────────────────────────────────────────────────────
    // Call these directly from DatabaseHelper or any Activity after each operation.

    public static void notifyBookingConfirmed(Context context,
                                              String date, String timeSlot) {
        sendNotification(context,
                NOTIF_CONFIRMED,
                "✅ Booking Confirmed!",
                "Your gym session on " + date + " at " + timeSlot + " is confirmed. See you there!");
    }

    public static void notifyWaitlisted(Context context,
                                        String date, String timeSlot, int position) {
        sendNotification(context,
                NOTIF_WAITLISTED,
                "⏳ Added to Waitlist",
                "The " + timeSlot + " slot on " + date + " is full. "
                        + "You are #" + position + " on the waitlist.");
    }

    public static void notifyPromoted(Context context,
                                      String date, String timeSlot) {
        sendNotification(context,
                NOTIF_PROMOTED,
                "🎉 You're In!",
                "A spot opened up! You're confirmed for " + date
                        + " at " + timeSlot + ". Check in on time or your spot may be forfeited.");
    }

    public static void notifyCheckedIn(Context context,
                                       String date, String timeSlot) {
        sendNotification(context,
                NOTIF_CHECKIN,
                "💪 Checked In!",
                "You've checked in for your " + timeSlot + " session on " + date + ". Enjoy your workout!");
    }

    public static void notifyCancelled(Context context,
                                       String date, String timeSlot) {
        sendNotification(context,
                NOTIF_CANCELLED,
                "❌ Booking Cancelled",
                "Your booking for " + date + " at " + timeSlot + " has been cancelled.");
    }
}