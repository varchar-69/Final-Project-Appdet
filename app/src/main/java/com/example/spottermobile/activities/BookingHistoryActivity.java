package com.example.spottermobile.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
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

import java.util.List;

public class BookingHistoryActivity extends AppCompatActivity {

    private RecyclerView    recyclerBookings;
    private TextView        tvEmpty;
    private DatabaseHelper dbHelper;
    private int             userId;
    private List<Booking>   bookings;
    private BookingAdapter  adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_history);

        recyclerBookings = findViewById(R.id.recyclerBookings);
        tvEmpty          = findViewById(R.id.tvEmpty);

        dbHelper = new DatabaseHelper(this);
        SharedPreferences prefs = getSharedPreferences("SpotterPrefs", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        recyclerBookings.setLayoutManager(new LinearLayoutManager(this));

        loadBookings();
    }

    private void loadBookings() {
        bookings = dbHelper.getUserBookings(userId);

        if (bookings.isEmpty()) {
            recyclerBookings.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }

        tvEmpty.setVisibility(View.GONE);
        recyclerBookings.setVisibility(View.VISIBLE);

        adapter = new BookingAdapter(this, bookings, (booking, position) -> {
            String status = booking.getStatus();

            // Only allow cancel on active bookings
            if ("booked".equals(status) || "waitlist".equals(status)) {
                showCancelConfirmDialog(booking);
            } else if ("completed".equals(status)) {
                Toast.makeText(this, "This session is already completed.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "This booking is already cancelled.", Toast.LENGTH_SHORT).show();
            }
        });

        recyclerBookings.setAdapter(adapter);
    }

    // ── CANCEL CONFIRMATION ────────────────────────────────────────────────────

    private void showCancelConfirmDialog(Booking booking) {
        String statusNote = "waitlist".equals(booking.getStatus())
                ? "\n\nThis is a waitlisted booking — cancelling removes you from the queue."
                : "\n\nCancelling a confirmed slot may promote someone from the waitlist.";

        new AlertDialog.Builder(this)
                .setTitle("Cancel Booking?")
                .setMessage(
                        "Are you sure you want to cancel this session?\n\n"
                                + "📋 " + booking.getWorkoutType() + "\n"
                                + "📅 " + booking.getSelectedDate() + "\n"
                                + "🕒 " + booking.getTimeSlot()
                                + statusNote
                )
                .setPositiveButton("Yes, Cancel", (dialog, which) -> cancelBooking(booking))
                .setNegativeButton("Keep It", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void cancelBooking(Booking booking) {
        boolean success = dbHelper.cancelBooking(booking.getId(), userId);
        if (success) {
            Toast.makeText(this, "Booking cancelled.", Toast.LENGTH_SHORT).show();
            loadBookings(); // refresh list
        } else {
            Toast.makeText(this, "Cancel failed. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }
}