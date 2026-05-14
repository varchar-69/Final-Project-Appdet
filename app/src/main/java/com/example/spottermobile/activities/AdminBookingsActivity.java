package com.example.spottermobile.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spottermobile.R;
import com.example.spottermobile.adapters.AdminBookingAdapter;
import com.example.spottermobile.database.DatabaseHelper;
import com.example.spottermobile.model.Booking;

import java.util.List;

public class AdminBookingsActivity extends AppCompatActivity {

    private RecyclerView        recyclerBookings;
    private TextView            tvEmpty;
    private DatabaseHelper      dbHelper;
    private AdminBookingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_bookings);

        recyclerBookings = findViewById(R.id.recyclerAdminBookings);
        tvEmpty          = findViewById(R.id.tvAdminEmpty);

        Button btnCheckIn  = findViewById(R.id.btnScanCheckIn);
        Button btnCheckOut = findViewById(R.id.btnScanCheckOut);

        dbHelper = new DatabaseHelper(this);
        recyclerBookings.setLayoutManager(new LinearLayoutManager(this));

        btnCheckIn.setOnClickListener(v  -> openScanner(QRScanActivity.MODE_CHECKIN));
        btnCheckOut.setOnClickListener(v -> openScanner(QRScanActivity.MODE_CHECKOUT));

        loadBookings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBookings(); // refresh after returning from scanner
    }

    private void loadBookings() {
        // Use JOIN query so each booking carries the member's name
        List<Booking> bookings = dbHelper.getAllBookingsWithNames();

        if (bookings.isEmpty()) {
            recyclerBookings.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }

        tvEmpty.setVisibility(View.GONE);
        recyclerBookings.setVisibility(View.VISIBLE);
        adapter = new AdminBookingAdapter(this, bookings);
        recyclerBookings.setAdapter(adapter);
    }

    private void openScanner(String mode) {
        Intent intent = new Intent(this, QRScanActivity.class);
        intent.putExtra(QRScanActivity.EXTRA_SCAN_MODE, mode);
        startActivity(intent);
    }
}