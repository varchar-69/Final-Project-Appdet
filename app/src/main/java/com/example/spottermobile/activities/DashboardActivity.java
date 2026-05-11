package com.example.spottermobile.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.spottermobile.R;

public class DashboardActivity extends AppCompatActivity {
    private SharedPreferences sharedPreferences;
    private TextView tvWelcome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        sharedPreferences = getSharedPreferences("SpotterPrefs", MODE_PRIVATE);
        tvWelcome = findViewById(R.id.tvWelcome);

        // Check if logged in
        if (!sharedPreferences.getBoolean("isLoggedIn", false)) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String username = sharedPreferences.getString("username", "User");
        String role = sharedPreferences.getString("role", "member");
        tvWelcome.setText("Welcome, " + username + " (" + role + ")");

        setupButtons();
    }

    private void setupButtons() {
        Button btnBookSession = findViewById(R.id.btnBookSession);
        Button btnBookingHistory = findViewById(R.id.btnBookingHistory);
        Button btnBMI = findViewById(R.id.btnBMI);
        Button btnWorkouts = findViewById(R.id.btnWorkouts);
        Button btnFeedback = findViewById(R.id.btnFeedback);
        Button btnLogout = findViewById(R.id.btnLogout);

        btnBookSession.setOnClickListener(v -> startActivity(new Intent(this, BookingActivity.class)));
        btnBookingHistory.setOnClickListener(v -> startActivity(new Intent(this, BookingHistoryActivity.class)));
        btnBMI.setOnClickListener(v -> startActivity(new Intent(this, BMIActivity.class)));
        btnWorkouts.setOnClickListener(v -> startActivity(new Intent(this, WorkoutListActivity.class)));
        btnFeedback.setOnClickListener(v -> startActivity(new Intent(this, FeedbackActivity.class)));
        btnLogout.setOnClickListener(v -> logout());
    }

    private void logout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        finish();
    }
}