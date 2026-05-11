package com.example.spottermobile.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import androidx.appcompat.app.AppCompatActivity;

import com.example.spottermobile.R;
import com.example.spottermobile.database.DatabaseHelper;
import com.example.spottermobile.model.Booking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BookingHistoryActivity extends AppCompatActivity {
    private ListView listViewBookings;
    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPreferences;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_history);

        listViewBookings = findViewById(R.id.listViewBookings);
        dbHelper = new DatabaseHelper(this);
        sharedPreferences = getSharedPreferences("SpotterPrefs", MODE_PRIVATE);
        userId = sharedPreferences.getInt("user_id", 1);

        loadBookings();
    }

    private void loadBookings() {
        List<Booking> bookings = dbHelper.getUserBookings(userId);

        List<HashMap<String, String>> data = new ArrayList<>();
        for (Booking booking : bookings) {
            HashMap<String, String> map = new HashMap<>();
            map.put("workout", booking.getWorkoutType());
            map.put("time", booking.getTimeSlot());
            map.put("branch", booking.getGymBranch());
            map.put("date", booking.getBookingDate());
            map.put("status", booking.getStatus());
            data.add(map);
        }

        SimpleAdapter adapter = new SimpleAdapter(
                this, data,
                R.layout.booking_list_item,
                new String[]{"workout", "time", "branch", "date", "status"},
                new int[]{R.id.tvWorkout, R.id.tvTime, R.id.tvBranch, R.id.tvDate, R.id.tvStatus}
        );
        listViewBookings.setAdapter(adapter);
    }
}