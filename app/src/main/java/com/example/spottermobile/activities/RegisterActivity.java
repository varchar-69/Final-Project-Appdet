package com.example.spottermobile.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spottermobile.R;
import com.example.spottermobile.database.DatabaseHelper;
import com.example.spottermobile.model.User;
import com.example.spottermobile.utils.PasswordUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * RegisterActivity — handles new member account creation.
 *
 * Key features added:
 *  1. Real-time TextWatcher validation on every field — errors appear
 *     inline via TextInputLayout.setError(), not Toast messages.
 *  2. Dynamic password strength evaluator — updates a 3-segment bar
 *     and label (Weak / Medium / Strong) as the user types.
 *  3. Confirm Password field — validates match in real-time.
 *  4. Final submit validation gate — all fields re-checked before
 *     hitting the database.
 */
public class RegisterActivity extends AppCompatActivity {

    // ── TextInputLayouts (hold error messages) ─────────────────────────────────
    private TextInputLayout tilUsername, tilFullName, tilEmail;
    private TextInputLayout tilPassword, tilConfirmPassword;
    private TextInputLayout tilContact, tilAddress;
    private TextInputLayout tilEmergencyName, tilEmergencyContact;

    // ── EditTexts (hold raw input) ─────────────────────────────────────────────
    private TextInputEditText etUsername, etFullName, etEmail;
    private TextInputEditText etPassword, etConfirmPassword;
    private TextInputEditText etContact, etAddress;
    private TextInputEditText etEmergencyName, etEmergencyContact;

    // ── Password strength views ────────────────────────────────────────────────
    private LinearLayout layoutPasswordStrength;
    private View segmentOne, segmentTwo, segmentThree;
    private TextView tvStrengthLabel, tvStrengthHint;

    // ── Gender toggle ──────────────────────────────────────────────────────────
    private LinearLayout btnMale, btnFemale;
    private String selectedGender = "Male"; // default

    private DatabaseHelper dbHelper;

    // ── Strength tier constants ────────────────────────────────────────────────
    private static final int STRENGTH_WEAK   = 1;
    private static final int STRENGTH_MEDIUM = 2;
    private static final int STRENGTH_STRONG = 3;

    // Segment colors
    private static final String COLOR_WEAK   = "#EF4444"; // red
    private static final String COLOR_MEDIUM = "#F59E0B"; // amber
    private static final String COLOR_STRONG = "#10B981"; // green
    private static final String COLOR_EMPTY  = "#E0E0E0"; // inactive gray

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        dbHelper = new DatabaseHelper(this);
        initViews();
        setupGenderToggle();
        attachTextWatchers();

