package com.example.spottermobile.activities;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.spottermobile.R;
import com.example.spottermobile.database.DatabaseHelper;
import com.example.spottermobile.model.Booking;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.Calendar;

public class BookingActivity extends AppCompatActivity {

    private Spinner     spinnerWorkout, spinnerTime;
    private TextView    tvSelectedDate, tvSlotStatus;
    private CardView    cardDatePicker;
    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPreferences;
    private int    userId;
    private String selectedDate = null;

    private final String[] workoutTypes = {
            "Cardio Training",
            "Strength Training",
            "CrossFit",
            "Yoga Session",
            "HIIT Workout",
            "Bodybuilding"
    };

    private final String[] timeSlots = {
            "08:00 AM", "09:00 AM", "10:00 AM", "11:00 AM",
            "12:00 PM", "01:00 PM", "02:00 PM", "03:00 PM",
            "04:00 PM", "05:00 PM", "06:00 PM", "07:00 PM"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        spinnerWorkout = findViewById(R.id.spinnerWorkout);
        spinnerTime    = findViewById(R.id.spinnerTime);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvSlotStatus   = findViewById(R.id.tvSlotStatus);
        cardDatePicker = findViewById(R.id.cardDatePicker);

        dbHelper          = new DatabaseHelper(this);
        sharedPreferences = getSharedPreferences("SpotterPrefs", MODE_PRIVATE);
        userId            = sharedPreferences.getInt("user_id", -1);

        setupWorkoutSpinner();
        setupTimeSpinner();
        setupDatePicker();

        Button btnBook = findViewById(R.id.btnConfirmBooking);
        btnBook.setOnClickListener(v -> confirmBooking());
    }

    // ── SPINNERS ───────────────────────────────────────────────────────────────

    private void setupWorkoutSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, workoutTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWorkout.setAdapter(adapter);
    }

    private void setupTimeSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, timeSlots);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTime.setAdapter(adapter);

        // Refresh slot status whenever user picks a different time
        spinnerTime.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshSlotStatus();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // ── DATE PICKER ────────────────────────────────────────────────────────────

    private void setupDatePicker() {
        cardDatePicker.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog picker = new DatePickerDialog(
                this,
                (view, y, m, d) -> {
                    Calendar chosen = Calendar.getInstance();
                    chosen.set(y, m, d);

                    if (chosen.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                        new AlertDialog.Builder(this)
                                .setTitle("Day Not Available")
                                .setMessage("Sundays are not available. The gym is closed. Please choose another day.")
                                .setPositiveButton("Choose Another Day", (dialog, which) -> showDatePicker())
                                .setNegativeButton("Cancel", null)
                                .show();
                        return;
                    }

                    selectedDate = String.format("%04d-%02d-%02d", y, m + 1, d);
                    String[] monthNames = {
                            "Jan","Feb","Mar","Apr","May","Jun",
                            "Jul","Aug","Sep","Oct","Nov","Dec"
                    };
                    tvSelectedDate.setText(monthNames[m] + " " + d + ", " + y);
                    refreshSlotStatus(); // update availability once date is picked
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        picker.getDatePicker().setMinDate(cal.getTimeInMillis());
        picker.show();
    }

    // ── SLOT STATUS ────────────────────────────────────────────────────────────

    /**
     * Updates the slot-status label below the time spinner.
     * Shows: available count, or "FULL – waitlist position N" if over capacity.
     */
    private void refreshSlotStatus() {
        if (selectedDate == null || tvSlotStatus == null) return;

        String timeSlot  = spinnerTime.getSelectedItem().toString();
        int booked       = dbHelper.getSlotCount(selectedDate, timeSlot);
        int waitlisted   = dbHelper.getWaitlistCount(selectedDate, timeSlot);
        int available    = DatabaseHelper.MAX_SLOT_CAPACITY - booked;

        if (available > 0) {
            tvSlotStatus.setText("✅ " + available + " slot(s) available out of "
                    + DatabaseHelper.MAX_SLOT_CAPACITY);
            tvSlotStatus.setTextColor(getResources().getColor(R.color.primary_blue, null));
        } else {
            tvSlotStatus.setText("🔴 FULL — " + waitlisted
                    + " on waitlist. You'll be #" + (waitlisted + 1) + " if you book now.");
            tvSlotStatus.setTextColor(Color.RED);
        }
        tvSlotStatus.setVisibility(View.VISIBLE);
    }

    // ── CONFIRM BOOKING ────────────────────────────────────────────────────────

    private void confirmBooking() {
        if (selectedDate == null) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
            return;
        }

        String workout = spinnerWorkout.getSelectedItem().toString();
        String time    = spinnerTime.getSelectedItem().toString();

        // Check 1-per-day rule before touching the DB
        if (dbHelper.isUserBookedOnDate(userId, selectedDate)) {
            Toast.makeText(this,
                    "You already have a booking on this date.\nOnly 1 booking per day is allowed.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setWorkoutType(workout);
        booking.setTimeSlot(time);
        booking.setSelectedDate(selectedDate);

        Booking result = dbHelper.addBooking(booking);

        if (result == null) {
            Toast.makeText(this, "✗ Booking failed. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        refreshSlotStatus(); // update label after booking

        if (DatabaseHelper.STATUS_BOOKED.equals(result.getStatus())) {
            showQrDialog(result);
        } else {
            // Waitlisted — no QR, just inform
            int waitPos = dbHelper.getWaitlistCount(selectedDate, time);
            new AlertDialog.Builder(this)
                    .setTitle("Added to Waitlist")
                    .setMessage("The " + time + " slot on " + selectedDate
                            + " is full.\n\nYou are #" + waitPos
                            + " on the waitlist.\n\nYou will be automatically confirmed "
                            + "if a spot opens up (FIFO).")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    // ── QR CODE ────────────────────────────────────────────────────────────────

    /**
     * Builds a QR code from booking details and shows it in an AlertDialog.
     * QR content: human-readable text the gym staff can scan to verify entry.
     */
    private void showQrDialog(Booking booking) {
        String qrContent = "SPOTTER GYM\n"
                + "Booking #" + booking.getId() + "\n"
                + "User ID: " + booking.getUserId() + "\n"
                + "Workout: " + booking.getWorkoutType() + "\n"
                + "Date: " + booking.getSelectedDate() + "\n"
                + "Time: " + booking.getTimeSlot() + "\n"
                + "Status: CONFIRMED";

        Bitmap qrBitmap = generateQrBitmap(qrContent, 600);

        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(qrBitmap);
        imageView.setPadding(32, 32, 32, 16);

        new AlertDialog.Builder(this)
                .setTitle("✅ Booking Confirmed!")
                .setMessage("Show this QR code at the gym entrance.\n\n"
                        + booking.getWorkoutType() + "  •  " + booking.getTimeSlot()
                        + "\n" + booking.getSelectedDate())
                .setView(imageView)
                .setPositiveButton("Done", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    /**
     * Generates a QR code Bitmap using ZXing.
     * Add to app/build.gradle:  implementation 'com.google.zxing:core:3.5.3'
     */
    private Bitmap generateQrBitmap(String content, int sizePx) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx);
            Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565);
            for (int x = 0; x < sizePx; x++) {
                for (int y = 0; y < sizePx; y++) {
                    bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }
}