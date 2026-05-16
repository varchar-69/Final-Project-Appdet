package com.example.spottermobile.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.spottermobile.R;
import com.example.spottermobile.database.DatabaseHelper;

public class DashboardActivity extends AppCompatActivity {
    private TextView tvWelcome, tvUserInfo;
    private TextView tvOccupancyCount, tvOccupancyStatus;
    private ProgressBar progressOccupancy;
    private SharedPreferences sharedPreferences;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        sharedPreferences = getSharedPreferences("SpotterPrefs", MODE_PRIVATE);
        dbHelper = new DatabaseHelper(this);
        initViews();
        loadUserInfo();
        setupNavigation();
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        tvUserInfo = findViewById(R.id.tvUserInfo);
        tvOccupancyCount = findViewById(R.id.tvOccupancyCount);
        tvOccupancyStatus = findViewById(R.id.tvOccupancyStatus);
        progressOccupancy = findViewById(R.id.progressOccupancy);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOccupancy();
    }

    private void loadOccupancy() {
        int current = dbHelper.getCurrentlyCheckedInCount();
        String activeSlot = dbHelper.getCurrentActiveSlot();
        int capacity = DatabaseHelper.MAX_SLOT_CAPACITY;

        tvOccupancyCount.setText(current + " / " + capacity);

        int pct = (int) ((current / (float) capacity) * 100);
        progressOccupancy.setProgress(pct);

        String statusMsg;
        int tint;
        if (activeSlot == null) {
            statusMsg = "No active session slot right now";
            tint = android.graphics.Color.parseColor("#6B7280");
        } else if (pct >= 90) {
            statusMsg = "Almost full - limited spots available";
            tint = android.graphics.Color.parseColor("#EF4444");
        } else if (pct >= 60) {
            statusMsg = "Getting busy";
            tint = android.graphics.Color.parseColor("#F59E0B");
        } else if (current == 0) {
            statusMsg = "Gym is currently empty";
            tint = android.graphics.Color.parseColor("#10B981");
        } else {
            statusMsg = "Plenty of space available";
            tint = android.graphics.Color.parseColor("#10B981");
        }

        if (activeSlot != null) {
            statusMsg = statusMsg + " | " + activeSlot + " slot";
        }

        tvOccupancyStatus.setText(statusMsg);
        tvOccupancyCount.setTextColor(tint);
        progressOccupancy.setProgressTintList(
                android.content.res.ColorStateList.valueOf(tint));
    }

    private void loadUserInfo() {
        String fullName = sharedPreferences.getString("full_name", "User");
        String email = sharedPreferences.getString("email", "");
        tvWelcome.setText("Welcome back,\n" + fullName + "!");
        tvUserInfo.setText(email);
    }

    private void setupNavigation() {
        findViewById(R.id.btnBookSession).setOnClickListener(v ->
                startActivity(new Intent(this, BookingActivity.class)));

        findViewById(R.id.btnBookingHistory).setOnClickListener(v ->
                startActivity(new Intent(this, BookingHistoryActivity.class)));

        findViewById(R.id.btnProfile).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        findViewById(R.id.btnBMICalculator).setOnClickListener(v ->
                startActivity(new Intent(this, BMIActivity.class)));

        findViewById(R.id.btnWorkoutPrograms).setOnClickListener(v ->
                startActivity(new Intent(this, WorkoutHistoryActivity.class)));

        findViewById(R.id.btnLogout).setOnClickListener(v -> showLogoutDialog());
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("YES", (dialog, which) -> logout())
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void logout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
