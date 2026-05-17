package com.example.spottermobile.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.spottermobile.R;
import com.example.spottermobile.database.DatabaseHelper;
import com.example.spottermobile.model.Booking;

import java.util.Calendar;

public class BookingActivity extends AppCompatActivity {

    private Spinner spinnerSplit;
    private Spinner spinnerTimeSlot;

    private TextView tvSplitDescription;
    private TextView tvSelectedDate;
    private TextView tvSlotStatus;

    private CardView cardDatePicker;
    private CardView cardCustomSplit;

    private EditText etCustomSplit;

    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPreferences;

    private int userId;

    private String selectedDate = null;
    private String selectedTime = null;

    // ── FIXED TIME SLOTS ───────────────────────────────────────────────────────

    private static final String[] TIME_SLOTS = {
            "Select a time slot",
            // FIX: Use plain ASCII hyphen-minus instead of en-dash (U+2013).
            // The en-dash caused ZXing to garble the time slot string during QR
            // encode/decode (ISO-8859-1 vs UTF-8 mismatch), making every HMAC
            // verification fail and producing "Invalid QR" on every scan.
            "5:00 AM - 7:00 AM",
            "7:00 AM - 9:00 AM",
            "9:00 AM - 11:00 AM",
            "11:00 AM - 1:00 PM",
            "1:00 PM - 3:00 PM",
            "3:00 PM - 5:00 PM",
            "5:00 PM - 7:00 PM",
            "7:00 PM - 9:00 PM",
            "9:00 PM - 11:00 PM"
    };

    // ── WORKOUT SPLITS ─────────────────────────────────────────────────────────

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

    private static final int CUSTOM_SPLIT_INDEX = 8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        spinnerSplit = findViewById(R.id.spinnerSplit);
        spinnerTimeSlot = findViewById(R.id.spinnerTimeSlot);

        tvSplitDescription = findViewById(R.id.tvSplitDescription);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvSlotStatus = findViewById(R.id.tvSlotStatus);

        cardDatePicker = findViewById(R.id.cardDatePicker);
        cardCustomSplit = findViewById(R.id.cardCustomSplit);

        etCustomSplit = findViewById(R.id.etCustomSplit);

        dbHelper = new DatabaseHelper(this);

        sharedPreferences = getSharedPreferences("SpotterPrefs", MODE_PRIVATE);
        userId = sharedPreferences.getInt("user_id", -1);

        // Block booking if user already has active booking
        if (dbHelper.hasAnyActiveBooking(userId)) {
            showAlreadyBookedDialog();
            return;
        }

        setupSplitSpinner();
        setupTimeSlotSpinner();
        setupDatePicker();

