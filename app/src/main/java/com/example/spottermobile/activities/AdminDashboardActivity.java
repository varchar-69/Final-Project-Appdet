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
import com.example.spottermobile.views.BookingsBarChartView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
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

    // 7-day bar chart
    private BookingsBarChartView barChartBookings;

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
        barChartBookings   = findViewById(R.id.barChartBookings);
    }

    private void loadStats() {
        // All queries already exist in DatabaseHelper — just call and display them.
        tvStatBookings.setText(String.valueOf(dbHelper.getTodayBookingCount()));
        tvStatCheckedIn.setText(String.valueOf(dbHelper.getCurrentlyCheckedInCount()));
        tvStatNoShows.setText(String.valueOf(dbHelper.getTodayNoShowCount()));
        tvStatWaitlisted.setText(String.valueOf(dbHelper.getTodayWaitlistCount()));
        tvStatTotalMembers.setText(String.valueOf(dbHelper.getTotalMemberCount()));

        // Build 7-day chart data
        loadChartData();
    }

    /**
     * Queries getDailyBookingCount() for each of the last 7 days and feeds
     * the result to the custom bar chart view.
     */
    private void loadChartData() {
        int     days    = 7;
        String[] labels = new String[days];
        int[]    values = new int[days];

        SimpleDateFormat dbFmt  = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat dayFmt = new SimpleDateFormat("EEE", Locale.US);
        SimpleDateFormat mDFmt  = new SimpleDateFormat("MM/dd", Locale.US);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -(days - 1)); // start N-1 days ago

        for (int i = 0; i < days; i++) {
            String dateStr = dbFmt.format(cal.getTime());
            values[i] = dbHelper.getDailyBookingCount(dateStr);
            labels[i] = dayFmt.format(cal.getTime()) + "\n" + mDFmt.format(cal.getTime());
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        barChartBookings.setData(labels, values);
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
