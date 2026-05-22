package com.example.spottermobile.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.spottermobile.R;
import com.example.spottermobile.database.FirestoreHelper;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    // ── Slot definitions (mirrors DatabaseHelper.GYM_TIME_SLOTS) ──────────────
    private static final String[] GYM_TIME_SLOTS = {
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

    private static final int MAX_SLOT_CAPACITY = 30;

    // ── Views ──────────────────────────────────────────────────────────────────
    private TextView        tvWelcome, tvUserInfo;
    private TextView        tvOccupancyCount, tvOccupancyStatus;
    private ProgressBar     progressOccupancy;
    private SharedPreferences sharedPreferences;
    private FirestoreHelper firestoreHelper;

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        sharedPreferences = getSharedPreferences("SpotterPrefs", MODE_PRIVATE);
        firestoreHelper   = new FirestoreHelper();

        initViews();
        loadUserInfo();
        setupNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOccupancy();
    }

    // ── View init ──────────────────────────────────────────────────────────────

    private void initViews() {
        tvWelcome         = findViewById(R.id.tvWelcome);
        tvUserInfo        = findViewById(R.id.tvUserInfo);
        tvOccupancyCount  = findViewById(R.id.tvOccupancyCount);
        tvOccupancyStatus = findViewById(R.id.tvOccupancyStatus);
        progressOccupancy = findViewById(R.id.progressOccupancy);
    }

    // ── Occupancy ──────────────────────────────────────────────────────────────

    /**
     * Determines which time slot is active right now by comparing the
     * current wall-clock time against each slot's start/end window.
     * Returns null if no slot is currently active.
     */
    private String getCurrentActiveSlot() {
        Calendar now = Calendar.getInstance();
        for (String slot : GYM_TIME_SLOTS) {
            String[] parts = slot.split(" - ");
            if (parts.length != 2) continue;
            Calendar start = buildTodayTimeCalendar(parts[0].trim());
            Calendar end   = buildTodayTimeCalendar(parts[1].trim());
            if (start == null || end == null) continue;
            if (!now.before(start) && now.before(end)) return slot;
        }
        return null;
    }

    /**
     * Parses a time string like "5:00 AM" or "11:00 PM" into a Calendar
     * set to today's date at that time. Returns null on parse failure.
     */
    private Calendar buildTodayTimeCalendar(String timeStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.US);
            Date parsed = sdf.parse(timeStr);
            if (parsed == null) return null;

            Calendar base = Calendar.getInstance();
            Calendar result = Calendar.getInstance();
            base.setTime(parsed);
            result.set(Calendar.HOUR_OF_DAY, base.get(Calendar.HOUR_OF_DAY));
            result.set(Calendar.MINUTE,      base.get(Calendar.MINUTE));
            result.set(Calendar.SECOND,      0);
            result.set(Calendar.MILLISECOND, 0);
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Fetches the live checked-in count for the current slot from Firestore,
     * then updates the occupancy card. If no slot is active right now the card
     * still shows a meaningful "No active session" message.
     */
    private void loadOccupancy() {
        String activeSlot = getCurrentActiveSlot();

        if (activeSlot == null) {
            // No active slot — render immediately without a network call
            renderOccupancy(0, null);
            return;
        }

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());

        // Count bookings in the current slot that are checked in
        firestoreHelper.getBookingsByDateAndStatuses(
                today,
                Arrays.asList("checked_in"),
                new FirestoreHelper.FirestoreCallback<Integer>() {
                    @Override
                    public void onSuccess(Integer count) {
                        renderOccupancy(count, activeSlot);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        // Degrade gracefully — show 0 rather than crashing
                        renderOccupancy(0, activeSlot);
                    }
                });
    }

    private void renderOccupancy(int current, String activeSlot) {
        tvOccupancyCount.setText(current + " / " + MAX_SLOT_CAPACITY);

        int pct = (int) ((current / (float) MAX_SLOT_CAPACITY) * 100);
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

    // ── User info ──────────────────────────────────────────────────────────────

    private void loadUserInfo() {
        String fullName = sharedPreferences.getString("full_name", "User");
        String email    = sharedPreferences.getString("email", "");
        tvWelcome.setText("Welcome back,\n" + fullName + "!");
        tvUserInfo.setText(email);
    }

    // ── Navigation ─────────────────────────────────────────────────────────────

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
                .setPositiveButton("YES",    (dialog, which) -> logout())
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void logout() {
        sharedPreferences.edit().clear().apply();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}