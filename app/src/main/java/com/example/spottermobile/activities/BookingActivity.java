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

public class BookingActivity extends AppCompatActivity {
    private Spinner spinnerWorkout, spinnerTime, spinnerBranch;
    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPreferences;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        initViews();
        setupSpinners();
        dbHelper = new DatabaseHelper(this);
        sharedPreferences = getSharedPreferences("SpotterPrefs", MODE_PRIVATE);
        userId = sharedPreferences.getInt("user_id", 1); // Default for now

        Button btnConfirm = findViewById(R.id.btnConfirmBooking);
        btnConfirm.setOnClickListener(v -> confirmBooking());
    }

    private void initViews() {
        spinnerWorkout = findViewById(R.id.spinnerWorkout);
        spinnerTime = findViewById(R.id.spinnerTime);
        spinnerBranch = findViewById(R.id.spinnerBranch);
    }

    private void setupSpinners() {
        // Workout Types
        String[] workouts = {"Cardio", "Weight Training", "Yoga", "CrossFit", "Swimming"};
        ArrayAdapter<String> workoutAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, workouts);
        workoutAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWorkout.setAdapter(workoutAdapter);

        // Time Slots
        String[] times = {"06:00 AM", "08:00 AM", "10:00 AM", "04:00 PM", "06:00 PM", "08:00 PM"};
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, times);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTime.setAdapter(timeAdapter);

        // Gym Branches
        String[] branches = {"Main Branch", "Downtown", "Uptown", "Sports Complex"};
        ArrayAdapter<String> branchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, branches);
        branchAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBranch.setAdapter(branchAdapter);
    }

    private void confirmBooking() {
        String workoutType = spinnerWorkout.getSelectedItem().toString();
        String timeSlot = spinnerTime.getSelectedItem().toString();
        String gymBranch = spinnerBranch.getSelectedItem().toString();

        if (dbHelper.addBooking(userId, workoutType, timeSlot, gymBranch)) {
            Toast.makeText(this, "✅ Booking Confirmed!\n" + workoutType + " at " + timeSlot, Toast.LENGTH_LONG).show();
            finish();
        } else {
            Toast.makeText(this, "❌ Booking failed. Try again.", Toast.LENGTH_SHORT).show();
        }
    }
}