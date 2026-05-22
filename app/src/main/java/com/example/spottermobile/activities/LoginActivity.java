package com.example.spottermobile.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spottermobile.R;
import com.example.spottermobile.database.FirestoreHelper;
import com.example.spottermobile.model.User;
import com.example.spottermobile.utils.PasswordUtils;

import java.util.Random;

public class LoginActivity extends AppCompatActivity {

    // ── Lockout constants ────────────────────────────────────────────────────
    private static final int  MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_MS   = 60_000L;              // 60 seconds

    private static final String PREF_FAIL_COUNT   = "login_fail_count";
    private static final String PREF_LOCKOUT_TIME = "login_lockout_until"; // epoch ms
    // ────────────────────────────────────────────────────────────────────────

    // ── CAPTCHA state ────────────────────────────────────────────────────────
    private int captchaAnswer = -1;
    // ────────────────────────────────────────────────────────────────────────

    private EditText          etIdentifier, etPassword, etCaptcha;
    private TextView          tvCaptchaQuestion;
    private Button            btnLogin;
    private FirestoreHelper   firestoreHelper;       // CHANGED: was DatabaseHelper
    private SharedPreferences prefs;
    private CountDownTimer    countDownTimer;

    // ─────────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        firestoreHelper = new FirestoreHelper();
        firestoreHelper.seedAdminIfNotExists();
        prefs = getSharedPreferences("SpotterPrefs", MODE_PRIVATE);

        // Register link
        TextView tvRegister = findViewById(R.id.tvRegister);
        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        // Demo admin hint — tapping fills the fields for quick testing
        TextView tvDemo = findViewById(R.id.tvDemoAdmin);
        tvDemo.setOnClickListener(v -> {
            etIdentifier.setText("admin");
            etPassword.setText("admin123");
        });

        btnLogin.setOnClickListener(v -> attemptLogin());

        // Generate first CAPTCHA before anything else is interactive
        generateCaptcha();

        // If app was reopened while a lockout was still running, restore the UI
        resumeLockoutIfActive();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  View initialisation
    // ─────────────────────────────────────────────────────────────────────────

