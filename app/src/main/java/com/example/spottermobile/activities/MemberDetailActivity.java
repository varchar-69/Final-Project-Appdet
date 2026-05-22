package com.example.spottermobile.activities;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.spottermobile.R;
import com.example.spottermobile.database.FirestoreHelper;
import com.example.spottermobile.model.Booking;
import com.example.spottermobile.model.User;

import java.util.List;

public class MemberDetailActivity extends AppCompatActivity {

    /**
     * Pass the Firestore document ID (String) of the user to view.
     * AdminUsersActivity already passes user.getFirestoreId() here.
     */
    public static final String EXTRA_USER_ID = "extra_user_id";

    private FirestoreHelper firestoreHelper;
    private User            member;

    // Header
    private TextView tvDetailInitials;
    private TextView tvDetailFullName;
    private TextView tvDetailUsername;
    private TextView tvDetailSuspendedBadge;

    // Stats
    private TextView tvStatTotal;
    private TextView tvStatCompleted;
    private TextView tvStatNoShow;

    // Info
    private TextView tvDetailEmail;
    private TextView tvDetailGender;
    private TextView tvDetailContact;
    private TextView tvDetailAddress;
    private TextView tvDetailEmergency;
    private TextView tvDetailJoined;

    // Actions
    private Button btnSuspendToggle;

