package com.example.spottermobile.activities;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.spottermobile.R;
import com.example.spottermobile.database.DatabaseHelper;
import com.example.spottermobile.model.Booking;

import java.util.Calendar;

public class BookingActivity extends AppCompatActivity {

    private Spinner spinnerWorkout, spinnerTime;
    private TextView tvSelectedDate;
    private CardView cardDatePicker;
    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPreferences;
    private int userId;
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
            "08:00 AM",
            "09:00 AM",
            "10:00 AM",
            "11:00 AM",
            "12:00 PM",
            "01:00 PM",
            "02:00 PM",
            "03:00 PM",
            "04:00 PM",
            "05:00 PM",
            "06:00 PM",
            "07:00 PM"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        spinnerWorkout = findViewById(R.id.spinnerWorkout);
        spinnerTime    = findViewById(R.id.spinnerTime);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
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

    private void setupWorkoutSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                workoutTypes
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWorkout.setAdapter(adapter);
    }

    private void setupTimeSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                timeSlots
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTime.setAdapter(adapter);
    }

    private void setupDatePicker() {
        cardDatePicker.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        int year  = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day   = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog picker = new DatePickerDialog(
                BookingActivity.this,
                (view, y, m, d) -> {
                    Calendar chosen = Calendar.getInstance();
                    chosen.set(y, m, d);

                    // Block Sundays — re-open picker so user can pick again
                    if (chosen.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                        new androidx.appcompat.app.AlertDialog.Builder(BookingActivity.this)
                                .setTitle("Day Not Available")
                                .setMessage("Sundays are not available for booking. The school gym is closed on Sundays. Please choose another day.")
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
                },
                year, month, day
        );

        picker.getDatePicker().setMinDate(cal.getTimeInMillis());
        picker.show();
    }

    private void confirmBooking() {
        if (selectedDate == null) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
            return;
        }

        String workout = spinnerWorkout.getSelectedItem().toString();
        String time    = spinnerTime.getSelectedItem().toString();

        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setWorkoutType(workout);
        booking.setTimeSlot(time);
        booking.setSelectedDate(selectedDate);
        booking.setStatus("booked");

        boolean success = dbHelper.addBooking(booking);

        if (success) {
            Toast.makeText(
                    this,
                    "Booking Confirmed!\n" + workout + " at " + time + "\n " + selectedDate,
                    Toast.LENGTH_LONG
            ).show();
        } else {
            Toast.makeText(
                    this,
                    "✗ Booking failed",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
}