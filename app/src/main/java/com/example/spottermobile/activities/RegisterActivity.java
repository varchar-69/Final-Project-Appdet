package com.example.spottermobile.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spottermobile.R;
import com.example.spottermobile.database.FirestoreHelper;
import com.example.spottermobile.utils.PasswordUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilUsername, tilFullName, tilEmail;
    private TextInputLayout tilPassword, tilConfirmPassword;
    private TextInputLayout tilContact, tilAddress;
    private TextInputLayout tilEmergencyName, tilEmergencyContact;

    private TextInputEditText etUsername, etFullName, etEmail;
    private TextInputEditText etPassword, etConfirmPassword;
    private TextInputEditText etContact, etAddress;
    private TextInputEditText etEmergencyName, etEmergencyContact;

    private LinearLayout layoutPasswordStrength;
    private View segmentOne, segmentTwo, segmentThree;
    private TextView tvStrengthLabel, tvStrengthHint;

    private LinearLayout btnMale, btnFemale;
    private String selectedGender = "Male";

    private FirestoreHelper firestoreHelper;          // CHANGED

    private static final int STRENGTH_WEAK   = 1;
    private static final int STRENGTH_MEDIUM = 2;
    private static final int STRENGTH_STRONG = 3;

    private static final String COLOR_WEAK   = "#EF4444";
    private static final String COLOR_MEDIUM = "#F59E0B";
    private static final String COLOR_STRONG = "#10B981";
    private static final String COLOR_EMPTY  = "#E0E0E0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        firestoreHelper = new FirestoreHelper();      // CHANGED
        initViews();
        setupGenderToggle();
        attachTextWatchers();

        findViewById(R.id.btnRegister).setOnClickListener(v -> attemptRegister());
    }

    private void initViews() {
        tilUsername         = findViewById(R.id.tilUsername);
        tilFullName         = findViewById(R.id.tilFullName);
        tilEmail            = findViewById(R.id.tilEmail);
        tilPassword         = findViewById(R.id.tilPassword);
        tilConfirmPassword  = findViewById(R.id.tilConfirmPassword);
        tilContact          = findViewById(R.id.tilContact);
        tilAddress          = findViewById(R.id.tilAddress);
        tilEmergencyName    = findViewById(R.id.tilEmergencyName);
        tilEmergencyContact = findViewById(R.id.tilEmergencyContact);

        etUsername         = findViewById(R.id.etUsername);
        etFullName         = findViewById(R.id.etFullName);
        etEmail            = findViewById(R.id.etEmail);
        etPassword         = findViewById(R.id.etPassword);
        etConfirmPassword  = findViewById(R.id.etConfirmPassword);
        etContact          = findViewById(R.id.etContact);
        etAddress          = findViewById(R.id.etAddress);
        etEmergencyName    = findViewById(R.id.etEmergencyName);
        etEmergencyContact = findViewById(R.id.etEmergencyContact);

        layoutPasswordStrength = findViewById(R.id.layoutPasswordStrength);
        segmentOne             = findViewById(R.id.segmentOne);
        segmentTwo             = findViewById(R.id.segmentTwo);
        segmentThree           = findViewById(R.id.segmentThree);
        tvStrengthLabel        = findViewById(R.id.tvStrengthLabel);
        tvStrengthHint         = findViewById(R.id.tvStrengthHint);

        btnMale   = findViewById(R.id.btnMale);
        btnFemale = findViewById(R.id.btnFemale);
    }

    private void setupGenderToggle() {
        btnMale.setSelected(true);
        btnMale.setOnClickListener(v -> {
            selectedGender = "Male";
            btnMale.setSelected(true);
            btnFemale.setSelected(false);
        });
        btnFemale.setOnClickListener(v -> {
            selectedGender = "Female";
            btnFemale.setSelected(true);
            btnMale.setSelected(false);
        });
    }

    private void attachTextWatchers() {
        etUsername.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                String v = s.toString().trim();
                if (v.length() >= 3) tilUsername.setError(null);
                else if (v.length() > 0) tilUsername.setError("Minimum 3 characters");
            }
        });
        etFullName.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                String v = s.toString().trim();
                if (v.length() >= 2) tilFullName.setError(null);
                else if (v.length() > 0) tilFullName.setError("Enter your full name");
            }
        });
        etEmail.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                String v = s.toString().trim();
                if (android.util.Patterns.EMAIL_ADDRESS.matcher(v).matches()) tilEmail.setError(null);
                else if (v.length() > 4) tilEmail.setError("Enter a valid email address");
            }
        });
        etPassword.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                String pwd = s.toString();
                if (pwd.isEmpty()) {
                    layoutPasswordStrength.setVisibility(View.GONE);
                    tilPassword.setError(null);
                    return;
                }
                layoutPasswordStrength.setVisibility(View.VISIBLE);
                updateStrengthIndicator(pwd);
                if (pwd.length() >= 6) tilPassword.setError(null);
                else tilPassword.setError("Minimum 6 characters");
                validateConfirmPasswordMatch();
            }
        });
        etConfirmPassword.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) { validateConfirmPasswordMatch(); }
        });
        etContact.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                String v = s.toString().trim();
                if (v.length() == 11) tilContact.setError(null);
                else if (v.length() > 0) tilContact.setError("Must be exactly 11 digits");
            }
        });
        etAddress.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                String v = s.toString().trim();
                if (v.length() >= 10) tilAddress.setError(null);
                else if (v.length() > 0) tilAddress.setError("Address is too short");
            }
        });
        etEmergencyName.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                String v = s.toString().trim();
                if (v.length() >= 2) tilEmergencyName.setError(null);
                else if (v.length() > 0) tilEmergencyName.setError("Enter contact name");
            }
        });
        etEmergencyContact.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                String v = s.toString().trim();
                if (v.length() == 11) tilEmergencyContact.setError(null);
                else if (v.length() > 0) tilEmergencyContact.setError("Must be exactly 11 digits");
            }
        });
    }

    private void validateConfirmPasswordMatch() {
        String pwd     = etPassword.getText()        != null ? etPassword.getText().toString()        : "";
        String confirm = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString() : "";
        if (confirm.isEmpty()) { tilConfirmPassword.setError(null); return; }
        if (pwd.equals(confirm)) tilConfirmPassword.setError(null);
        else tilConfirmPassword.setError("Passwords do not match");
    }

    private void updateStrengthIndicator(String password) {
        int score = 0;
        if (password.length() >= 8)                          score++;
        if (password.matches(".*[A-Z].*"))                   score++;
        if (password.matches(".*[a-z].*"))                   score++;
        if (password.matches(".*\\d.*"))                     score++;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\",./<>?].*")) score++;
        int tier;
        if (score <= 1)      tier = STRENGTH_WEAK;
        else if (score <= 3) tier = STRENGTH_MEDIUM;
        else                 tier = STRENGTH_STRONG;
        applyStrengthUi(tier, password.length());
    }

    private void applyStrengthUi(int tier, int passwordLen) {
        String barColor, label, hint;
        switch (tier) {
            case STRENGTH_MEDIUM:
                barColor = COLOR_MEDIUM; label = "Medium"; hint = "Add uppercase, numbers or symbols";
                segmentOne.setBackgroundColor(Color.parseColor(COLOR_MEDIUM));
                segmentTwo.setBackgroundColor(Color.parseColor(COLOR_MEDIUM));
                segmentThree.setBackgroundColor(Color.parseColor(COLOR_EMPTY));
                break;
            case STRENGTH_STRONG:
                barColor = COLOR_STRONG; label = "Strong ✓"; hint = "Great password!";
                segmentOne.setBackgroundColor(Color.parseColor(COLOR_STRONG));
                segmentTwo.setBackgroundColor(Color.parseColor(COLOR_STRONG));
                segmentThree.setBackgroundColor(Color.parseColor(COLOR_STRONG));
                break;
            default:
                barColor = COLOR_WEAK; label = "Weak";
                hint = passwordLen < 8 ? "Use 8+ characters" : "Add uppercase, numbers & symbols";
                segmentOne.setBackgroundColor(Color.parseColor(COLOR_WEAK));
                segmentTwo.setBackgroundColor(Color.parseColor(COLOR_EMPTY));
                segmentThree.setBackgroundColor(Color.parseColor(COLOR_EMPTY));
                break;
        }
        tvStrengthLabel.setText(label);
        tvStrengthLabel.setTextColor(Color.parseColor(barColor));
        tvStrengthHint.setText(hint);
    }

    private boolean validateAllFields(String username, String fullName, String email,
                                      String contact, String address, String emergencyName,
                                      String emergencyContact, String password, String confirmPassword) {
        boolean isValid = true;
        if (username.length() < 3) { tilUsername.setError("Username must be at least 3 characters"); if (isValid) etUsername.requestFocus(); isValid = false; } else tilUsername.setError(null);
        if (fullName.length() < 2) { tilFullName.setError("Full name is required"); if (isValid) etFullName.requestFocus(); isValid = false; } else tilFullName.setError(null);
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) { tilEmail.setError("Enter a valid email address"); if (isValid) etEmail.requestFocus(); isValid = false; } else tilEmail.setError(null);
        if (password.length() < 6) { tilPassword.setError("Password must be at least 6 characters"); if (isValid) etPassword.requestFocus(); isValid = false; } else tilPassword.setError(null);
        if (!password.equals(confirmPassword)) { tilConfirmPassword.setError("Passwords do not match"); if (isValid) etConfirmPassword.requestFocus(); isValid = false; }
        else if (confirmPassword.isEmpty()) { tilConfirmPassword.setError("Please confirm your password"); if (isValid) etConfirmPassword.requestFocus(); isValid = false; }
        else tilConfirmPassword.setError(null);
        if (contact.length() != 11 || !contact.matches("\\d+")) { tilContact.setError("Enter a valid 11-digit contact number"); if (isValid) etContact.requestFocus(); isValid = false; } else tilContact.setError(null);
        if (address.length() < 10) { tilAddress.setError("Address is too short — provide your full address"); if (isValid) etAddress.requestFocus(); isValid = false; } else tilAddress.setError(null);
        if (emergencyName.length() < 2) { tilEmergencyName.setError("Emergency contact name is required"); if (isValid) etEmergencyName.requestFocus(); isValid = false; } else tilEmergencyName.setError(null);
        if (emergencyContact.length() != 11 || !emergencyContact.matches("\\d+")) { tilEmergencyContact.setError("Enter a valid 11-digit number"); if (isValid) etEmergencyContact.requestFocus(); isValid = false; } else tilEmergencyContact.setError(null);
        return isValid;
    }

    // ── REGISTRATION SUBMIT ────────────────────────────────────────────────────

    private void attemptRegister() {
        String username         = etUsername.getText()         != null ? etUsername.getText().toString().trim()         : "";
        String fullName         = etFullName.getText()         != null ? etFullName.getText().toString().trim()         : "";
        String email            = etEmail.getText()            != null ? etEmail.getText().toString().trim()            : "";
        String contact          = etContact.getText()          != null ? etContact.getText().toString().trim()          : "";
        String address          = etAddress.getText()          != null ? etAddress.getText().toString().trim()          : "";
        String emergencyName    = etEmergencyName.getText()    != null ? etEmergencyName.getText().toString().trim()    : "";
        String emergencyContact = etEmergencyContact.getText() != null ? etEmergencyContact.getText().toString().trim() : "";
        String password         = etPassword.getText()         != null ? etPassword.getText().toString()                : "";
        String confirmPassword  = etConfirmPassword.getText()  != null ? etConfirmPassword.getText().toString()         : "";

        // Gate 1: field validation
        if (!validateAllFields(username, fullName, email, contact, address,
                emergencyName, emergencyContact, password, confirmPassword)) return;

        // Gate 2: hash password
        String hashedPassword = PasswordUtils.hashPassword(password);
        if (hashedPassword == null) {
            Toast.makeText(this, "Security error. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // CHANGED: disable button while async calls run
        Button btnRegister = findViewById(R.id.btnRegister);
        btnRegister.setEnabled(false);

        // Gate 3: check username uniqueness asynchronously, then register
        // CHANGED: was two synchronous isUserExists() calls + registerUser()
        firestoreHelper.isUserExists(username, new FirestoreHelper.FirestoreCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean exists) {
                if (exists) {
                    tilUsername.setError("This username is already taken");
                    etUsername.requestFocus();
                    btnRegister.setEnabled(true);
                    return;
                }
                // Username is free — now register (FirestoreHelper doesn't check email
                // separately, so we pass all fields directly)
                final String finalUsername    = username;
                final String finalFullName    = fullName;
                final String finalEmail       = email;
                final String finalContact     = contact;
                final String finalAddress     = address;
                final String finalEmgName     = emergencyName;
                final String finalEmgContact  = emergencyContact;
                final String finalHashedPwd   = hashedPassword;
                final String finalGender      = selectedGender; // FIX: capture gender for registerUser

                // FIX: pass all profile fields — previously only 5 args were sent,
                // so gender/contact/address/emergency were never saved to Firestore.
                firestoreHelper.registerUser(
                        finalFullName, finalEmail, finalUsername, finalHashedPwd,
                        "member",
                        finalGender,      // was missing
                        finalContact,     // was missing
                        finalAddress,     // was missing
                        finalEmgName,     // was missing
                        finalEmgContact,  // was missing
                        new FirestoreHelper.FirestoreCallback<String>() {
                            @Override
                            public void onSuccess(String docId) {
                                Toast.makeText(RegisterActivity.this,
                                        "Registration successful! Please log in.",
                                        Toast.LENGTH_LONG).show();
                                finish();
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                btnRegister.setEnabled(true);
                                Toast.makeText(RegisterActivity.this,
                                        "Registration failed. Please try again.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            public void onFailure(String errorMessage) {
                btnRegister.setEnabled(true);
                Toast.makeText(RegisterActivity.this,
                        "Could not verify username. Please try again.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}