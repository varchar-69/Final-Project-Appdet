package com.example.spottermobile.activities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.spottermobile.database.DatabaseHelper;
import com.example.spottermobile.model.Booking;
import com.example.spottermobile.notifications.AutoCheckoutReceiver;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.Calendar;

public class QRScanActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;

    // Action constants — passed via Intent extras from AdminBookingsActivity
    public static final String EXTRA_SCAN_MODE = "scan_mode";
    public static final String MODE_CHECKIN    = "checkin";
    public static final String MODE_CHECKOUT   = "checkout";

    private String scanMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper = new DatabaseHelper(this);
        scanMode = getIntent().getStringExtra(EXTRA_SCAN_MODE);
        if (scanMode == null) scanMode = MODE_CHECKIN;

        // Launch ZXing scanner immediately — no layout needed
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt(MODE_CHECKIN.equals(scanMode)
                ? "Scan member QR code to CHECK IN"
                : "Scan member QR code to CHECK OUT");
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(true);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Let ZXing handle its own result first
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null) {
            if (result.getContents() == null) {
                // User pressed back / cancelled scan
                Toast.makeText(this, "Scan cancelled.", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                handleScanResult(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    // ── QR PARSING ─────────────────────────────────────────────────────────────

    /**
     * QR content format (from BookingActivity):
     *   SPOTTER GYM
     *   Booking #<id>
     *   User ID: <userId>
     *   Workout: <type>
     *   Date: <date>
     *   Time: <time>
     *   Status: CONFIRMED
     *
     * We parse line 2 ("Booking #12") to extract the booking ID.
     */
    private void handleScanResult(String qrContent) {
        int bookingId = parseBookingId(qrContent);

        if (bookingId == -1) {
            showResultDialog("Invalid QR",
                    "This QR code is not a valid Spotter Gym booking.", false);
            return;
        }

        Booking booking = dbHelper.getBookingById(bookingId);

        if (booking == null) {
            showResultDialog("Not Found",
                    "No booking found for ID #" + bookingId + ".", false);
            return;
        }

        if (MODE_CHECKIN.equals(scanMode)) {
            processCheckIn(booking);
        } else {
            processCheckOut(booking);
        }
    }

    /**
     * Verifies the HMAC token in the QR and returns the booking ID if authentic.
     * Returns -1 if the QR is forged, malformed, or not from this app.
     * Uses QrTokenUtils — the same secret key that was used to generate the QR.
     */
    private int parseBookingId(String qrContent) {
        return com.example.spottermobile.utils.QrTokenUtils.verifyAndExtractBookingId(qrContent);
    }

    // ── CHECK-IN ───────────────────────────────────────────────────────────────

    private void processCheckIn(Booking booking) {
        String currentStatus = booking.getStatus();

        if (DatabaseHelper.STATUS_CHECKED_IN.equals(currentStatus)) {
            showResultDialog("Already Checked In",
                    "Booking #" + booking.getId() + " is already checked in at "
                            + booking.getCheckinTime() + ".", false);
            return;
        }

        if (DatabaseHelper.STATUS_COMPLETED.equals(currentStatus)) {
            showResultDialog("Session Completed",
                    "This booking has already been completed (checked out).", false);
            return;
        }

        if (DatabaseHelper.STATUS_CANCELLED.equals(currentStatus)) {
            showResultDialog("Booking Cancelled",
                    "This booking was cancelled and cannot be used for check-in.", false);
            return;
        }

        if (!DatabaseHelper.STATUS_BOOKED.equals(currentStatus)) {
            showResultDialog("Not Valid",
                    "Booking #" + booking.getId() + " has status: " + currentStatus
                            + ". Only confirmed bookings can check in.", false);
            return;
        }

        boolean success = dbHelper.checkInBooking(booking.getId());
        if (success) {
            // Schedule auto-checkout at slot end time in case member forgets to scan out
            scheduleAutoCheckout(booking.getId(), booking.getSelectedDate(), booking.getTimeSlot());

            showResultDialog("✅ Checked In!",
                    "Welcome!\n\n"
                            + "Member ID: " + booking.getUserId() + "\n"
                            + "Workout: " + booking.getWorkoutType() + "\n"
                            + "Slot: " + booking.getTimeSlot() + "\n"
                            + "Date: " + booking.getSelectedDate(),
                    true);
        } else {
            showResultDialog("Check-In Failed",
                    "Could not check in booking #" + booking.getId()
                            + ". Please try again.", false);
        }
    }

    // ── CHECK-OUT ──────────────────────────────────────────────────────────────

    private void processCheckOut(Booking booking) {
        String currentStatus = booking.getStatus();

        if (!DatabaseHelper.STATUS_CHECKED_IN.equals(currentStatus)) {
            String msg = DatabaseHelper.STATUS_COMPLETED.equals(currentStatus)
                    ? "This member has already checked out."
                    : "This booking is not currently checked in. Status: " + currentStatus;
            showResultDialog("Cannot Check Out", msg, false);
            return;
        }

        boolean success = dbHelper.checkOutBooking(booking.getId());
        if (success) {
            showResultDialog("🏁 Checked Out!",
                    "Session complete!\n\n"
                            + "Member ID: " + booking.getUserId() + "\n"
                            + "Workout: " + booking.getWorkoutType() + "\n"
                            + "Checked in at: " + booking.getCheckinTime(),
                    true);
        } else {
            showResultDialog("Check-Out Failed",
                    "Could not check out booking #" + booking.getId()
                            + ". Please try again.", false);
        }
    }

    // ── RESULT DIALOG ──────────────────────────────────────────────────────────

    private void showResultDialog(String title, String message, boolean success) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setIcon(success
                        ? android.R.drawable.ic_dialog_info
                        : android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Scan Another", (dialog, which) -> {
                    // Re-launch scanner for next member
                    IntentIntegrator integrator = new IntentIntegrator(this);
                    integrator.setPrompt(MODE_CHECKIN.equals(scanMode)
                            ? "Scan member QR code to CHECK IN"
                            : "Scan member QR code to CHECK OUT");
                    integrator.setBeepEnabled(true);
                    integrator.setOrientationLocked(true);
                    integrator.initiateScan();
                })
                .setNegativeButton("Done", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }
    // ── AUTO CHECKOUT ──────────────────────────────────────────────────────────

    /**
     * Schedules an AlarmManager alarm to fire at slot end time.
     * AutoCheckoutReceiver will check if the booking is still CHECKED_IN
     * and call checkOutBooking() automatically if so.
     */
    private void scheduleAutoCheckout(int bookingId, String date, String timeSlot) {
        Calendar endCal = DatabaseHelper.getSlotEndCalendar(date, timeSlot);
        if (endCal == null) return; // couldn't parse slot — skip silently

        // If slot end is already in the past, fire immediately (1 second from now)
        if (endCal.getTimeInMillis() < System.currentTimeMillis()) {
            endCal = Calendar.getInstance();
            endCal.add(Calendar.SECOND, 1);
        }

        Intent intent = new Intent(this, AutoCheckoutReceiver.class);
        intent.setAction(AutoCheckoutReceiver.ACTION_AUTO_CHECKOUT);
        intent.putExtra(AutoCheckoutReceiver.EXTRA_BOOKING_ID, bookingId);

        // Use bookingId as requestCode so each booking gets its own alarm
        PendingIntent pi = PendingIntent.getBroadcast(this, bookingId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            try {
                // canScheduleExactAlarms() requires API 31+; use try/catch for compatibility
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                        endCal.getTimeInMillis(), pi);
            } catch (SecurityException e) {
                // Exact alarms not permitted on this device/OS version — fall back to inexact
                am.set(AlarmManager.RTC_WAKEUP, endCal.getTimeInMillis(), pi);
            }
        }
    }

}