    // Booking history container
    private LinearLayout layoutBookingHistory;
    private TextView     tvNoBookings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_detail);

        firestoreHelper = new FirestoreHelper();

        // Firestore user IDs are Strings
        String userId = getIntent().getStringExtra(EXTRA_USER_ID);
        if (userId == null || userId.isEmpty()) { finish(); return; }

        bindViews();
        findViewById(R.id.tvBack).setOnClickListener(v -> finish());

        // Load user from Firestore then populate the whole screen
        loadMember(userId);
    }

    // ── BIND ───────────────────────────────────────────────────────────────────

    private void bindViews() {
        tvDetailInitials       = findViewById(R.id.tvDetailInitials);
        tvDetailFullName       = findViewById(R.id.tvDetailFullName);
        tvDetailUsername       = findViewById(R.id.tvDetailUsername);
        tvDetailSuspendedBadge = findViewById(R.id.tvDetailSuspendedBadge);

        tvStatTotal     = findViewById(R.id.tvStatTotal);
        tvStatCompleted = findViewById(R.id.tvStatCompleted);
        tvStatNoShow    = findViewById(R.id.tvStatNoShow);

        tvDetailEmail     = findViewById(R.id.tvDetailEmail);
        tvDetailGender    = findViewById(R.id.tvDetailGender);
        tvDetailContact   = findViewById(R.id.tvDetailContact);
        tvDetailAddress   = findViewById(R.id.tvDetailAddress);
        tvDetailEmergency = findViewById(R.id.tvDetailEmergency);
        tvDetailJoined    = findViewById(R.id.tvDetailJoined);

        btnSuspendToggle    = findViewById(R.id.btnSuspendToggle);
        layoutBookingHistory = findViewById(R.id.layoutBookingHistory);
        tvNoBookings         = findViewById(R.id.tvNoBookings);
    }

    // ── LOAD ───────────────────────────────────────────────────────────────────

    private void loadMember(String userId) {
        firestoreHelper.getUserById(userId, new FirestoreHelper.FirestoreCallback<User>() {
            @Override
            public void onSuccess(User user) {
                member = user;
                runOnUiThread(() -> {
                    populateHeader();
                    populateInfo();
                    setupSuspendButton();
                    loadBookingsForMember(userId);
                });
            }
            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> finish());
            }
        });
    }

    private void loadBookingsForMember(String userId) {
        firestoreHelper.getUserBookings(userId, new FirestoreHelper.FirestoreCallback<List<Booking>>() {
            @Override
            public void onSuccess(List<Booking> bookings) {
                runOnUiThread(() -> {
                    populateStats(bookings);
                    populateBookingHistory(bookings);
                });
            }
            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    tvStatTotal.setText("—");
                    tvStatCompleted.setText("—");
                    tvStatNoShow.setText("—");
                    tvNoBookings.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    // ── POPULATE ───────────────────────────────────────────────────────────────

    private void populateHeader() {
        tvDetailInitials.setText(getInitials(member.getFullName()));
        tvDetailFullName.setText(member.getFullName());
        tvDetailUsername.setText("@" + member.getUsername());

        if (member.isSuspended()) {
            tvDetailSuspendedBadge.setVisibility(View.VISIBLE);
            tvDetailInitials.setBackgroundColor(Color.parseColor("#EF4444"));
        } else {
            tvDetailSuspendedBadge.setVisibility(View.GONE);
            tvDetailInitials.setBackgroundColor(Color.parseColor("#1E3A8A"));
        }
    }

    private void populateStats(List<Booking> bookings) {
        int total     = bookings.size();
        int completed = 0;
        int noShows   = 0;

        for (Booking b : bookings) {
            if ("completed".equals(b.getStatus())) completed++;
            if ("no_show".equals(b.getStatus()))   noShows++;
        }

        tvStatTotal.setText(String.valueOf(total));
        tvStatCompleted.setText(String.valueOf(completed));
        tvStatNoShow.setText(String.valueOf(noShows));
    }

    private void populateInfo() {
        tvDetailEmail.setText("Email:  "     + safe(member.getEmail()));
        tvDetailGender.setText("Gender:  "   + safe(member.getGender()));
        tvDetailContact.setText("Contact:  " + safe(member.getContactNumber()));
        tvDetailAddress.setText("Address:  " + safe(member.getAddress()));
        tvDetailEmergency.setText("Emergency:  " + safe(member.getEmergencyContactName())
                + "  ·  " + safe(member.getEmergencyContactNumber()));
        tvDetailJoined.setText("Joined: " + safe(member.getCreatedDate()));
    }

    private void populateBookingHistory(List<Booking> bookings) {
        layoutBookingHistory.removeAllViews();

        if (bookings.isEmpty()) {
            tvNoBookings.setVisibility(View.VISIBLE);
            return;
        }

        tvNoBookings.setVisibility(View.GONE);

        for (Booking b : bookings) {
            CardView card = new CardView(this);
            CardView.LayoutParams cardParams = new CardView.LayoutParams(
                    CardView.LayoutParams.MATCH_PARENT,
                    CardView.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, 10);
            card.setLayoutParams(cardParams);
            card.setRadius(12f);
            card.setCardElevation(2f);
            card.setCardBackgroundColor(Color.WHITE);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(40, 28, 40, 28);

            // Top row: date + status badge
            LinearLayout topRow = new LinearLayout(this);
            topRow.setOrientation(LinearLayout.HORIZONTAL);

            TextView tvDate = new TextView(this);
            // Firestore bookings store "date" and "timeSlot"
            String dateStr = b.getSelectedDate() != null ? b.getSelectedDate() : b.getBookingDate();
            tvDate.setText("📅 " + safe(dateStr) + "   🕐 " + safe(b.getTimeSlot()));
            tvDate.setTextSize(13f);
            tvDate.setTextColor(Color.parseColor("#334155"));
            tvDate.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvStatus = new TextView(this);
            tvStatus.setText(statusLabel(b.getStatus()));
            tvStatus.setTextSize(10f);
            tvStatus.setTypeface(null, Typeface.BOLD);
            tvStatus.setPadding(20, 8, 20, 8);
            tvStatus.setTextColor(statusTextColor(b.getStatus()));
            tvStatus.setBackgroundColor(statusBgColor(b.getStatus()));

            topRow.addView(tvDate);
            topRow.addView(tvStatus);

            // Gym / workout info (stored in gymName in Firestore)
            TextView tvWorkout = new TextView(this);
            tvWorkout.setText(safe(b.getWorkoutType())); // workoutType reused for gymName
            tvWorkout.setTextSize(12f);
            tvWorkout.setTextColor(Color.parseColor("#64748B"));
            tvWorkout.setPadding(0, 6, 0, 0);

            row.addView(topRow);
            row.addView(tvWorkout);
            card.addView(row);
            layoutBookingHistory.addView(card);
        }
    }

    // ── SUSPEND BUTTON ─────────────────────────────────────────────────────────

    private void setupSuspendButton() {
        updateSuspendButton();

        btnSuspendToggle.setOnClickListener(v -> {
            boolean currentlySuspended = member.isSuspended();
            String action  = currentlySuspended ? "unsuspend" : "suspend";
            String message = currentlySuspended
                    ? "Reinstate " + member.getFullName() + "? They will be able to log in again."
                    : "Suspend " + member.getFullName() + "? They will be blocked from logging in.";

            new AlertDialog.Builder(this)
                    .setTitle(currentlySuspended ? "Unsuspend Member" : "Suspend Member")
                    .setMessage(message)
                    .setPositiveButton(action.toUpperCase(), (dialog, which) -> {
                        toggleSuspension(currentlySuspended);
                    })
                    .setNegativeButton("CANCEL", null)
                    .show();
        });
    }

    private void toggleSuspension(boolean currentlySuspended) {
        String userId = member.getFirestoreId(); // Firestore document ID

        FirestoreHelper.FirestoreCallback<Void> callback = new FirestoreHelper.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                member.setSuspended(!currentlySuspended);
                runOnUiThread(() -> {
                    updateSuspendButton();
                    populateHeader(); // refresh suspended badge
                    Toast.makeText(MemberDetailActivity.this,
                            member.getFullName() + " has been "
                                    + (member.isSuspended() ? "suspended." : "reinstated."),
                            Toast.LENGTH_SHORT).show();
                });
            }
            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() ->
                        Toast.makeText(MemberDetailActivity.this,
                                "Action failed. Please try again.",
                                Toast.LENGTH_SHORT).show());
            }
        };

        if (currentlySuspended) {
            // Unsuspend: set isSuspended = false
            firestoreHelper.unsuspendUser(userId, callback);
        } else {
            // Suspend: set isSuspended = true
            firestoreHelper.suspendUser(userId, callback);
        }
    }

    private void updateSuspendButton() {
        if (member.isSuspended()) {
            btnSuspendToggle.setText("UNSUSPEND MEMBER");
            btnSuspendToggle.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#10B981")));
        } else {
            btnSuspendToggle.setText("SUSPEND MEMBER");
            btnSuspendToggle.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#EF4444")));
        }
    }

    // ── HELPERS ────────────────────────────────────────────────────────────────

    private String safe(String val) {
        return (val == null || val.trim().isEmpty()) ? "—" : val;
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private String statusLabel(String status) {
        if (status == null) return "UNKNOWN";
        switch (status) {
            case "confirmed":   return "CONFIRMED";
            case "checked_in":  return "CHECKED IN";
            case "completed":   return "COMPLETED";
            case "cancelled":   return "CANCELLED";
            case "no_show":     return "NO SHOW";
            case "waitlisted":  return "WAITLISTED";
            default:            return status.toUpperCase();
        }
    }

    private int statusTextColor(String status) {
        if (status == null) return Color.GRAY;
        switch (status) {
            case "confirmed":   return Color.parseColor("#1E3A8A");
            case "checked_in":  return Color.parseColor("#D97706");
            case "completed":   return Color.parseColor("#10B981");
            case "cancelled":   return Color.parseColor("#64748B");
            case "no_show":     return Color.parseColor("#EF4444");
            case "waitlisted":  return Color.parseColor("#7C3AED");
            default:            return Color.DKGRAY;
        }
    }

    private int statusBgColor(String status) {
        if (status == null) return Color.LTGRAY;
        switch (status) {
            case "confirmed":   return Color.parseColor("#DBEAFE");
            case "checked_in":  return Color.parseColor("#FEF3C7");
            case "completed":   return Color.parseColor("#D1FAE5");
            case "cancelled":   return Color.parseColor("#F1F5F9");
            case "no_show":     return Color.parseColor("#FEE2E2");
            case "waitlisted":  return Color.parseColor("#EDE9FE");
            default:            return Color.parseColor("#F1F5F9");
        }
    }
}