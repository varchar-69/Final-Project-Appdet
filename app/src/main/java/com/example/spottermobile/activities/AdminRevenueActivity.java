package com.example.spottermobile.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spottermobile.R;
import com.example.spottermobile.database.FirestoreHelper;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminRevenueActivity extends AppCompatActivity {

    private static final int SESSION_PRICE = 200;

    private FirestoreHelper firestoreHelper;

    private TextView tvTotalRevenue;
    private TextView tvPaidCount;
    private TextView tvEmpty;
    private LinearLayout tableBody;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_revenue);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        firestoreHelper = new FirestoreHelper();

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

        firestoreHelper.getPaidBookingsWithNames(
                new FirestoreHelper.FirestoreCallback<List<Map<String, Object>>>() {

                    @Override
                    public void onSuccess(List<Map<String, Object>> paidEntries) {

                        if (paidEntries == null) {
                            paidEntries = java.util.Collections.emptyList();
                        }

                        int count = paidEntries.size();
                        int totalRevenue = count * SESSION_PRICE;

                        tvTotalRevenue.setText(
                                String.format(Locale.getDefault(),
                                        "Total Revenue: ₱%d",
                                        totalRevenue)
                        );

                        tvPaidCount.setText(
                                String.format(Locale.getDefault(),
                                        "Paid Sessions: %d",
                                        count)
                        );

                        if (count == 0) {
                            showEmptyState();
                            return;
                        }

                        tvEmpty.setVisibility(View.GONE);
                        tableBody.setVisibility(View.VISIBLE);
                        tableBody.removeAllViews();

                        LayoutInflater inflater =
                                LayoutInflater.from(AdminRevenueActivity.this);

                        for (Map<String, Object> entry : paidEntries) {

                            View row = inflater.inflate(
                                    R.layout.item_revenue_row,
                                    tableBody,
                                    false
                            );

                            TextView tvRowName = row.findViewById(R.id.tvRowName);
                            TextView tvRowDate = row.findViewById(R.id.tvRowDate);
                            TextView tvRowMethod = row.findViewById(R.id.tvRowMethod);
                            TextView tvRowRef = row.findViewById(R.id.tvRowRef);
                            TextView tvRowAmount = row.findViewById(R.id.tvRowAmount);

                            tvRowName.setText(getString(entry, "userName"));
                            tvRowDate.setText(getString(entry, "date"));
                            tvRowMethod.setText(getString(entry, "paymentMethod"));
                            tvRowRef.setText(getString(entry, "paymentReference"));
                            tvRowAmount.setText(
                                    String.format(Locale.getDefault(),
                                            "₱%d",
                                            SESSION_PRICE)
                            );

                            stylePaymentMethod(tvRowMethod,
                                    getString(entry, "paymentMethod"));

                            tableBody.addView(row);
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        showEmptyState();
                    }
                });
    }

    private void showEmptyState() {
        tvTotalRevenue.setText("Total Revenue: ₱0");
        tvPaidCount.setText("Paid Sessions: 0");

        tvEmpty.setVisibility(View.VISIBLE);
        tableBody.setVisibility(View.GONE);
    }

    private void stylePaymentMethod(TextView view, String method) {

        if ("GCash".equalsIgnoreCase(method)) {
            view.setTextColor(Color.parseColor("#007DFE"));

        } else if ("Maya".equalsIgnoreCase(method)) {
            view.setTextColor(Color.parseColor("#019F3C"));

        } else {
            view.setTextColor(
                    getResources().getColor(
                            R.color.dark_gray,
                            null
                    )
            );
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "—";
    }
}