package com.example.spottermobile.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.spottermobile.R;
import com.example.spottermobile.database.FirestoreHelper;
import com.example.spottermobile.model.Booking;
import com.example.spottermobile.notifications.NoShowWorker;
import com.example.spottermobile.views.BookingsBarChartView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Arrays;

public class AdminDashboardActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private FirestoreHelper firestoreHelper;

    // Stat card TextViews
    private TextView tvStatBookings;
    private TextView tvStatCheckedIn;
    private TextView tvStatNoShows;
    private TextView tvStatWaitlisted;
    private TextView tvStatTotalMembers;
    private TextView tvStatRevenue;
    private TextView tvRevenueEmpty;
    private LinearLayout layoutRevenueTable;

    // 7-day bar chart
    private BookingsBarChartView barChartBookings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        sharedPreferences = getSharedPreferences("SpotterPrefs", MODE_PRIVATE);
        firestoreHelper = new FirestoreHelper();

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
        tvStatRevenue      = findViewById(R.id.tvStatRevenue);
        tvRevenueEmpty     = findViewById(R.id.tvRevenueEmpty);
        layoutRevenueTable = findViewById(R.id.layoutRevenueTable);
        barChartBookings   = findViewById(R.id.barChartBookings);
    }

    // ── LOAD STATS ─────────────────────────────────────────────────────────────

    /**
     * Fires all Firestore stat queries in parallel and updates each card
     * independently as results arrive. The bar chart and revenue table are
     * loaded separately so a slow query doesn't block the rest of the UI.
     */
    private void loadStats() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(Calendar.getInstance().getTime());

        loadTodayBookings(today);
        loadCheckedIn(today);
        loadNoShows(today);
        loadWaitlisted(today);
        loadTotalMembers();
        loadTotalRevenue();
        loadChartData();
        loadRevenueTable();
    }

    /** Today's confirmed + checked-in bookings count. */
    private void loadTodayBookings(String today) {
        firestoreHelper.getBookingsByDateAndStatuses(
                today, Arrays.asList("confirmed", "checked_in"),
                new FirestoreHelper.FirestoreCallback<Integer>() {
                    @Override public void onSuccess(Integer count) {
                        runOnUiThread(() -> tvStatBookings.setText(String.valueOf(count)));
                    }
                    @Override public void onFailure(String err) {
                        runOnUiThread(() -> tvStatBookings.setText("—"));
                    }
                });
    }

    /** Members currently checked in today. */
    private void loadCheckedIn(String today) {
        firestoreHelper.getBookingsByDateAndStatuses(
                today, Arrays.asList("confirmed", "checked_in"),
                new FirestoreHelper.FirestoreCallback<Integer>() {
                    @Override public void onSuccess(Integer count) {
                        runOnUiThread(() -> tvStatCheckedIn.setText(String.valueOf(count)));
                    }
                    @Override public void onFailure(String err) {
                        runOnUiThread(() -> tvStatCheckedIn.setText("—"));
                    }
                });
    }

    /** Today's no-show bookings count. */
    private void loadNoShows(String today) {
        firestoreHelper.getBookingsByDateAndStatuses(
                today, Arrays.asList("no_show"),
                new FirestoreHelper.FirestoreCallback<Integer>() {
                    @Override public void onSuccess(Integer count) {
                        runOnUiThread(() -> tvStatNoShows.setText(String.valueOf(count)));
                    }
                    @Override public void onFailure(String err) {
                        runOnUiThread(() -> tvStatNoShows.setText("—"));
                    }
                });
    }

    /** Today's waitlisted bookings count. */
    private void loadWaitlisted(String today) {
        firestoreHelper.getBookingsByDateAndStatuses(
                today, Arrays.asList("waitlisted"),
                new FirestoreHelper.FirestoreCallback<Integer>() {
                    @Override public void onSuccess(Integer count) {
                        runOnUiThread(() -> tvStatWaitlisted.setText(String.valueOf(count)));
                    }
                    @Override public void onFailure(String err) {
                        runOnUiThread(() -> tvStatWaitlisted.setText("—"));
                    }
                });
    }

    /** Total registered members (users with userType == "member"). */
    private void loadTotalMembers() {
        firestoreHelper.getMemberCount(new FirestoreHelper.FirestoreCallback<Integer>() {
            @Override public void onSuccess(Integer count) {
                runOnUiThread(() -> tvStatTotalMembers.setText(String.valueOf(count)));
            }
            @Override public void onFailure(String err) {
                runOnUiThread(() -> tvStatTotalMembers.setText("—"));
            }
        });
    }

    /** Total revenue from paid bookings. */
    private void loadTotalRevenue() {
        firestoreHelper.getTotalRevenue(new FirestoreHelper.FirestoreCallback<Double>() {
            @Override public void onSuccess(Double revenue) {
                runOnUiThread(() ->
                        tvStatRevenue.setText(
                                String.format(Locale.getDefault(), "\u20B1%.0f.00", revenue)));
            }
            @Override public void onFailure(String err) {
                runOnUiThread(() -> tvStatRevenue.setText("—"));
            }
        });
    }

    // ── CHART ─────────────────────────────────────────────────────────────────

    /**
     * Queries booking counts for each of the last 7 days and feeds results
     * into the custom bar chart. Counts are loaded in parallel; the chart is
     * rendered only after all 7 values are ready.
     */
    private void loadChartData() {
        int days = 7;
        String[] labels = new String[days];
        int[]    values = new int[days];

        SimpleDateFormat dbFmt  = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat dayFmt = new SimpleDateFormat("EEE", Locale.US);
        SimpleDateFormat mDFmt  = new SimpleDateFormat("MM/dd", Locale.US);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -(days - 1));

        String[] dates = new String[days];
        for (int i = 0; i < days; i++) {
            dates[i]  = dbFmt.format(cal.getTime());
            labels[i] = dayFmt.format(cal.getTime()) + "\n" + mDFmt.format(cal.getTime());
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        AtomicInteger remaining = new AtomicInteger(days);

        for (int i = 0; i < days; i++) {
            final int idx = i;
            firestoreHelper.getBookingsByDateAndStatuses(
                    dates[i], Arrays.asList("confirmed", "checked_in", "completed", "no_show"),
                    new FirestoreHelper.FirestoreCallback<Integer>() {
                        @Override public void onSuccess(Integer count) {
                            values[idx] = count;
                            if (remaining.decrementAndGet() == 0) {
                                runOnUiThread(() -> barChartBookings.setData(labels, values));
                            }
                        }
                        @Override public void onFailure(String err) {
                            values[idx] = 0;
                            if (remaining.decrementAndGet() == 0) {
                                runOnUiThread(() -> barChartBookings.setData(labels, values));
                            }
                        }
                    });
        }
    }

    // ── REVENUE TABLE ─────────────────────────────────────────────────────────

    private void loadRevenueTable() {
        firestoreHelper.getPaidBookingsWithNames(
                new FirestoreHelper.FirestoreCallback<List<Map<String, Object>>>() {
                    @Override
                    public void onSuccess(List<Map<String, Object>> paidBookings) {
                        runOnUiThread(() -> renderRevenueTable(paidBookings));
                    }
                    @Override
                    public void onFailure(String err) {
                        runOnUiThread(() -> {
                            tvRevenueEmpty.setVisibility(View.VISIBLE);
                            layoutRevenueTable.removeAllViews();
                        });
                    }
                });
    }

    private void renderRevenueTable(List<Map<String, Object>> paidBookings) {
        layoutRevenueTable.removeAllViews();

        if (paidBookings == null || paidBookings.isEmpty()) {
            tvRevenueEmpty.setVisibility(View.VISIBLE);
            return;
        }

        tvRevenueEmpty.setVisibility(View.GONE);

        // Build a Booking-like row from the raw Firestore map
        for (Map<String, Object> data : paidBookings) {
            String memberName      = (String) data.get("userName");
            String paymentMethod   = (String) data.get("paymentMethod");
            String paymentReference = (String) data.get("paymentReference");

            // Reuse a lightweight Booking for the existing row builder
            Booking b = new Booking();
            b.setMemberName(memberName);
            b.setPaymentMethod(paymentMethod);
            b.setPaymentReference(paymentReference);

            layoutRevenueTable.addView(buildRevenueRow(b));
        }
    }

    private View buildRevenueRow(Booking booking) {
        LinearLayout row = new LinearLayout(this);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(12), 0, dp(12));

        // Session price constant (200) kept local since DatabaseHelper is removed
        row.addView(buildRevenueCell(booking.getMemberName(),       1.3f, TextView.TEXT_ALIGNMENT_VIEW_START));
        row.addView(buildRevenueCell(booking.getPaymentMethod(),    0.9f, TextView.TEXT_ALIGNMENT_CENTER));
        row.addView(buildRevenueCell(booking.getPaymentReference(), 1.2f, TextView.TEXT_ALIGNMENT_CENTER));
        row.addView(buildRevenueCell("\u20B1200.00",                0.8f, TextView.TEXT_ALIGNMENT_VIEW_END));

        return row;
    }

    private TextView buildRevenueCell(String text, float weight, int alignment) {
        TextView cell = new TextView(this);
        cell.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, weight));
        cell.setText(text != null ? text : "—");
        cell.setTextSize(12);
        cell.setTextColor(getResources().getColor(R.color.dark_gray, null));
        cell.setTextAlignment(alignment);
        return cell;
    }

    // ── NAVIGATION ────────────────────────────────────────────────────────────

    private void setupAdminNavigation() {
        findViewById(R.id.btnViewUsers).setOnClickListener(v ->
                startActivity(new Intent(this, AdminUsersActivity.class)));

        findViewById(R.id.btnViewBookings).setOnClickListener(v ->
                startActivity(new Intent(this, AdminBookingsActivity.class)));

        findViewById(R.id.btnRevenue).setOnClickListener(v ->
                startActivity(new Intent(this, AdminRevenueActivity.class)));

        findViewById(R.id.btnLogout).setOnClickListener(v -> showLogoutDialog());
    }

    // ── WORKER ────────────────────────────────────────────────────────────────

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

    // ── LOGOUT ────────────────────────────────────────────────────────────────

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

    // ── UTILS ─────────────────────────────────────────────────────────────────

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}