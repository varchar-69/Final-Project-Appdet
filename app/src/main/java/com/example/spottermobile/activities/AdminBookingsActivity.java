package com.example.spottermobile.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.spottermobile.R;
import com.example.spottermobile.database.DatabaseHelper;
import com.example.spottermobile.model.Booking;

import java.util.ArrayList;
import java.util.List;

public class AdminBookingsActivity extends AppCompatActivity {
    private ListView listViewBookings;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_bookings);

        listViewBookings = findViewById(R.id.listViewBookings);
        dbHelper = new DatabaseHelper(this);

        loadAllBookings();
    }

    private void loadAllBookings() {
        // FIX: Use getAllBookings() instead of getUserBookings() (which required a userId)
        List<Booking> bookings = dbHelper.getAllBookings();
        List<String> bookingList = new ArrayList<>();

        if (bookings.isEmpty()) {
            bookingList.add("No bookings yet");
        } else {
            for (Booking booking : bookings) {
                // FIX: Compare against the string "booked", not an emoji
                String status = "booked".equals(booking.getStatus()) ? "✅ BOOKED" : "❌ CANCELLED";
                // FIX: Also show user ID and workout type for useful admin info
                bookingList.add(
                        "User #" + booking.getUserId() +
                                " | " + booking.getWorkoutType() +
                                " | " + booking.getBookingDate() +
                                " | " + booking.getTimeSlot() +
                                " | " + status
                );
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, bookingList);
        listViewBookings.setAdapter(adapter);
    }
}