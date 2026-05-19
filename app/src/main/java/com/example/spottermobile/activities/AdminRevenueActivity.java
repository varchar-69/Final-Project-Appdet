package com.example.spottermobile.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spottermobile.R;
import com.example.spottermobile.database.DatabaseHelper;
import com.example.spottermobile.model.Booking;

import java.util.List;
import java.util.Locale;

public class AdminRevenueActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private TextView tvTotalRevenue;
    private TextView tvPaidCount;
    private TextView tvEmpty;
    private LinearLayout tableBody;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_revenue);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        dbHelper = new DatabaseHelper(this);
        initViews();
        loadRevenueData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRevenueData();
    }

    private void initViews() {
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvPaidCount = findViewById(R.id.tvPaidCount);
        tvEmpty = findViewById(R.id.tvEmpty);
        tableBody = findViewById(R.id.tableBody);
    }

    private void loadRevenueData() {
        int total = dbHelper.getTotalRevenue();
        tvTotalRevenue.setText(String.format(Locale.getDefault(), "Total Revenue: \u20B1%d", total));

        List<Booking> paid = dbHelper.getPaidBookingsWithNames();
        tvPaidCount.setText(String.format(Locale.getDefault(), "Paid Sessions: %d", paid.size()));

        if (paid.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            tableBody.setVisibility(View.GONE);
            return;
        }

        tvEmpty.setVisibility(View.GONE);
        tableBody.setVisibility(View.VISIBLE);
        tableBody.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);
        for (Booking booking : paid) {
            View rowView = inflater.inflate(R.layout.item_revenue_row, tableBody, false);

            TextView tvRowName = rowView.findViewById(R.id.tvRowName);
            TextView tvRowDate = rowView.findViewById(R.id.tvRowDate);
            TextView tvRowMethod = rowView.findViewById(R.id.tvRowMethod);
            TextView tvRowRef = rowView.findViewById(R.id.tvRowRef);
            TextView tvRowAmount = rowView.findViewById(R.id.tvRowAmount);

            tvRowName.setText(booking.getMemberName());
            tvRowDate.setText(booking.getSelectedDate());
            tvRowMethod.setText(booking.getPaymentMethod());
            tvRowRef.setText(booking.getPaymentReference());
            tvRowAmount.setText(String.format(Locale.getDefault(), "\u20B1%d", DatabaseHelper.SESSION_PRICE));

            if ("GCash".equalsIgnoreCase(booking.getPaymentMethod())) {
                tvRowMethod.setTextColor(android.graphics.Color.parseColor("#007DFE"));
            } else if ("Maya".equalsIgnoreCase(booking.getPaymentMethod())) {
                tvRowMethod.setTextColor(android.graphics.Color.parseColor("#019F3C"));
            } else {
                tvRowMethod.setTextColor(getResources().getColor(R.color.dark_gray, null));
            }

            tableBody.addView(rowView);
        }
    }
}
