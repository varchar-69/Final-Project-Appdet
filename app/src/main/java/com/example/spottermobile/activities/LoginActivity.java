package com.example.spottermobile.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spottermobile.R;
import com.example.spottermobile.database.DatabaseHelper;
import com.example.spottermobile.model.User;

public class LoginActivity extends AppCompatActivity {
    private EditText etIdentifier, etPassword;
    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        dbHelper = new DatabaseHelper(this);
        sharedPreferences = getSharedPreferences("SpotterPrefs", MODE_PRIVATE);

        // Register link
        TextView tvRegister = findViewById(R.id.tvRegister);
        tvRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));

        // Demo admin hint
        TextView tvDemo = findViewById(R.id.tvDemoAdmin);
        tvDemo.setOnClickListener(v -> {
            etIdentifier.setText("admin");
            etPassword.setText("admin123");
        });

        // Login button
        Button btnLogin = findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void initViews() {
        etIdentifier = findViewById(R.id.etIdentifier);
        etPassword = findViewById(R.id.etPassword);
        etPassword.setInputType(145); // Password type
    }

    private void attemptLogin() {
        String identifier = etIdentifier.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(identifier) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 4) {
            Toast.makeText(this, "Password too short", Toast.LENGTH_SHORT).show();
            return;
        }

        // Login attempt (Username OR Email)
        User user = dbHelper.loginUser(identifier, password);
        if (user != null) {
            // Save user data
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("user_id", user.getId());
            editor.putString("username", user.getUsername());
            editor.putString("full_name", user.getFullName());
            editor.putString("email", user.getEmail());
            editor.putString("role", user.getRole());
            editor.putBoolean("isLoggedIn", true);
            editor.apply();

            Toast.makeText(this, "Welcome " + user.getFullName() + "!", Toast.LENGTH_SHORT).show();

            // Navigate based on role
            if ("admin".equals(user.getRole())) {
                startActivity(new Intent(this, AdminDashboardActivity.class));
            } else {
                startActivity(new Intent(this, DashboardActivity.class));
            }
            finish();
        } else {
            Toast.makeText(this, "❌ Invalid credentials", Toast.LENGTH_SHORT).show();
        }
    }
}