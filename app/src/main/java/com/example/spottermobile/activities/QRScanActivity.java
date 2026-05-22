package com.example.spottermobile.activities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.spottermobile.database.FirestoreHelper;
import com.example.spottermobile.model.Booking;
import com.example.spottermobile.notifications.AutoCheckoutReceiver;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class QRScanActivity extends AppCompatActivity {

    private static final String TAG = "QRScanActivity";

    private FirestoreHelper firestoreHelper;

    // Action constants — passed via Intent extras from AdminBookingsActivity
    public static final String EXTRA_SCAN_MODE = "scan_mode";
    public static final String MODE_CHECKIN    = "checkin";
    public static final String MODE_CHECKOUT   = "checkout";

    // Firestore status strings (mirrors FirestoreHelper / Booking model)
    private static final String STATUS_CONFIRMED  = "confirmed";
    private static final String STATUS_CHECKED_IN = "checked_in";
    private static final String STATUS_COMPLETED  = "completed";
    private static final String STATUS_CANCELLED  = "cancelled";

    private String scanMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firestoreHelper = new FirestoreHelper();
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
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null) {
            if (result.getContents() == null) {
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
     * QR format (produced by BookingActivity / QrTokenUtils):
     *   SPOTTER|<bookingId>|<userId>|<date>|<timeSlot>|<hmacToken>
     *
     * QrTokenUtils.verifyAndExtractBookingId() validates the HMAC and returns
     * the booking ID (as an int for legacy reasons — we convert to String for
     * Firestore document lookup).
     */
    private void handleScanResult(String qrContent) {
        // AFTER
        String bookingId = com.example.spottermobile.utils.QrTokenUtils
                .verifyAndExtractBookingId(qrContent);

        if (bookingId == null) {
            showResultDialog("Invalid QR",
                    "This QR code is not a valid Spotter Gym booking.", false);
            return;
        }


        firestoreHelper.getBookingById(bookingId, new FirestoreHelper.FirestoreCallback<Booking>() {
            @Override
            public void onSuccess(Booking booking) {
                if (MODE_CHECKIN.equals(scanMode)) {
                    processCheckIn(booking);
                } else {
                    processCheckOut(booking);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "getBookingById failed: " + errorMessage);
                showResultDialog("Not Found",
                        "No booking found for ID #" + bookingId + ".", false);
            }
        });
    }

    // ── CHECK-IN ───────────────────────────────────────────────────────────────

    private void processCheckIn(Booking booking) {
        String currentStatus = booking.getStatus();

        if (STATUS_CHECKED_IN.equals(currentStatus)) {
            showResultDialog("Already Checked In",
                    "Booking #" + booking.getFirestoreId() + " is already checked in at "
                            + booking.getCheckinTime() + ".", false);
            return;
        }

        if (STATUS_COMPLETED.equals(currentStatus)) {
            showResultDialog("Session Completed",
                    "This booking has already been completed (checked out).", false);
            return;
        }

        if (STATUS_CANCELLED.equals(currentStatus)) {
            showResultDialog("Booking Cancelled",
                    "This booking was cancelled and cannot be used for check-in.", false);
            return;
        }

        if (!STATUS_CONFIRMED.equals(currentStatus)) {
            showResultDialog("Not Valid",
                    "Booking #" + booking.getFirestoreId() + " has status: " + currentStatus
                            + ". Only confirmed bookings can check in.", false);
            return;
        }

        firestoreHelper.checkInBooking(booking.getFirestoreId(),
                new FirestoreHelper.FirestoreCallback<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        // Schedule auto-checkout at slot end in case the member forgets to scan out
                        scheduleAutoCheckout(
                                booking.getFirestoreId(),
                                booking.getSelectedDate(),
                                booking.getTimeSlot());

                        showResultDialog("✅ Checked In!",
                                "Welcome!\n\n"
                                        + "Member: " + booking.getMemberName() + "\n"
                                        + "Workout: " + booking.getWorkoutType() + "\n"
                                        + "Slot: " + booking.getTimeSlot() + "\n"
                                        + "Date: " + booking.getSelectedDate(),
                                true);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "checkInBooking failed: " + errorMessage);
                        showResultDialog("Check-In Failed",
                                "Could not check in booking #" + booking.getFirestoreId()
                                        + ". Please try again.", false);
                    }
                });
    }

    // ── CHECK-OUT ──────────────────────────────────────────────────────────────

    private void processCheckOut(Booking booking) {
        String currentStatus = booking.getStatus();

        if (!STATUS_CHECKED_IN.equals(currentStatus)) {
            String msg = STATUS_COMPLETED.equals(currentStatus)
                    ? "This member has already checked out."
                    : "This booking is not currently checked in. Status: " + currentStatus;
            showResultDialog("Cannot Check Out", msg, false);
            return;
        }

        firestoreHelper.checkOutBooking(booking.getFirestoreId(),
                new FirestoreHelper.FirestoreCallback<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        showResultDialog("🏁 Checked Out!",
                                "Session complete!\n\n"
                                        + "Member: " + booking.getMemberName() + "\n"
                                        + "Workout: " + booking.getWorkoutType() + "\n"
                                        + "Checked in at: " + booking.getCheckinTime(),
                                true);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "checkOutBooking failed: " + errorMessage);
                        showResultDialog("Check-Out Failed",
                                "Could not check out booking #" + booking.getFirestoreId()
                                        + ". Please try again.", false);
                    }
                });
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
     * AutoCheckoutReceiver will call firestoreHelper.checkOutBooking() if the
     * booking is still CHECKED_IN at that point.
     *
     * Time slot format: "08:00 AM - 09:00 AM"
     * We parse the end half ("09:00 AM") to build the Calendar trigger time.
     */
    private void scheduleAutoCheckout(String bookingId, String date, String timeSlot) {
        Calendar endCal = parseSlotEndCalendar(date, timeSlot);
        if (endCal == null) return; // couldn't parse slot — skip silently

        // If slot end is already in the past, fire in 1 second (edge case)
        if (endCal.getTimeInMillis() < System.currentTimeMillis()) {
            endCal = Calendar.getInstance();
            endCal.add(Calendar.SECOND, 1);
        }

        Intent intent = new Intent(this, AutoCheckoutReceiver.class);
        intent.setAction(AutoCheckoutReceiver.ACTION_AUTO_CHECKOUT);
        intent.putExtra(AutoCheckoutReceiver.EXTRA_BOOKING_ID, bookingId);

        // Use bookingId.hashCode() as requestCode so each booking gets its own alarm
        int requestCode = bookingId.hashCode();
        PendingIntent pi = PendingIntent.getBroadcast(this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            try {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                        endCal.getTimeInMillis(), pi);
            } catch (SecurityException e) {
                // Exact alarms not permitted on this device/OS — fall back to inexact
                am.set(AlarmManager.RTC_WAKEUP, endCal.getTimeInMillis(), pi);
            }
        }
    }

    /**
     * Parses the end time of a time slot string into a Calendar set on the given date.
     *
     * Expected slot format: "08:00 AM - 09:00 AM"
     * Expected date format: "yyyy-MM-dd"
     *
     * Returns null if parsing fails so the caller can skip silently.
     */
    private static Calendar parseSlotEndCalendar(String date, String timeSlot) {
        try {
            // Split "08:00 AM - 09:00 AM" → take the right half "09:00 AM"
            String[] parts = timeSlot.split(" - ");
            if (parts.length < 2) return null;
            String endTimeStr = parts[1].trim();

            // Combine date + end time into a single parseable string
            String combined = date + " " + endTimeStr;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
            java.util.Date parsed = sdf.parse(combined);
            if (parsed == null) return null;

            Calendar cal = Calendar.getInstance();
            cal.setTime(parsed);
            return cal;
        } catch (Exception e) {
            Log.w(TAG, "Could not parse slot end calendar for date=" + date
                    + " slot=" + timeSlot, e);
            return null;
        }
    }
}