package com.example.spottermobile.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spottermobile.R;
import com.example.spottermobile.database.DatabaseHelper;
import com.example.spottermobile.model.User;

public class RegisterActivity extends AppCompatActivity {
    private EditText etUsername, etFullName, etEmail, etContact, etAddress,
            etEmergencyName, etEmergencyContact, etPassword;
    private RadioGroup rgGender;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        dbHelper = new DatabaseHelper(this);

        Button btnRegister = findViewById(R.id.btnRegister);
        btnRegister.setOnClickListener(v -> attemptRegister());
    }

    private void initViews() {
        etUsername = findViewById(R.id.etUsername);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etContact = findViewById(R.id.etContact);
        etAddress = findViewById(R.id.etAddress);
        etEmergencyName = findViewById(R.id.etEmergencyName);
        etEmergencyContact = findViewById(R.id.etEmergencyContact);
        etPassword = findViewById(R.id.etPassword);
        rgGender = findViewById(R.id.rgGender);

        etPassword.setInputType(145);
    }

    private void attemptRegister() {
        String username = etUsername.getText().toString().trim();
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String contact = etContact.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String emergencyName = etEmergencyName.getText().toString().trim();
        String emergencyContact = etEmergencyContact.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        int selectedGenderId = rgGender.getCheckedRadioButtonId();
        String gender = selectedGenderId == R.id.rbMale ? "Male" : "Female";

        if (!validateInputs(username, fullName, email, contact, address,
                emergencyName, emergencyContact, password, gender)) {
            return;
        }

        // FIX: Check username and email separately with correct method
        if (dbHelper.isUserExists(username)) {
            Toast.makeText(this, "❌ Username already exists", Toast.LENGTH_SHORT).show();
            return;
        }
        if (dbHelper.isUserExists(email)) {
            Toast.makeText(this, "❌ Email already registered", Toast.LENGTH_SHORT).show();
            return;
        }

        User user = new User(username, fullName, email, gender, contact,
                address, emergencyName, emergencyContact, password);

        // FIX: registerUser() returns boolean, not long
        boolean success = dbHelper.registerUser(user);
        if (success) {
            Toast.makeText(this, "✅ Registration Successful!\nPlease login.",
                    Toast.LENGTH_LONG).show();
            finish();
        } else {
            Toast.makeText(this, "❌ Registration failed", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateInputs(String username, String fullName, String email,
                                   String contact, String address, String emergencyName,
                                   String emergencyContact, String password, String gender) {
        if (username.length() < 3) {
            Toast.makeText(this, "Username must be 3+ characters", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (fullName.length() < 2) {
            Toast.makeText(this, "Full name required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Valid email required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (contact.length() != 10) {
            Toast.makeText(this, "Contact number must be 10 digits", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (address.length() < 10) {
            Toast.makeText(this, "Address too short", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (emergencyName.length() < 2) {
            Toast.makeText(this, "Emergency contact name required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (emergencyContact.length() != 10) {
            Toast.makeText(this, "Emergency contact must be 10 digits", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be 6+ characters", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}