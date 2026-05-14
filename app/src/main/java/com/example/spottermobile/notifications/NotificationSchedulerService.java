package com.example.spottermobile.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.example.spottermobile.R;

public class NotificationSchedulerService extends Service {

    private static final String SERVICE_CHANNEL_ID = "spotter_service";
    private static final int    SERVICE_NOTIF_ID   = 9999;

    public static final String EXTRA_NOTIF_TYPE = BookingNotificationReceiver.EXTRA_NOTIF_TYPE;
    public static final String EXTRA_DATE       = BookingNotificationReceiver.EXTRA_DATE;
    public static final String EXTRA_TIME_SLOT  = BookingNotificationReceiver.EXTRA_TIME_SLOT;
    public static final String EXTRA_POSITION   = BookingNotificationReceiver.EXTRA_POSITION;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Post a silent foreground notification so Android lets us run in the background
        startForeground(SERVICE_NOTIF_ID, buildServiceNotification());

        if (intent != null) {
            String type     = intent.getStringExtra(EXTRA_NOTIF_TYPE);
            String date     = intent.getStringExtra(EXTRA_DATE);
            String timeSlot = intent.getStringExtra(EXTRA_TIME_SLOT);
            int    position = intent.getIntExtra(EXTRA_POSITION, 1);

            if (type != null && date != null && timeSlot != null) {
                switch (type) {
                    case BookingNotificationReceiver.TYPE_CONFIRMED:
                        NotificationHelper.notifyBookingConfirmed(this, date, timeSlot);
                        break;
                    case BookingNotificationReceiver.TYPE_WAITLISTED:
                        NotificationHelper.notifyWaitlisted(this, date, timeSlot, position);
                        break;
                    case BookingNotificationReceiver.TYPE_PROMOTED:
                        NotificationHelper.notifyPromoted(this, date, timeSlot);
                        break;
                    case BookingNotificationReceiver.TYPE_CHECKIN:
                        NotificationHelper.notifyCheckedIn(this, date, timeSlot);
                        break;
                    case BookingNotificationReceiver.TYPE_CANCELLED:
                        NotificationHelper.notifyCancelled(this, date, timeSlot);
                        break;
                }
            }
        }

        stopSelf(); // done — no need to stay alive
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    private Notification buildServiceNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    SERVICE_CHANNEL_ID, "Spotter Service", NotificationManager.IMPORTANCE_MIN);
            channel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
        return new NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
                .setContentTitle("Spotter")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setSilent(true)
                .build();
    }
}