        findViewById(R.id.btnRegister).setOnClickListener(v -> attemptRegister());
    }

    // ── VIEW BINDING ───────────────────────────────────────────────────────────

    private void initViews() {
        // TextInputLayouts
        tilUsername         = findViewById(R.id.tilUsername);
        tilFullName         = findViewById(R.id.tilFullName);
        tilEmail            = findViewById(R.id.tilEmail);
        tilPassword         = findViewById(R.id.tilPassword);
        tilConfirmPassword  = findViewById(R.id.tilConfirmPassword);
        tilContact          = findViewById(R.id.tilContact);
        tilAddress          = findViewById(R.id.tilAddress);
        tilEmergencyName    = findViewById(R.id.tilEmergencyName);
        tilEmergencyContact = findViewById(R.id.tilEmergencyContact);

        // EditTexts
        etUsername         = findViewById(R.id.etUsername);
        etFullName         = findViewById(R.id.etFullName);
        etEmail            = findViewById(R.id.etEmail);
        etPassword         = findViewById(R.id.etPassword);
        etConfirmPassword  = findViewById(R.id.etConfirmPassword);
        etContact          = findViewById(R.id.etContact);
        etAddress          = findViewById(R.id.etAddress);
        etEmergencyName    = findViewById(R.id.etEmergencyName);
        etEmergencyContact = findViewById(R.id.etEmergencyContact);

        // Strength indicator views
        layoutPasswordStrength = findViewById(R.id.layoutPasswordStrength);
        segmentOne             = findViewById(R.id.segmentOne);
        segmentTwo             = findViewById(R.id.segmentTwo);
        segmentThree           = findViewById(R.id.segmentThree);
        tvStrengthLabel        = findViewById(R.id.tvStrengthLabel);
        tvStrengthHint         = findViewById(R.id.tvStrengthHint);

        // Gender toggle
        btnMale   = findViewById(R.id.btnMale);
        btnFemale = findViewById(R.id.btnFemale);
    }

    // ── GENDER TOGGLE ──────────────────────────────────────────────────────────

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

    // ── TEXT WATCHERS ──────────────────────────────────────────────────────────

    /**
     * Attaches a TextWatcher to every field.
     * Each watcher fires on every keystroke (afterTextChanged) and:
     *   - Clears the error as soon as the user starts correcting the field.
     *   - Runs a lightweight inline check to show errors as they type.
     */
    private void attachTextWatchers() {

        // Username — clear error on typing, validate on change
        etUsername.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                String v = s.toString().trim();
                if (v.length() >= 3) {
                    tilUsername.setError(null);
                } else if (v.length() > 0) {
                    tilUsername.setError("Minimum 3 characters");
                }
            }
        });

        // Full Name
        etFullName.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                String v = s.toString().trim();
                if (v.length() >= 2) tilFullName.setError(null);
                else if (v.length() > 0) tilFullName.setError("Enter your full name");
            }
        });

        // Email — validate format in real-time
        etEmail.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                String v = s.toString().trim();
                if (android.util.Patterns.EMAIL_ADDRESS.matcher(v).matches()) {
                    tilEmail.setError(null);
                } else if (v.length() > 4) {
                    // Only show after they've typed enough to tell it's wrong
                    tilEmail.setError("Enter a valid email address");
                }
            }
        });

        // Password — drives the strength evaluator
        etPassword.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                String pwd = s.toString();

                if (pwd.isEmpty()) {
                    // Hide indicator when field is cleared
                    layoutPasswordStrength.setVisibility(View.GONE);
                    tilPassword.setError(null);
                    return;
                }

                // Show the strength indicator as soon as typing begins
                layoutPasswordStrength.setVisibility(View.VISIBLE);
                updateStrengthIndicator(pwd);

                // Inline error only if they've typed something meaningful
                if (pwd.length() >= 6) {
                    tilPassword.setError(null);
                } else {
                    tilPassword.setError("Minimum 6 characters");
                }

                // Re-evaluate confirm password match live whenever password changes
                validateConfirmPasswordMatch();
            }
        });

        // Confirm Password — checks match in real time
        etConfirmPassword.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                validateConfirmPasswordMatch();
            }
        });

        // Contact — must be exactly 11 digits
        etContact.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                String v = s.toString().trim();
                if (v.length() == 11) {
                    tilContact.setError(null);
                } else if (v.length() > 0) {
                    tilContact.setError("Must be exactly 11 digits");
                }
            }
        });

        // Address — minimum length check
        etAddress.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                String v = s.toString().trim();
                if (v.length() >= 10) tilAddress.setError(null);
                else if (v.length() > 0) tilAddress.setError("Address is too short");
            }
        });

        // Emergency Name
        etEmergencyName.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                String v = s.toString().trim();
                if (v.length() >= 2) tilEmergencyName.setError(null);
                else if (v.length() > 0) tilEmergencyName.setError("Enter contact name");
            }
        });

        // Emergency Contact — 11 digits
        etEmergencyContact.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                String v = s.toString().trim();
                if (v.length() == 11) tilEmergencyContact.setError(null);
                else if (v.length() > 0) tilEmergencyContact.setError("Must be exactly 11 digits");
            }
        });
    }

    // ── CONFIRM PASSWORD MATCH ─────────────────────────────────────────────────

    /**
     * Called from both the password and confirmPassword TextWatchers
     * so the match check updates whichever field the user last changed.
     */
    private void validateConfirmPasswordMatch() {
        String pwd     = etPassword.getText() != null ? etPassword.getText().toString() : "";
        String confirm = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString() : "";

        if (confirm.isEmpty()) {
            tilConfirmPassword.setError(null);
            return;
        }

        if (pwd.equals(confirm)) {
            tilConfirmPassword.setError(null);
        } else {
            tilConfirmPassword.setError("Passwords do not match");
        }
    }

    // ── PASSWORD STRENGTH EVALUATOR ────────────────────────────────────────────

    /**
     * Evaluates password strength and updates the 3-segment bar + labels.
     *
     * Scoring criteria (each criterion adds 1 point):
     *  1. Length ≥ 8 characters
     *  2. Contains at least one uppercase letter
     *  3. Contains at least one lowercase letter
     *  4. Contains at least one digit
     *  5. Contains at least one special character
     *
     * Score → Tier mapping:
     *  0–1  → WEAK   (segment 1 lit red)
     *  2–3  → MEDIUM (segments 1-2 lit amber)
     *  4–5  → STRONG (all 3 segments lit green)
     *
     * @param password The raw password string the user has typed.
     */
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

    /**
     * Applies the visual state for a given strength tier.
     * Segments light up progressively. Labels update with color + text.
     *
     * @param tier         STRENGTH_WEAK / MEDIUM / STRONG
     * @param passwordLen  Raw length used to build the hint message
     */
    private void applyStrengthUi(int tier, int passwordLen) {
        String barColor;
        String label;
        String hint;

        switch (tier) {
            case STRENGTH_MEDIUM:
                barColor = COLOR_MEDIUM;
                label    = "Medium";
                hint     = "Add uppercase, numbers or symbols";
                // Light segments 1 and 2; dim segment 3
                segmentOne.setBackgroundColor(Color.parseColor(COLOR_MEDIUM));
                segmentTwo.setBackgroundColor(Color.parseColor(COLOR_MEDIUM));
                segmentThree.setBackgroundColor(Color.parseColor(COLOR_EMPTY));
                break;

            case STRENGTH_STRONG:
                barColor = COLOR_STRONG;
                label    = "Strong ✓";
                hint     = "Great password!";
                // Light all three segments
                segmentOne.setBackgroundColor(Color.parseColor(COLOR_STRONG));
                segmentTwo.setBackgroundColor(Color.parseColor(COLOR_STRONG));
                segmentThree.setBackgroundColor(Color.parseColor(COLOR_STRONG));
                break;

            default: // STRENGTH_WEAK
                barColor = COLOR_WEAK;
                label    = "Weak";
                hint     = passwordLen < 8
                        ? "Use 8+ characters"
                        : "Add uppercase, numbers & symbols";
                // Only segment 1 lit; 2 and 3 dimmed
                segmentOne.setBackgroundColor(Color.parseColor(COLOR_WEAK));
                segmentTwo.setBackgroundColor(Color.parseColor(COLOR_EMPTY));
                segmentThree.setBackgroundColor(Color.parseColor(COLOR_EMPTY));
                break;
        }

        tvStrengthLabel.setText(label);
        tvStrengthLabel.setTextColor(Color.parseColor(barColor));
        tvStrengthHint.setText(hint);
    }

    // ── FINAL SUBMIT VALIDATION ────────────────────────────────────────────────

    /**
     * Runs a complete validation sweep across all fields before hitting the DB.
     * Each field sets its own error via TIL — the method returns false on first
     * failure so the user's focus lands naturally on the first broken field.
     *
     * Unlike the real-time watchers (which are forgiving while typing),
     * this is strict: every rule must pass to proceed.
     *
     * @return true if all inputs are valid, false otherwise.
     */
    private boolean validateAllFields(String username, String fullName, String email,
                                      String contact, String address, String emergencyName,
                                      String emergencyContact, String password,
                                      String confirmPassword) {
        boolean isValid = true;

        // Username
        if (username.length() < 3) {
            tilUsername.setError("Username must be at least 3 characters");
            if (isValid) etUsername.requestFocus();
            isValid = false;
        } else {
            tilUsername.setError(null);
        }

        // Full Name
        if (fullName.length() < 2) {
            tilFullName.setError("Full name is required");
            if (isValid) etFullName.requestFocus();
            isValid = false;
        } else {
            tilFullName.setError(null);
        }

        // Email
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email address");
            if (isValid) etEmail.requestFocus();
            isValid = false;
        } else {
            tilEmail.setError(null);
        }

        // Password length
        if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            if (isValid) etPassword.requestFocus();
            isValid = false;
        } else {
            tilPassword.setError(null);
        }

        // Confirm password match
        if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Passwords do not match");
            if (isValid) etConfirmPassword.requestFocus();
            isValid = false;
        } else if (confirmPassword.isEmpty()) {
            tilConfirmPassword.setError("Please confirm your password");
            if (isValid) etConfirmPassword.requestFocus();
            isValid = false;
        } else {
            tilConfirmPassword.setError(null);
        }

        // Contact — must be 11 digits
        if (contact.length() != 11 || !contact.matches("\\d+")) {
            tilContact.setError("Enter a valid 11-digit contact number");
            if (isValid) etContact.requestFocus();
            isValid = false;
        } else {
            tilContact.setError(null);
        }

        // Address
        if (address.length() < 10) {
            tilAddress.setError("Address is too short — provide your full address");
            if (isValid) etAddress.requestFocus();
            isValid = false;
        } else {
            tilAddress.setError(null);
        }

        // Emergency name
        if (emergencyName.length() < 2) {
            tilEmergencyName.setError("Emergency contact name is required");
            if (isValid) etEmergencyName.requestFocus();
            isValid = false;
        } else {
            tilEmergencyName.setError(null);
        }

        // Emergency contact — 11 digits
        if (emergencyContact.length() != 11 || !emergencyContact.matches("\\d+")) {
            tilEmergencyContact.setError("Enter a valid 11-digit number");
            if (isValid) etEmergencyContact.requestFocus();
            isValid = false;
        } else {
            tilEmergencyContact.setError(null);
        }

        return isValid;
    }

    // ── REGISTRATION SUBMIT ────────────────────────────────────────────────────

    private void attemptRegister() {
        // Collect all trimmed values
        String username         = etUsername.getText()         != null ? etUsername.getText().toString().trim()         : "";
        String fullName         = etFullName.getText()         != null ? etFullName.getText().toString().trim()         : "";
        String email            = etEmail.getText()            != null ? etEmail.getText().toString().trim()            : "";
        String contact          = etContact.getText()          != null ? etContact.getText().toString().trim()          : "";
        String address          = etAddress.getText()          != null ? etAddress.getText().toString().trim()          : "";
        String emergencyName    = etEmergencyName.getText()    != null ? etEmergencyName.getText().toString().trim()    : "";
        String emergencyContact = etEmergencyContact.getText() != null ? etEmergencyContact.getText().toString().trim() : "";
        String password         = etPassword.getText()         != null ? etPassword.getText().toString()                : "";
        String confirmPassword  = etConfirmPassword.getText()  != null ? etConfirmPassword.getText().toString()         : "";

        // Gate 1: field-level validation
        if (!validateAllFields(username, fullName, email, contact, address,
                emergencyName, emergencyContact, password, confirmPassword)) {
            return;
        }

        // Gate 2: uniqueness checks against the database
        if (dbHelper.isUserExists(username)) {
            tilUsername.setError("This username is already taken");
            etUsername.requestFocus();
            return;
        }
        if (dbHelper.isUserExists(email)) {
            tilEmail.setError("This email is already registered");
            etEmail.requestFocus();
            return;
        }

        // Gate 3: password hashing
        String hashedPassword = PasswordUtils.hashPassword(password);
        if (hashedPassword == null) {
            Toast.makeText(this, "Security error. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // All gates passed — create the user
        User user = new User(username, fullName, email, selectedGender, contact,
                address, emergencyName, emergencyContact, hashedPassword);

        boolean success = dbHelper.registerUser(user);
        if (success) {
            Toast.makeText(this, "Registration successful! Please log in.",
                    Toast.LENGTH_LONG).show();
            finish();
        } else {
            Toast.makeText(this, "Registration failed. Please try again.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ── SIMPLE TEXT WATCHER HELPER ─────────────────────────────────────────────

    /**
     * Abstract base class that stubs out beforeTextChanged() and onTextChanged()
     * so each anonymous watcher only needs to override afterTextChanged().
     * Keeps the watcher attachment code clean and DRY.
     */
    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}
