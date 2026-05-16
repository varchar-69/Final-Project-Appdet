package com.example.spottermobile.activities;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spottermobile.R;
import com.example.spottermobile.adapters.BookingAdapter;
import com.example.spottermobile.database.DatabaseHelper;
import com.example.spottermobile.model.Booking;

import java.util.ArrayList;
import java.util.List;

public class BookingHistoryActivity extends AppCompatActivity {

    // ── Filter definitions ─────────────────────────────────────────────────────

    private static final String[] FILTER_LABELS  = {
            "All", "Upcoming", "Checked In", "Completed", "No Show", "Cancelled", "Waitlisted"
    };
    private static final String[] FILTER_STATUSES = {
            null, "booked", "checked_in", "completed", "no_show", "cancelled", "waitlisted"
    };

    // ── Views & state ──────────────────────────────────────────────────────────

    private RecyclerView    recyclerBookings;
    private LinearLayout    layoutEmpty;
    private TextView        tvEmpty;
    private LinearLayout    chipBar;

    private DatabaseHelper  dbHelper;
    private int             userId;

    private List<Booking>   allBookings;   // full unfiltered list
    private BookingAdapter  adapter;
    private int             activeChip = 0; // index into FILTER_LABELS

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_history);

        recyclerBookings = findViewById(R.id.recyclerBookings);
        layoutEmpty      = findViewById(R.id.layoutEmpty);
        tvEmpty          = findViewById(R.id.tvEmpty);
        chipBar          = findViewById(R.id.chipBar);

        dbHelper = new DatabaseHelper(this);
        SharedPreferences prefs = getSharedPreferences("SpotterPrefs", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        recyclerBookings.setLayoutManager(new LinearLayoutManager(this));

        buildChips();
        loadBookings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBookings();
    }

    // ── Chip bar ───────────────────────────────────────────────────────────────

    private void buildChips() {
        chipBar.removeAllViews();
        for (int i = 0; i < FILTER_LABELS.length; i++) {
            final int idx = i;
            TextView chip = new TextView(this);
            chip.setText(FILTER_LABELS[i]);
            chip.setTextSize(12f);
            chip.setPaddingRelative(28, 10, 28, 10);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(8);
            chip.setLayoutParams(lp);
            chip.setClickable(true);
            chip.setFocusable(true);

            styleChip(chip, i == activeChip);
            chip.setOnClickListener(v -> {
                activeChip = idx;
                rebuildChips();
                applyFilter();
            });

            chipBar.addView(chip);
        }
    }

    /** Applies bold only — avoids TextView.setTextStyle which doesn't exist. */
    private void setTextStyle(TextView tv, boolean bold) {
        tv.setTypeface(null, bold
                ? android.graphics.Typeface.BOLD
                : android.graphics.Typeface.NORMAL);
    }

    private void styleChip(TextView chip, boolean selected) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(50f);
        if (selected) {
            bg.setColor(Color.parseColor("#1E3A8A"));
            chip.setTextColor(Color.WHITE);
            chip.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            bg.setColor(Color.parseColor("#F0F4FF"));
            bg.setStroke(1, Color.parseColor("#C5D3F0"));
            chip.setTextColor(Color.parseColor("#334155"));
            chip.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
        chip.setBackground(bg);
    }

    private void rebuildChips() {
        for (int i = 0; i < chipBar.getChildCount(); i++) {
            View v = chipBar.getChildAt(i);
            if (v instanceof TextView) {
                styleChip((TextView) v, i == activeChip);
            }
        }
    }

    // ── Data loading ───────────────────────────────────────────────────────────

    private void loadBookings() {
        allBookings = dbHelper.getUserBookings(userId);
        applyFilter();
    }

    private void applyFilter() {
        String statusFilter = FILTER_STATUSES[activeChip];
        List<Booking> filtered;

        if (statusFilter == null) {
            filtered = new ArrayList<>(allBookings);
        } else {
            filtered = new ArrayList<>();
            for (Booking b : allBookings) {
                if (statusFilter.equals(b.getStatus())) {
                    filtered.add(b);
                }
            }
        }

        if (filtered.isEmpty()) {
            recyclerBookings.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
            String label = FILTER_LABELS[activeChip];
            tvEmpty.setText(activeChip == 0
                    ? "No bookings yet."
                    : "No " + label.toLowerCase() + " bookings.");
            return;
        }

        layoutEmpty.setVisibility(View.GONE);
        recyclerBookings.setVisibility(View.VISIBLE);

        adapter = new BookingAdapter(this, filtered, (booking, position) -> {
            String status = booking.getStatus();
            switch (status) {
                case "booked":
                case "waitlisted":
                    showCancelConfirmDialog(booking);
                    break;
                case "completed":
                    Toast.makeText(this,
                            "This session is already completed.",
                            Toast.LENGTH_SHORT).show();
                    break;
                case "no_show":
                    Toast.makeText(this,
                            "You were marked as a no-show for this session.",
                            Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(this,
                            "This booking cannot be modified.",
                            Toast.LENGTH_SHORT).show();
            }
        });
        recyclerBookings.setAdapter(adapter);
    }

    // ── Cancel ─────────────────────────────────────────────────────────────────

    private void showCancelConfirmDialog(Booking booking) {
        boolean isWaitlisted = "waitlisted".equals(booking.getStatus());
        String statusNote = isWaitlisted
                ? "\n\nThis is a waitlisted booking — cancelling removes you from the queue."
                : "\n\nCancelling a confirmed slot may promote someone from the waitlist.";

        new AlertDialog.Builder(this)
                .setTitle("Cancel Booking?")
                .setMessage(
                        "Are you sure you want to cancel this session?\n\n"
                                + "📋 " + booking.getWorkoutType() + "\n"
                                + "📅 " + booking.getSelectedDate() + "\n"
                                + "🕒 " + booking.getTimeSlot()
                                + statusNote)
                .setPositiveButton("Yes, Cancel", (dialog, which) -> cancelBooking(booking))
                .setNegativeButton("Keep It", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void cancelBooking(Booking booking) {
        boolean isWaitlisted = "waitlisted".equals(booking.getStatus());
        boolean success = isWaitlisted
                ? dbHelper.removeFromWaitlist(booking.getId(), userId)
                : dbHelper.cancelBooking(booking.getId(), userId);

        if (success) {
            Toast.makeText(this, "Booking cancelled.", Toast.LENGTH_SHORT).show();
            loadBookings();
        } else {
            Toast.makeText(this, "Cancel failed. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }
}