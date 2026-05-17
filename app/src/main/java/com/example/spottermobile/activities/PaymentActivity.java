package com.example.spottermobile.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.spottermobile.R;
import com.example.spottermobile.database.DatabaseHelper;

import java.util.Locale;
import java.util.Random;

public class PaymentActivity extends AppCompatActivity {

    private TextView tvPayWorkout;
    private TextView tvPayDate;
    private TextView tvPayTimeSlot;
    private TextView tvPayMember;
    private TextView tvPayAmount;
    private Button btnGCash;
    private Button btnMaya;
    private boolean paymentInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        bindViews();
        populateSummary();

        findViewById(R.id.btnPayBack).setOnClickListener(v -> {
            if (!paymentInProgress) finish();
        });

        btnGCash.setOnClickListener(v -> processMockPayment("GCash"));
        btnMaya.setOnClickListener(v -> processMockPayment("Maya"));
    }

    private void bindViews() {
        tvPayWorkout = findViewById(R.id.tvPayWorkout);
        tvPayDate = findViewById(R.id.tvPayDate);
        tvPayTimeSlot = findViewById(R.id.tvPayTimeSlot);
        tvPayMember = findViewById(R.id.tvPayMember);
        tvPayAmount = findViewById(R.id.tvPayAmount);
        btnGCash = findViewById(R.id.btnGCash);
        btnMaya = findViewById(R.id.btnMaya);
    }

    private void populateSummary() {
        Intent intent = getIntent();
        tvPayWorkout.setText(intent.getStringExtra("workout_type"));
        tvPayDate.setText(intent.getStringExtra("selected_date"));
        tvPayTimeSlot.setText(intent.getStringExtra("time_slot"));
        tvPayMember.setText(intent.getStringExtra("member_name"));
        tvPayAmount.setText(String.format(Locale.getDefault(), "\u20B1%d.00", DatabaseHelper.SESSION_PRICE));
    }

    private void processMockPayment(String method) {
        paymentInProgress = true;
        setPaymentButtonsEnabled(false);

        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setTitle("Processing Payment");
        dialog.setMessage("Connecting to " + method + "...");
        dialog.setCancelable(false);
        dialog.show();

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            dialog.dismiss();

            Random random = new Random();
            PaymentResult result;
            if (random.nextInt(100) < 90) {
                String ref = "SPT-" + String.format(Locale.getDefault(), "%06d", random.nextInt(999999));
                result = new PaymentResult(true, ref, method, null);
            } else {
                result = new PaymentResult(false, null, method,
                        "Payment could not be processed. Please try again.");
            }

            if (result.isSuccess()) {
                Intent intent = new Intent();
                intent.putExtra("payment_status", "paid");
                intent.putExtra("payment_reference", result.getReferenceNumber());
                intent.putExtra("payment_method", result.getPaymentMethod());
                setResult(RESULT_OK, intent);
                finish();
                return;
            }

            paymentInProgress = false;
            setPaymentButtonsEnabled(true);

            new AlertDialog.Builder(this)
                    .setTitle("Payment Failed")
                    .setMessage(result.getErrorMessage())
                    .setPositiveButton("Retry", (d, which) -> d.dismiss())
                    .setNegativeButton("Cancel", (d, which) -> finish())
                    .setCancelable(false)
                    .show();
        }, 2000);
    }

    private void setPaymentButtonsEnabled(boolean enabled) {
        btnGCash.setEnabled(enabled);
        btnMaya.setEnabled(enabled);
    }

    @Override
    public void onBackPressed() {
        if (!paymentInProgress) {
            super.onBackPressed();
        }
    }
}
