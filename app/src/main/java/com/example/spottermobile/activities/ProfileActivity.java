package com.example.spottermobile.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
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

        dbHelper = new DatabaseHelper(this);
        sharedPreferences = getSharedPreferences("SpotterPrefs", MODE_PRIVATE);
        userId = sharedPreferences.getInt("user_id", -1);

        initViews();
        loadProfile();
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