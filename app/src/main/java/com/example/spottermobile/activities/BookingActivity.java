package com.example.spottermobile.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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

    private Spinner  spinnerSplit;
    private TextView tvSplitDescription, tvSelectedDate, tvSelectedTime, tvSlotStatus;
    private CardView cardDatePicker, cardTimePicker, cardCustomSplit;
    private EditText etCustomSplit;

    private DatabaseHelper    dbHelper;
    private SharedPreferences sharedPreferences;
    private int    userId;
    private String selectedDate = null;
    private String selectedTime = null;  // free-form HH:mm chosen by user

    // ── WORKOUT SPLITS ─────────────────────────────────────────────────────────
    // Each entry maps to a description shown below the spinner.

    private static final String[] SPLIT_NAMES = {
            "Push — Chest / Shoulders / Triceps",
            "Pull — Back / Biceps / Rear Delts",
            "Legs — Quads / Hamstrings / Glutes / Calves",
            "Upper Body — Chest, Back, Shoulders, Arms",
            "Lower Body — Full Legs + Core",
            "Full Body — Compound Lifts",
            "Core & Cardio — Abs / Obliques / Cardio",
            "Mobility & Stretching",
            "Custom — I'll describe my own"
    };

    private static final String[] SPLIT_DESCRIPTIONS = {
            "Pressing movements: bench, overhead press, dips, lateral raises, push-downs.",
            "Pulling movements: rows, pull-ups/downs, curls, face pulls.",
            "Squat, Romanian deadlift, leg press, lunges, calf raises.",
            "Balanced upper session: push + pull + shoulders + arms.",
            "Leg-focused with added core work: planks, leg raises, deadlifts.",
            "Full-body compound session: squat, bench, deadlift, rows, OHP.",
            "Core stability, ab work, and steady-state or interval cardio.",
            "Foam rolling, static stretches, and joint mobility drills.",
            "Enter your own workout focus below."
    };

    private static final int CUSTOM_SPLIT_INDEX = 8; // index of "Custom" in the arrays

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        spinnerSplit       = findViewById(R.id.spinnerSplit);
        tvSplitDescription = findViewById(R.id.tvSplitDescription);
        tvSelectedDate     = findViewById(R.id.tvSelectedDate);
        tvSelectedTime     = findViewById(R.id.tvSelectedTime);
        tvSlotStatus       = findViewById(R.id.tvSlotStatus);
        cardDatePicker     = findViewById(R.id.cardDatePicker);
        cardTimePicker     = findViewById(R.id.cardTimePicker);
        cardCustomSplit    = findViewById(R.id.cardCustomSplit);
        etCustomSplit      = findViewById(R.id.etCustomSplit);

        dbHelper          = new DatabaseHelper(this);
        sharedPreferences = getSharedPreferences("SpotterPrefs", MODE_PRIVATE);
        userId            = sharedPreferences.getInt("user_id", -1);

        setupSplitSpinner();
        setupDatePicker();
        setupTimePicker();

        Button btnBook = findViewById(R.id.btnConfirmBooking);
        btnBook.setOnClickListener(v -> confirmBooking());
    }

    // ── SPLIT SPINNER ──────────────────────────────────────────────────────────

    private void setupSplitSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, SPLIT_NAMES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSplit.setAdapter(adapter);

        // Show description and toggle custom input on selection
        spinnerSplit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                tvSplitDescription.setText(SPLIT_DESCRIPTIONS[position]);
                cardCustomSplit.setVisibility(
                        position == CUSTOM_SPLIT_INDEX ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Trigger initial description
        tvSplitDescription.setText(SPLIT_DESCRIPTIONS[0]);
    }

    // ── DATE PICKER ────────────────────────────────────────────────────────────

    private void setupDatePicker() {
        cardDatePicker.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) -> {
            Calendar chosen = Calendar.getInstance();
            chosen.set(y, m, d);

            if (chosen.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                new AlertDialog.Builder(this)
                        .setTitle("Gym Closed on Sundays")
                        .setMessage("Please choose a Monday–Saturday date.")
                        .setPositiveButton("Choose Again", (dlg, w) -> showDatePicker())
                        .setNegativeButton("Cancel", null)
                        .show();
                return;
            }

            selectedDate = String.format("%04d-%02d-%02d", y, m + 1, d);
            String[] months = {"Jan","Feb","Mar","Apr","May","Jun",
                    "Jul","Aug","Sep","Oct","Nov","Dec"};
            tvSelectedDate.setText(months[m] + " " + d + ", " + y);
            refreshDailyCapacity();

        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
        {{ getDatePicker().setMinDate(cal.getTimeInMillis()); }}
                .show();
    }

    // ── TIME PICKER ────────────────────────────────────────────────────────────

    private void setupTimePicker() {
        cardTimePicker.setOnClickListener(v -> showTimePicker());
    }

    private void showTimePicker() {
        Calendar cal = Calendar.getInstance();
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {

            // Validate gym hours: 5:00 AM (05:00) to midnight (00:00 next day → hour <= 23)
            // Gym opens at 5, closes at 0 (midnight). Disallow 1–4 AM.
            if (hourOfDay >= 1 && hourOfDay < 5) {
                new AlertDialog.Builder(this)
                        .setTitle("Outside Gym Hours")
                        .setMessage("The gym is open 5:00 AM – 12:00 AM (midnight).\nPlease choose a valid arrival time.")
                        .setPositiveButton("Pick Again", (dlg, w) -> showTimePicker())
                        .setNegativeButton("Cancel", null)
                        .show();
                return;
            }

            // Format for display and storage
            String amPm   = hourOfDay < 12 ? "AM" : "PM";
            int    hour12 = hourOfDay % 12;
            if (hour12 == 0) hour12 = 12;
            selectedTime = String.format("%02d:%02d %s", hour12, minute, amPm);
            tvSelectedTime.setText(selectedTime);

        }, cal.get(Calendar.HOUR_OF_DAY), 0, false).show();
    }

    // ── DAILY CAPACITY ─────────────────────────────────────────────────────────

    private void refreshDailyCapacity() {
        if (selectedDate == null || tvSlotStatus == null) return;

        int booked    = dbHelper.getDailyBookingCount(selectedDate);
        int available = DatabaseHelper.MAX_DAILY_CAPACITY - booked;

        if (available > 0) {
            tvSlotStatus.setText("✅ " + available + " spot(s) available on this date  ("
                    + booked + "/" + DatabaseHelper.MAX_DAILY_CAPACITY + " booked)");
            tvSlotStatus.setTextColor(getResources().getColor(R.color.primary_blue, null));
        } else {
            tvSlotStatus.setText("🔴 Fully booked for this date. Please choose another day.");
            tvSlotStatus.setTextColor(Color.RED);
        }
        tvSlotStatus.setVisibility(View.VISIBLE);
    }

    // ── CONFIRM ────────────────────────────────────────────────────────────────

    private void confirmBooking() {
        if (selectedDate == null) {
            Toast.makeText(this, "Please select a date.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedTime == null) {
            Toast.makeText(this, "Please set your arrival time.", Toast.LENGTH_SHORT).show();
            return;
        }

        int splitIndex = spinnerSplit.getSelectedItemPosition();
        String workoutSplit;

        if (splitIndex == CUSTOM_SPLIT_INDEX) {
            String custom = etCustomSplit.getText().toString().trim();
            if (custom.isEmpty()) {
                Toast.makeText(this, "Please describe your custom workout.", Toast.LENGTH_SHORT).show();
                return;
            }
            workoutSplit = "Custom: " + custom;
        } else {
            workoutSplit = SPLIT_NAMES[splitIndex];
        }

        // 1-per-day check
        if (dbHelper.isUserBookedOnDate(userId, selectedDate)) {
            Toast.makeText(this,
                    "You already have a booking on this date.\nOnly 1 booking per day allowed.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Daily cap check
        if (dbHelper.getDailyBookingCount(selectedDate) >= DatabaseHelper.MAX_DAILY_CAPACITY) {
            Toast.makeText(this,
                    "This date is fully booked (30/30).\nPlease choose a different date.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setWorkoutType(workoutSplit);  // stores split name
        booking.setTimeSlot(selectedTime);     // stores free arrival time
        booking.setSelectedDate(selectedDate);

        Booking result = dbHelper.addBooking(booking);

        if (result == null) {
            Toast.makeText(this, "Booking failed. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        refreshDailyCapacity();
        showQrDialog(result);
    }

    // ── QR CODE ────────────────────────────────────────────────────────────────

    private void showQrDialog(Booking booking) {
        String qrContent = "SPOTTER GYM\n"
                + "Booking #" + booking.getId() + "\n"
                + "User ID: " + booking.getUserId() + "\n"
                + "Split: " + booking.getWorkoutType() + "\n"
                + "Date: " + booking.getSelectedDate() + "\n"
                + "Arrival: " + booking.getTimeSlot() + "\n"
                + "Status: CONFIRMED";

        Bitmap qrBitmap = generateQrBitmap(qrContent, 600);

        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(qrBitmap);
        imageView.setPadding(32, 32, 32, 16);

        new AlertDialog.Builder(this)
                .setTitle("✅ Booking Confirmed!")
                .setMessage(booking.getWorkoutType()
                        + "\n📅 " + booking.getSelectedDate()
                        + "   🕐 Arrival: " + booking.getTimeSlot()
                        + "\n\nShow this QR to gym staff on arrival.")
                .setView(imageView)
                .setPositiveButton("Done", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private Bitmap generateQrBitmap(String content, int sizePx) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx);
            Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565);
            for (int x = 0; x < sizePx; x++)
                for (int y = 0; y < sizePx; y++)
                    bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }
}