        Button btnBook = findViewById(R.id.btnConfirmBooking);
        btnBook.setOnClickListener(v -> confirmBooking());
    }

    // ── ACTIVE BOOKING GUARD ───────────────────────────────────────────────────

    private void showAlreadyBookedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Active Booking Exists")
                .setMessage(
                        "You already have an active booking.\n\n" +
                                "You can only make a new booking after your current session " +
                                "is completed or cancelled.")
                .setPositiveButton("View My Bookings", (d, w) -> finish())
                .setNegativeButton("Go Back", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    // ── SPLIT SPINNER ──────────────────────────────────────────────────────────

    private void setupSplitSpinner() {

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                SPLIT_NAMES
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerSplit.setAdapter(adapter);

        spinnerSplit.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(AdapterView<?> parent,
                                               View view,
                                               int position,
                                               long id) {

                        tvSplitDescription.setText(
                                SPLIT_DESCRIPTIONS[position]
                        );

                        cardCustomSplit.setVisibility(
                                position == CUSTOM_SPLIT_INDEX
                                        ? View.VISIBLE
                                        : View.GONE
                        );
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                }
        );

        tvSplitDescription.setText(SPLIT_DESCRIPTIONS[0]);
    }

    // ── TIME SLOT SPINNER ──────────────────────────────────────────────────────

    private void setupTimeSlotSpinner() {

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                TIME_SLOTS
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerTimeSlot.setAdapter(adapter);

        spinnerTimeSlot.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(AdapterView<?> parent,
                                               View view,
                                               int position,
                                               long id) {

                        if (position == 0) {

                            selectedTime = null;

                            if (tvSlotStatus != null) {
                                tvSlotStatus.setVisibility(View.GONE);
                            }

                        } else {

                            selectedTime = TIME_SLOTS[position];
                            refreshSlotStatus();
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                }
        );
    }

    // ── DATE PICKER ────────────────────────────────────────────────────────────

    private void setupDatePicker() {
        cardDatePicker.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {

        Calendar cal = Calendar.getInstance();

        new DatePickerDialog(
                this,
                (view, y, m, d) -> {

                    Calendar chosen = Calendar.getInstance();
                    chosen.set(y, m, d);

                    selectedDate = String.format(
                            "%04d-%02d-%02d",
                            y,
                            m + 1,
                            d
                    );

                    String[] months = {
                            "Jan","Feb","Mar","Apr","May","Jun",
                            "Jul","Aug","Sep","Oct","Nov","Dec"
                    };

                    tvSelectedDate.setText(
                            months[m] + " " + d + ", " + y
                    );

                    refreshSlotStatus();

                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ) {{
            getDatePicker().setMinDate(cal.getTimeInMillis());
        }}.show();
    }

    // ── SLOT STATUS ────────────────────────────────────────────────────────────

    private void refreshSlotStatus() {

        if (tvSlotStatus == null) return;

        if (selectedDate == null || selectedTime == null) {
            tvSlotStatus.setVisibility(View.GONE);
            return;
        }

        int slotBooked = dbHelper.getSlotBookingCount(
                selectedDate,
                selectedTime
        );

        int slotAvailable =
                DatabaseHelper.MAX_SLOT_CAPACITY - slotBooked;

        int waitlisted =
                dbHelper.getWaitlistForSlot(
                        selectedDate,
                        selectedTime
                ).size();

        if (slotAvailable > 0) {

            tvSlotStatus.setText(
                    "✅ " + slotAvailable + " spot(s) open  ("
                            + slotBooked + "/"
                            + DatabaseHelper.MAX_SLOT_CAPACITY
                            + " booked)"
            );

            tvSlotStatus.setTextColor(
                    getResources().getColor(
                            R.color.primary_blue,
                            null
                    )
            );

        } else if (waitlisted > 0) {

            tvSlotStatus.setText(
                    "⏳ Full — "
                            + waitlisted
                            + " on waitlist. You'll be #"
                            + (waitlisted + 1)
            );

            tvSlotStatus.setTextColor(
                    Color.parseColor("#FF8C00")
            );

        } else {

            tvSlotStatus.setText(
                    "🔴 Full — you'll be #1 on the waitlist"
            );

            tvSlotStatus.setTextColor(Color.RED);
        }

        tvSlotStatus.setVisibility(View.VISIBLE);
    }

    // ── CONFIRM BOOKING ────────────────────────────────────────────────────────

    private void confirmBooking() {

        if (selectedDate == null) {

            Toast.makeText(
                    this,
                    "Please select a date.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (selectedTime == null) {

            Toast.makeText(
                    this,
                    "Please select a time slot.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        int splitIndex =
                spinnerSplit.getSelectedItemPosition();

        String workoutSplit;

        // Custom split
        if (splitIndex == CUSTOM_SPLIT_INDEX) {

            String custom =
                    etCustomSplit.getText().toString().trim();

            if (custom.isEmpty()) {

                Toast.makeText(
                        this,
                        "Please describe your custom workout.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            workoutSplit = "Custom: " + custom;

        } else {

            workoutSplit = SPLIT_NAMES[splitIndex];
        }

        // Active booking guard
        if (dbHelper.hasAnyActiveBooking(userId)) {
            showAlreadyBookedDialog();
            return;
        }

        // Same-day duplicate guard
        if (dbHelper.isUserBookedOnDate(userId, selectedDate)) {

            Toast.makeText(
                    this,
                    "You already have a booking on this date.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        // Duplicate waitlist guard
        if (dbHelper.isUserWaitlistedForSlot(
                userId,
                selectedDate,
                selectedTime
        )) {

            Toast.makeText(
                    this,
                    "You're already on the waitlist for this slot.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        Booking booking = new Booking();

        booking.setUserId(userId);
        booking.setWorkoutType(workoutSplit);
        booking.setTimeSlot(selectedTime);
        booking.setSelectedDate(selectedDate);

        Booking result = dbHelper.addBooking(booking);

        if (result == null) {

            Toast.makeText(
                    this,
                    "Booking failed. Please try again.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        refreshSlotStatus();

        if (DatabaseHelper.STATUS_WAITLISTED.equals(
                result.getStatus()
        )) {

            showWaitlistDialog(result);

        } else {

            showQrDialog(result);
        }
    }

    // ── WAITLIST DIALOG ────────────────────────────────────────────────────────

    private void showWaitlistDialog(Booking booking) {
        Intent intent = new Intent(this, BookingConfirmedActivity.class);
        intent.putExtra(BookingConfirmedActivity.EXTRA_BOOKING_ID,   booking.getId());
        intent.putExtra(BookingConfirmedActivity.EXTRA_USER_ID,      booking.getUserId());
        intent.putExtra(BookingConfirmedActivity.EXTRA_WORKOUT_TYPE, booking.getWorkoutType());
        intent.putExtra(BookingConfirmedActivity.EXTRA_DATE,         booking.getSelectedDate());
        intent.putExtra(BookingConfirmedActivity.EXTRA_TIME_SLOT,    booking.getTimeSlot());
        intent.putExtra(BookingConfirmedActivity.EXTRA_IS_WAITLIST,  true);
        intent.putExtra(BookingConfirmedActivity.EXTRA_QUEUE_POS,    booking.getQueuePosition());
        startActivity(intent);
        finish();
    }

    // ── QR CODE ────────────────────────────────────────────────────────────────

    private void showQrDialog(Booking booking) {
        Intent intent = new Intent(this, BookingConfirmedActivity.class);
        intent.putExtra(BookingConfirmedActivity.EXTRA_BOOKING_ID,   booking.getId());
        intent.putExtra(BookingConfirmedActivity.EXTRA_USER_ID,      booking.getUserId());
        intent.putExtra(BookingConfirmedActivity.EXTRA_WORKOUT_TYPE, booking.getWorkoutType());
        intent.putExtra(BookingConfirmedActivity.EXTRA_DATE,         booking.getSelectedDate());
        intent.putExtra(BookingConfirmedActivity.EXTRA_TIME_SLOT,    booking.getTimeSlot());
        intent.putExtra(BookingConfirmedActivity.EXTRA_IS_WAITLIST,  false);
        startActivity(intent);
        finish();
    }

}
