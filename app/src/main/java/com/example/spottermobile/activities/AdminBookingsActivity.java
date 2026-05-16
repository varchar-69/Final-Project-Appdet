package com.example.spottermobile.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spottermobile.R;
import com.example.spottermobile.adapters.AdminBookingAdapter;
import com.example.spottermobile.database.DatabaseHelper;
import com.example.spottermobile.model.Booking;

import java.util.Calendar;
import java.util.List;

public class AdminBookingsActivity extends AppCompatActivity {

    // ── Status chip definitions ────────────────────────────────────────────────
    private static final String[] CHIP_LABELS  = {"All", "Booked", "Checked In", "Completed", "No Show", "Cancelled", "Waitlisted"};
    private static final String[] CHIP_VALUES  = {null,
            DatabaseHelper.STATUS_BOOKED,
            DatabaseHelper.STATUS_CHECKED_IN,
            DatabaseHelper.STATUS_COMPLETED,
            DatabaseHelper.STATUS_NO_SHOW,
            DatabaseHelper.STATUS_CANCELLED,
            DatabaseHelper.STATUS_WAITLISTED};

    // Chip colors: [selectedBg, selectedText, unselectedBg, unselectedText]
    private static final int[][] CHIP_COLORS = {
            {0xFF1E3A8A, 0xFFFFFFFF, 0xFFE2E8F0, 0xFF475569}, // All — blue
            {0xFF1E3A8A, 0xFFFFFFFF, 0xFFDBEAFE, 0xFF1E3A8A}, // Booked
            {0xFFD97706, 0xFFFFFFFF, 0xFFFEF3C7, 0xFFD97706}, // Checked In
            {0xFF10B981, 0xFFFFFFFF, 0xFFD1FAE5, 0xFF10B981}, // Completed
            {0xFFEF4444, 0xFFFFFFFF, 0xFFFEE2E2, 0xFFEF4444}, // No Show
            {0xFF64748B, 0xFFFFFFFF, 0xFFF1F5F9, 0xFF64748B}, // Cancelled
            {0xFF7C3AED, 0xFFFFFFFF, 0xFFEDE9FE, 0xFF7C3AED}, // Waitlisted
    };

    // ── State ──────────────────────────────────────────────────────────────────
    private String        activeDate   = null;  // null = all dates
    private int           activeChip   = 0;     // index into CHIP_VALUES; 0 = All

    // ── Views ──────────────────────────────────────────────────────────────────
    private RecyclerView    recyclerBookings;
    private TextView        tvEmpty;
    private TextView        tvSelectedDate;
    private TextView        tvBookingCount;
    private LinearLayout    layoutStatusChips;
    private final TextView[]chipViews = new TextView[CHIP_LABELS.length];

    private DatabaseHelper      dbHelper;
    private AdminBookingAdapter adapter;

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_bookings);

        dbHelper = new DatabaseHelper(this);

        bindViews();
        setupScanButtons();
        setupDatePicker();
        setupStatusChips();
        loadBookings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBookings(); // refresh after returning from scanner
    }

    // ── BIND ───────────────────────────────────────────────────────────────────

    private void bindViews() {
        recyclerBookings = findViewById(R.id.recyclerAdminBookings);
        tvEmpty          = findViewById(R.id.tvAdminEmpty);
        tvSelectedDate   = findViewById(R.id.tvSelectedDate);
        tvBookingCount   = findViewById(R.id.tvBookingCount);
        layoutStatusChips = findViewById(R.id.layoutStatusChips);
        recyclerBookings.setLayoutManager(new LinearLayoutManager(this));
    }

    // ── SCAN BUTTONS ───────────────────────────────────────────────────────────

    private void setupScanButtons() {
        findViewById(R.id.btnScanCheckIn).setOnClickListener(v ->
                openScanner(QRScanActivity.MODE_CHECKIN));
        findViewById(R.id.btnScanCheckOut).setOnClickListener(v ->
                openScanner(QRScanActivity.MODE_CHECKOUT));
    }

    private void openScanner(String mode) {
        Intent intent = new Intent(this, QRScanActivity.class);
        intent.putExtra(QRScanActivity.EXTRA_SCAN_MODE, mode);
        startActivity(intent);
    }

    // ── DATE PICKER ────────────────────────────────────────────────────────────

    private void setupDatePicker() {
        Button btnPick  = findViewById(R.id.btnPickDate);
        Button btnClear = findViewById(R.id.btnClearDate);

        btnPick.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this,
                    (view, year, month, day) -> {
                        activeDate = String.format("%04d-%02d-%02d", year, month + 1, day);
                        tvSelectedDate.setText(activeDate);
                        loadBookings();
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        btnClear.setOnClickListener(v -> {
            activeDate = null;
            tvSelectedDate.setText("All Dates");
            loadBookings();
        });
    }

    // ── STATUS CHIPS ───────────────────────────────────────────────────────────

    private void setupStatusChips() {
        int dp8  = dp(8);
        int dp20 = dp(20);

        for (int i = 0; i < CHIP_LABELS.length; i++) {
            final int idx = i;
            TextView chip = new TextView(this);
            chip.setText(CHIP_LABELS[i]);
            chip.setTextSize(12f);
            chip.setTypeface(null, Typeface.BOLD);
            chip.setPadding(dp20, dp8, dp20, dp8);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, dp(8), 0);
            chip.setLayoutParams(lp);

            chip.setOnClickListener(v -> {
                activeChip = idx;
                refreshChipStyles();
                loadBookings();
            });

            chipViews[i] = chip;
            layoutStatusChips.addView(chip);
        }
        refreshChipStyles();
    }

    private void refreshChipStyles() {
        for (int i = 0; i < chipViews.length; i++) {
            if (chipViews[i] == null) continue;
            boolean selected = (i == activeChip);
            chipViews[i].setBackgroundColor(selected ? CHIP_COLORS[i][0] : CHIP_COLORS[i][2]);
            chipViews[i].setTextColor(     selected ? CHIP_COLORS[i][1] : CHIP_COLORS[i][3]);
        }
    }

    // ── LOAD ───────────────────────────────────────────────────────────────────

    private void loadBookings() {
        String statusFilter = CHIP_VALUES[activeChip]; // null when "All" is selected
        List<Booking> bookings = dbHelper.getFilteredBookingsWithNames(activeDate, statusFilter);

        tvBookingCount.setText(bookings.size() + " booking" + (bookings.size() == 1 ? "" : "s"));

        if (bookings.isEmpty()) {
            recyclerBookings.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText(buildEmptyMessage());
            return;
        }

        tvEmpty.setVisibility(View.GONE);
        recyclerBookings.setVisibility(View.VISIBLE);
        adapter = new AdminBookingAdapter(this, bookings);
        recyclerBookings.setAdapter(adapter);
    }

    private String buildEmptyMessage() {
        if (activeDate != null && activeChip != 0)
            return "No " + CHIP_LABELS[activeChip].toLowerCase() + " bookings on " + activeDate;
        if (activeDate != null)
            return "No bookings on " + activeDate;
        if (activeChip != 0)
            return "No " + CHIP_LABELS[activeChip].toLowerCase() + " bookings";
        return "No bookings in the system yet.";
    }

    // ── UTILS ──────────────────────────────────────────────────────────────────

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
