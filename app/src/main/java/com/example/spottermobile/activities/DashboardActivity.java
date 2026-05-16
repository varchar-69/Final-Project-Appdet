package com.example.spottermobile.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.spottermobile.R;

public class DashboardActivity extends AppCompatActivity {
    private TextView tvWelcome, tvUserInfo;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        sharedPreferences = getSharedPreferences("SpotterPrefs", MODE_PRIVATE);
        initViews();
        loadUserInfo();
        setupNavigation();
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        tvUserInfo = findViewById(R.id.tvUserInfo);
    }

    private void loadUserInfo() {
        String fullName = sharedPreferences.getString("full_name", "User");
        String email = sharedPreferences.getString("email", "");
        tvWelcome.setText("Welcome back,\n" + fullName + "!");
        tvUserInfo.setText(email);
    }

    private void setupNavigation() {
        // Core Features
        findViewById(R.id.btnBookSession).setOnClickListener(v ->
                startActivity(new Intent(this, BookingActivity.class)));

        findViewById(R.id.btnBookingHistory).setOnClickListener(v ->
                startActivity(new Intent(this, BookingHistoryActivity.class)));

        findViewById(R.id.btnProfile).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        // Fitness Tools
        findViewById(R.id.btnBMICalculator).setOnClickListener(v ->
                startActivity(new Intent(this, BMIActivity.class)));

        // Workout History — now wired to WorkoutHistoryActivity (auto-populated on checkout)
        findViewById(R.id.btnWorkoutPrograms).setOnClickListener(v ->
                startActivity(new Intent(this, WorkoutHistoryActivity.class)));



        // Logout with confirmation
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