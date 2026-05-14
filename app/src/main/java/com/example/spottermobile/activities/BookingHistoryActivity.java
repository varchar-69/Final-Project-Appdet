package com.example.spottermobile.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spottermobile.R;
import com.example.spottermobile.adapters.BookingAdapter;
import com.example.spottermobile.database.DatabaseHelper;
import com.example.spottermobile.model.Booking;

import java.util.List;

public class BookingHistoryActivity extends AppCompatActivity {

    private RecyclerView   recyclerBookings;
    private LinearLayout   layoutEmpty;
    private DatabaseHelper dbHelper;
    private int            userId;
    private List<Booking>  bookings;
    private BookingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_history);

        recyclerBookings = findViewById(R.id.recyclerBookings);
        layoutEmpty      = findViewById(R.id.layoutEmpty);

        dbHelper = new DatabaseHelper(this);
        SharedPreferences prefs = getSharedPreferences("SpotterPrefs", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        recyclerBookings.setLayoutManager(new LinearLayoutManager(this));
        loadBookings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBookings(); // refresh when returning from another screen
    }

    private void loadBookings() {
        bookings = dbHelper.getUserBookings(userId);

        if (bookings.isEmpty()) {
            recyclerBookings.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
            return;
        }

        layoutEmpty.setVisibility(View.GONE);
        recyclerBookings.setVisibility(View.VISIBLE);

        adapter = new BookingAdapter(this, bookings, (booking, position) -> {
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

    // ── CANCEL ─────────────────────────────────────────────────────────────────

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

        boolean success;
        if (isWaitlisted) {
            // Remove from waitlist table directly
            success = dbHelper.removeFromWaitlist(booking.getId(), userId);
        } else {
            success = dbHelper.cancelBooking(booking.getId(), userId);
        }

        if (success) {
            Toast.makeText(this, "Booking cancelled.", Toast.LENGTH_SHORT).show();
            loadBookings();
        } else {
            Toast.makeText(this, "Cancel failed. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }
}