package com.example.spottermobile.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spottermobile.R;
import com.example.spottermobile.database.DatabaseHelper;
import com.example.spottermobile.model.Booking;

import java.util.ArrayList;
import java.util.List;

public class BookingHistoryActivity extends AppCompatActivity {
    private ListView listViewBookings;
    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPreferences;
    private int userId;
    private List<Booking> bookings;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_history);

        listViewBookings = findViewById(R.id.listViewBookings);
        dbHelper = new DatabaseHelper(this);
        sharedPreferences = getSharedPreferences("SpotterPrefs", MODE_PRIVATE);
        userId = sharedPreferences.getInt("user_id", -1);

        loadBookings();
        setupListClick();
    }

    private void loadBookings() {
        bookings = dbHelper.getUserBookings(userId);
        List<String> displayList = new ArrayList<>();

        if (bookings.isEmpty()) {
            displayList.add("No bookings yet");
        } else {
            for (Booking booking : bookings) {
                String status = booking.getStatus().equals("booked") ? "✅ Booked" : "❌ Cancelled";
                displayList.add(booking.getBookingDate() + " | " + booking.getTimeSlot() + " | " + status);
            }
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayList);
        listViewBookings.setAdapter(adapter);
    }

    private void setupListClick() {
        listViewBookings.setOnItemClickListener((parent, view, position, id) -> {
            Booking booking = bookings.get(position);
            if ("booked".equals(booking.getStatus())) {
                cancelBooking(booking.getId());
            }
        });
    }

    private void cancelBooking(int bookingId) {
        if (dbHelper.cancelBooking(bookingId, userId)) {
            Toast.makeText(this, "✅ Booking cancelled successfully", Toast.LENGTH_SHORT).show();
            loadBookings(); // Refresh
        } else {
            Toast.makeText(this, "❌ Cancel failed", Toast.LENGTH_SHORT).show();
        }
    }
}