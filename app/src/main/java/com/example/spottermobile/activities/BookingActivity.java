package com.example.spottermobile.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spottermobile.R;
import com.example.spottermobile.database.DatabaseHelper;
import com.example.spottermobile.model.Booking;

public class BookingActivity extends AppCompatActivity {

    private Spinner spinnerWorkout, spinnerTime;
    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPreferences;
    private int userId;

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

        // FIX: Removed @SuppressLint — use correct Spinner IDs from layout
        spinnerWorkout = findViewById(R.id.spinnerWorkout);
        spinnerTime = findViewById(R.id.spinnerTime);

        dbHelper = new DatabaseHelper(this);
        sharedPreferences = getSharedPreferences("SpotterPrefs", MODE_PRIVATE);
        userId = sharedPreferences.getInt("user_id", -1);

        setupWorkoutSpinner();
        setupTimeSpinner();

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

    private void confirmBooking() {
        String workout = spinnerWorkout.getSelectedItem().toString();
        String time = spinnerTime.getSelectedItem().toString();

        // FIX: Use setters instead of a constructor — date is handled by DatabaseHelper
        // FIX: Set status to "booked" so the DB doesn't store null
        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setWorkoutType(workout);
        booking.setTimeSlot(time);
        booking.setStatus("booked");

        // FIX: addBooking() returns boolean, not long
        boolean success = dbHelper.addBooking(booking);

        if (success) {
            Toast.makeText(
                    this,
                    "✅ Booking Confirmed!\n" + workout + " at " + time,
                    Toast.LENGTH_LONG
            ).show();
        } else {
            Toast.makeText(
                    this,
                    "❌ Booking failed",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
}