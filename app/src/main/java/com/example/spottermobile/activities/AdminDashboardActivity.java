package com.example.spottermobile.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.spottermobile.R;
import com.example.spottermobile.database.DatabaseHelper;
import com.example.spottermobile.notifications.NoShowWorker;

import java.util.concurrent.TimeUnit;

public class AdminDashboardActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private DatabaseHelper dbHelper;

    // Stat card TextViews
    private TextView tvStatBookings;
    private TextView tvStatCheckedIn;
    private TextView tvStatNoShows;
    private TextView tvStatWaitlisted;
    private TextView tvStatTotalMembers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        sharedPreferences = getSharedPreferences("SpotterPrefs", MODE_PRIVATE);
        dbHelper = new DatabaseHelper(this);

        bindViews();
        setupAdminNavigation();
        scheduleNoShowWorker();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh stats every time admin returns to dashboard
        loadStats();
    }

    private void bindViews() {
        tvStatBookings     = findViewById(R.id.tvStatBookings);
        tvStatCheckedIn    = findViewById(R.id.tvStatCheckedIn);
        tvStatNoShows      = findViewById(R.id.tvStatNoShows);
        tvStatWaitlisted   = findViewById(R.id.tvStatWaitlisted);
        tvStatTotalMembers = findViewById(R.id.tvStatTotalMembers);
    }

    private void loadStats() {
        // All queries already exist in DatabaseHelper — just call and display them.
        tvStatBookings.setText(String.valueOf(dbHelper.getTodayBookingCount()));
        tvStatCheckedIn.setText(String.valueOf(dbHelper.getCurrentlyCheckedInCount()));
        tvStatNoShows.setText(String.valueOf(dbHelper.getTodayNoShowCount()));
        tvStatWaitlisted.setText(String.valueOf(dbHelper.getTodayWaitlistCount()));
        tvStatTotalMembers.setText(String.valueOf(dbHelper.getTotalMemberCount()));
    }

    private void setupAdminNavigation() {
        findViewById(R.id.btnViewUsers).setOnClickListener(v ->
                startActivity(new Intent(this, AdminUsersActivity.class)));

        findViewById(R.id.btnViewBookings).setOnClickListener(v ->
                startActivity(new Intent(this, AdminBookingsActivity.class)));

        findViewById(R.id.btnLogout).setOnClickListener(v -> showLogoutDialog());
    }

    /**
     * Schedules the NoShowWorker to run every 30 minutes.
     * KEEP policy means it won't create a duplicate if already scheduled.
     * Requires: implementation "androidx.work:work-runtime:2.9.0" in build.gradle
     */
    private void scheduleNoShowWorker() {
        PeriodicWorkRequest noShowWork =
                new PeriodicWorkRequest.Builder(NoShowWorker.class, 30, TimeUnit.MINUTES)
                        .build();
        WorkManager.getInstance(getApplicationContext())
                .enqueueUniquePeriodicWork(
                        "no_show_check",
                        ExistingPeriodicWorkPolicy.KEEP,
                        noShowWork);
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Admin Logout")
                .setMessage("Logout from admin panel?")
                .setPositiveButton("YES", (dialog, which) -> logout())
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void logout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("user_id");
        editor.remove("username");
        editor.remove("full_name");
        editor.remove("email");
        editor.remove("role");
        editor.remove("isLoggedIn");
        editor.apply();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
