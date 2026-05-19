package com.example.spottermobile.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spottermobile.R;
import com.example.spottermobile.database.DatabaseHelper;
import com.example.spottermobile.model.User;

public class ProfileActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPreferences;
    private int userId;

    // All profile fields
    private TextView tvUsername, tvFullName, tvEmail, tvGender, tvContact,
            tvAddress, tvEmergencyName, tvEmergencyContact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        setupBottomNav("profile");

        dbHelper = new DatabaseHelper(this);
        sharedPreferences = getSharedPreferences("SpotterPrefs", MODE_PRIVATE);
        userId = sharedPreferences.getInt("user_id", -1);

        initViews();
        loadProfile();
    }

    private void setupBottomNav(String activeTab) {
        LinearLayout tabBook = findViewById(R.id.tabBook);
        LinearLayout tabMyBookings = findViewById(R.id.tabMyBookings);
        LinearLayout tabWorkouts = findViewById(R.id.tabWorkouts);
        LinearLayout tabBmi = findViewById(R.id.tabBmi);
        LinearLayout tabProfile = findViewById(R.id.tabProfile);

        highlightTab(tabBook, "book".equals(activeTab));
        highlightTab(tabMyBookings, "mybookings".equals(activeTab));
        highlightTab(tabWorkouts, "workouts".equals(activeTab));
        highlightTab(tabBmi, "bmi".equals(activeTab));
        highlightTab(tabProfile, "profile".equals(activeTab));

        tabBook.setOnClickListener(v -> navigateToTab(activeTab, "book", BookingActivity.class));
        tabMyBookings.setOnClickListener(v -> navigateToTab(activeTab, "mybookings", BookingHistoryActivity.class));
        tabWorkouts.setOnClickListener(v -> navigateToTab(activeTab, "workouts", WorkoutHistoryActivity.class));
        tabBmi.setOnClickListener(v -> navigateToTab(activeTab, "bmi", BMIActivity.class));
        tabProfile.setOnClickListener(v -> navigateToTab(activeTab, "profile", ProfileActivity.class));
    }

    private void navigateToTab(String activeTab, String targetTab, Class<?> activityClass) {
        if (targetTab.equals(activeTab)) {
            return;
        }

        startActivity(new Intent(this, activityClass));
        finish();
    }

    private void highlightTab(LinearLayout tab, boolean active) {
        int color = Color.parseColor(active ? "#FFFFFF" : "#6B7280");

        for (int i = 0; i < tab.getChildCount(); i++) {
            View child = tab.getChildAt(i);
            if (child instanceof TextView) {
                ((TextView) child).setTextColor(color);
            } else if (child instanceof ImageView) {
                ((ImageView) child).setColorFilter(color);
            }
        }
    }

    private void initViews() {
        tvUsername = findViewById(R.id.tvUsername);
        tvFullName = findViewById(R.id.tvFullName);
        tvEmail = findViewById(R.id.tvEmail);
        tvGender = findViewById(R.id.tvGender);
        tvContact = findViewById(R.id.tvContact);
        tvAddress = findViewById(R.id.tvAddress);
        tvEmergencyName = findViewById(R.id.tvEmergencyName);
        tvEmergencyContact = findViewById(R.id.tvEmergencyContact);
    }

    private void loadProfile() {
        User user = dbHelper.getUserById(userId);
        if (user != null) {
            tvUsername.setText(user.getUsername());
            tvFullName.setText(user.getFullName());
            tvEmail.setText(user.getEmail());
            tvGender.setText(user.getGender());
            tvContact.setText(user.getContactNumber());
            tvAddress.setText(user.getAddress());
            tvEmergencyName.setText(user.getEmergencyContactName());
            tvEmergencyContact.setText(user.getEmergencyContactNumber());
        }
    }
}