    private void initViews() {
        etIdentifier      = findViewById(R.id.etIdentifier);
        etPassword        = findViewById(R.id.etPassword);
        etCaptcha         = findViewById(R.id.etCaptcha);
        tvCaptchaQuestion = findViewById(R.id.tvCaptchaQuestion);
        btnLogin          = findViewById(R.id.btnLogin);
        // NOTE: Do NOT call setInputType() on TextInputEditText fields programmatically.
        // inputType is already declared in the XML layout and overriding it here breaks
        // the Material password-toggle icon on the password field.
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CAPTCHA
    // ─────────────────────────────────────────────────────────────────────────

    private void generateCaptcha() {
        Random rnd = new Random();
        int a  = rnd.nextInt(10) + 1;
        int b  = rnd.nextInt(10) + 1;
        int op = rnd.nextInt(2);

        String question;
        if (op == 0) {
            captchaAnswer = a + b;
            question = a + " + " + b + " = ?";
        } else {
            if (a < b) { int t = a; a = b; b = t; }
            captchaAnswer = a - b;
            question = a + " - " + b + " = ?";
        }

        tvCaptchaQuestion.setText(question);
        etCaptcha.setText("");
        etCaptcha.setError(null);
    }

    private boolean isCaptchaCorrect() {
        android.text.Editable editable = etCaptcha.getText();
        String input = (editable != null) ? editable.toString().trim() : "";

        if (input.isEmpty()) {
            etCaptcha.setError("Please answer the security question");
            return false;
        }

        try {
            int userAnswer = Integer.parseInt(input);
            if (userAnswer == captchaAnswer) {
                return true;
            }
        } catch (NumberFormatException ignored) {
            // Non-numeric input — fall through to error below
        }

        etCaptcha.setError("Wrong answer — try the new question");
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Core login flow
    //  Enforced order: lockout → CAPTCHA → field validation → hash → Firestore auth
    // ─────────────────────────────────────────────────────────────────────────

    private void attemptLogin() {

        // ── Step 1: Lockout gate ──────────────────────────────────────────────
        if (isLockedOut()) return;

        // ── Step 2: CAPTCHA gate ──────────────────────────────────────────────
        if (!isCaptchaCorrect()) {
            generateCaptcha();
            return;
        }

        // ── Step 3: Field validation ──────────────────────────────────────────
        String identifier = etIdentifier.getText() != null
                ? etIdentifier.getText().toString().trim() : "";
        String password   = etPassword.getText() != null
                ? etPassword.getText().toString().trim() : "";

        if (identifier.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            generateCaptcha();
            return;
        }
        if (password.length() < 4) {
            Toast.makeText(this, "Password too short", Toast.LENGTH_SHORT).show();
            generateCaptcha();
            return;
        }

        // ── Step 4: SHA-256 hash ──────────────────────────────────────────────
        String hashedPassword = PasswordUtils.hashPassword(password);
        if (hashedPassword == null) {
            Toast.makeText(this, "Security error. Please try again.", Toast.LENGTH_SHORT).show();
            generateCaptcha();
            return;
        }

        // ── Step 5: Disable button while Firestore call is in-flight ─────────
        // CHANGED: Firestore is async — disable the button to prevent double-taps
        btnLogin.setEnabled(false);

        // ── Step 6: Firestore authentication ─────────────────────────────────
        // CHANGED: replaced synchronous dbHelper.loginUser() with async loginUser()
        firestoreHelper.loginUser(identifier, hashedPassword, new FirestoreHelper.FirestoreCallback<User>() {

            @Override
            public void onSuccess(User user) {
                // ── Suspended check ───────────────────────────────────────────
                if (user.isSuspended()) {
                    btnLogin.setEnabled(true);
                    generateCaptcha();
                    Toast.makeText(LoginActivity.this,
                            "Your account has been suspended. Please contact the gym.",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                // ── SUCCESS ───────────────────────────────────────────────────
                resetFailCount();

                // CHANGED: user_id is now a Firestore document ID (String).
                // We store it under the key "user_id" as a String so other
                // activities that read prefs.getString("user_id", "") keep working
                // without further changes. Remove the old putInt("user_id") call.
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("user_id", user.getFirestoreId()); //old: editor.putString("user_id", user.getId());
                editor.putString("username",    user.getUsername());
                editor.putString("full_name",   user.getFullName());
                editor.putString("email",       user.getEmail());
                editor.putString("role",        user.getRole());
                editor.putBoolean("isLoggedIn", true);
                editor.apply();

                Toast.makeText(LoginActivity.this,
                        "Welcome " + user.getFullName() + "!", Toast.LENGTH_SHORT).show();

                // FirestoreHelper uses "userType" field; User.getRole() returns it
                Class<?> destination = "admin".equals(user.getRole())
                        ? AdminDashboardActivity.class
                        : DashboardActivity.class;
                startActivity(new Intent(LoginActivity.this, destination));
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                // ── FAILURE ───────────────────────────────────────────────────
                btnLogin.setEnabled(true);

                int fails     = incrementFailCount();
                int remaining = MAX_ATTEMPTS - fails;

                generateCaptcha();

                if (fails >= MAX_ATTEMPTS) {
                    startLockout();
                } else {
                    Toast.makeText(LoginActivity.this,
                            "Invalid credentials. " + remaining
                                    + " attempt" + (remaining == 1 ? "" : "s") + " remaining.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Attempt counter helpers
    // ─────────────────────────────────────────────────────────────────────────

    private int incrementFailCount() {
        int count = prefs.getInt(PREF_FAIL_COUNT, 0) + 1;
        prefs.edit().putInt(PREF_FAIL_COUNT, count).apply();
        return count;
    }

    private void resetFailCount() {
        prefs.edit()
                .putInt(PREF_FAIL_COUNT, 0)
                .putLong(PREF_LOCKOUT_TIME, 0)
                .apply();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Lockout helpers
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isLockedOut() {
        long lockoutUntil = prefs.getLong(PREF_LOCKOUT_TIME, 0);
        if (System.currentTimeMillis() < lockoutUntil) {
            if (countDownTimer == null) runCountdown(lockoutUntil);
            return true;
        }
        return false;
    }

    private void startLockout() {
        long lockoutUntil = System.currentTimeMillis() + LOCKOUT_MS;
        prefs.edit().putLong(PREF_LOCKOUT_TIME, lockoutUntil).apply();
        runCountdown(lockoutUntil);
    }

    private void resumeLockoutIfActive() {
        long lockoutUntil = prefs.getLong(PREF_LOCKOUT_TIME, 0);
        if (System.currentTimeMillis() < lockoutUntil) {
            runCountdown(lockoutUntil);
        }
    }

    private void runCountdown(long lockoutUntil) {
        long millisRemaining = lockoutUntil - System.currentTimeMillis();
        if (millisRemaining <= 0) return;

        btnLogin.setEnabled(false);
        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(millisRemaining, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                btnLogin.setText("Try again in " + (millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                btnLogin.setEnabled(true);
                btnLogin.setText("Login");
                resetFailCount();
                generateCaptcha();
                Toast.makeText(LoginActivity.this,
                        "You may try again now.", Toast.LENGTH_SHORT).show();
            }
        }.start();

        Toast.makeText(this,
                "Too many failed attempts. Try again after 60 seconds.",
                Toast.LENGTH_LONG).show();
    }
}