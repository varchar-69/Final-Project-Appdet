package com.example.spottermobile.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spottermobile.R;
import com.example.spottermobile.adapters.BookingAdapter;
import com.example.spottermobile.database.FirestoreHelper;
import com.example.spottermobile.model.Booking;
import com.example.spottermobile.notifications.NotificationHelper;

import java.util.ArrayList;
import java.util.List;
import android.content.Context;


public class BookingHistoryActivity extends AppCompatActivity {

    // ── Filter definitions ─────────────────────────────────────────────────────

    private static final String[] FILTER_LABELS  = {
            "All", "Upcoming", "Checked In", "Completed", "No Show", "Cancelled", "Waitlisted"
    };
    private static final String[] FILTER_STATUSES = {
            null, "confirmed", "checked_in", "completed", "no_show", "cancelled", "waitlisted"
    };

    // ── Views & state ──────────────────────────────────────────────────────────

    private RecyclerView    recyclerBookings;
    private LinearLayout    layoutEmpty;
    private TextView        tvEmpty;
    private LinearLayout    chipBar;

    private FirestoreHelper firestoreHelper;
    private String          userId;     // Firestore document ID (String)

    private List<Booking>   allBookings = new ArrayList<>();
    private BookingAdapter  adapter;
    private int             activeChip  = 0;

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_history);
        setupBottomNav("mybookings");

        recyclerBookings = findViewById(R.id.recyclerBookings);
        layoutEmpty      = findViewById(R.id.layoutEmpty);
        tvEmpty          = findViewById(R.id.tvEmpty);
        chipBar          = findViewById(R.id.chipBar);

        firestoreHelper = new FirestoreHelper();

        SharedPreferences prefs = getSharedPreferences("SpotterPrefs", MODE_PRIVATE);
        userId = prefs.getString("user_id", null);

        recyclerBookings.setLayoutManager(new LinearLayoutManager(this));

        buildChips();
        loadBookings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBookings();
    }

    // ── Bottom nav ─────────────────────────────────────────────────────────────

    private void setupBottomNav(String activeTab) {
        LinearLayout tabBook       = findViewById(R.id.tabBook);
        LinearLayout tabMyBookings = findViewById(R.id.tabMyBookings);
        LinearLayout tabWorkouts   = findViewById(R.id.tabWorkouts);
        LinearLayout tabBmi        = findViewById(R.id.tabBmi);
        LinearLayout tabProfile    = findViewById(R.id.tabProfile);

        highlightTab(tabBook,       "book".equals(activeTab));
        highlightTab(tabMyBookings, "mybookings".equals(activeTab));
        highlightTab(tabWorkouts,   "workouts".equals(activeTab));
        highlightTab(tabBmi,        "bmi".equals(activeTab));
        highlightTab(tabProfile,    "profile".equals(activeTab));

        tabBook.setOnClickListener(v       -> navigateToTab(activeTab, "book",       BookingActivity.class));
        tabMyBookings.setOnClickListener(v -> navigateToTab(activeTab, "mybookings", BookingHistoryActivity.class));
        tabWorkouts.setOnClickListener(v   -> navigateToTab(activeTab, "workouts",   WorkoutHistoryActivity.class));
        tabBmi.setOnClickListener(v        -> navigateToTab(activeTab, "bmi",        BMIActivity.class));
        tabProfile.setOnClickListener(v    -> navigateToTab(activeTab, "profile",    ProfileActivity.class));
    }

    private void navigateToTab(String activeTab, String targetTab, Class<?> activityClass) {
        if (targetTab.equals(activeTab)) return;
        startActivity(new Intent(this, activityClass));
        finish();
    }

    private void highlightTab(LinearLayout tab, boolean active) {
        int color = Color.parseColor(active ? "#FFFFFF" : "#6B7280");
        for (int i = 0; i < tab.getChildCount(); i++) {
            View child = tab.getChildAt(i);
            if (child instanceof TextView) {
                ((TextView) child).setTextColor(color);
            } else if (child instanceof ImageView) {
                ((ImageView) child).setColorFilter(color);
            }
        }
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
        if (userId == null) {
            showEmpty("Unable to load bookings. Please log in again.");
            return;
        }

        firestoreHelper.getUserBookings(userId, new FirestoreHelper.FirestoreCallback<List<Booking>>() {
            @Override
            public void onSuccess(List<Booking> result) {
                allBookings = result;
                applyFilter();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(BookingHistoryActivity.this,
                        "Failed to load bookings: " + errorMessage,
                        Toast.LENGTH_SHORT).show();
                showEmpty("Could not load bookings.");
            }
        });
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
            String label = FILTER_LABELS[activeChip];
            showEmpty(activeChip == 0
                    ? "No bookings yet."
                    : "No " + label.toLowerCase() + " bookings.");
            return;
        }

        layoutEmpty.setVisibility(View.GONE);
        recyclerBookings.setVisibility(View.VISIBLE);

        adapter = new BookingAdapter(this, filtered, (booking, position) -> {
            String status = booking.getStatus();
            switch (status) {
                case "confirmed":
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

    private void showEmpty(String message) {
        recyclerBookings.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText(message);
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
        // FIX: use getId() — this is the Firestore document ID set by docToBooking().
        // getPaymentReference() is the GCash/Maya transaction ref, not the doc ID.
        String bookingId = booking.getId();
        if (bookingId == null || bookingId.isEmpty()) {
            Toast.makeText(this, "Cancel failed: booking ID not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        firestoreHelper.cancelBooking(bookingId, (Context) this, new FirestoreHelper.FirestoreCallback<Void>()  {
            @Override
            public void onSuccess(Void result) {
                NotificationHelper.notifyCancelled(BookingHistoryActivity.this,
                        booking.getSelectedDate(), booking.getTimeSlot());

                Toast.makeText(BookingHistoryActivity.this,
                        "Booking cancelled.", Toast.LENGTH_SHORT).show();
                loadBookings();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(BookingHistoryActivity.this,
                        "Cancel failed: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}