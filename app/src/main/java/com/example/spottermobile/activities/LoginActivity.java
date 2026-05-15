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
import com.example.spottermobile.database.DatabaseHelper;
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
    // captchaAnswer is always set by generateCaptcha() before any check runs.
    private int captchaAnswer = -1;
    // ────────────────────────────────────────────────────────────────────────

    private EditText          etIdentifier, etPassword, etCaptcha;
    private TextView          tvCaptchaQuestion;
    private Button            btnLogin;
    private DatabaseHelper    dbHelper;
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
        dbHelper = new DatabaseHelper(this);
        prefs    = getSharedPreferences("SpotterPrefs", MODE_PRIVATE);

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

    /**
     * Generates a new random arithmetic question (addition or subtraction, result ≥ 0),
     * stores the answer in {@code captchaAnswer}, updates the question label, and
     * clears the answer field.
     *
     * Called: on launch, after every wrong CAPTCHA answer, after every wrong credential
     * attempt, and once the 60-second lockout expires.
     */
    private void generateCaptcha() {
        Random rnd = new Random();
        int a  = rnd.nextInt(10) + 1;  // 1–10
        int b  = rnd.nextInt(10) + 1;  // 1–10
        int op = rnd.nextInt(2);       // 0 = add, 1 = subtract

        String question;
        if (op == 0) {
            captchaAnswer = a + b;
            question = a + " + " + b + " = ?";
        } else {
            // Keep the larger number first so the result is always ≥ 0
            if (a < b) { int t = a; a = b; b = t; }
            captchaAnswer = a - b;
            question = a + " - " + b + " = ?";   // plain ASCII hyphen, not Unicode minus
        }

        tvCaptchaQuestion.setText(question);
        etCaptcha.setText("");
        etCaptcha.setError(null);
    }

    /**
     * Reads the user's typed answer and compares it to {@code captchaAnswer}.
     *
     * BUG FIX: previously this could silently fail because getText() on a
     * TextInputEditText sometimes returns null before the view is fully attached.
     * Now guarded with an explicit null-check before calling toString().
     *
     * @return true only when the parsed integer exactly matches {@code captchaAnswer}.
     */
    private boolean isCaptchaCorrect() {
        // FIX: null-guard getText() — TextInputEditText can return null in edge cases
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
    //  Enforced order: lockout → CAPTCHA → field validation → hash → DB auth
    // ─────────────────────────────────────────────────────────────────────────

    private void attemptLogin() {

        // ── Step 1: Lockout gate ──────────────────────────────────────────────
        // If the user is locked out, block silently (countdown UI already visible).
        if (isLockedOut()) return;

        // ── Step 2: CAPTCHA gate ──────────────────────────────────────────────
        // Must be solved before credentials are ever read.
        // generateCaptcha() is called ONLY when the answer is wrong — NOT here —
        // so captchaAnswer stays valid for re-submission if fields are empty.
        if (!isCaptchaCorrect()) {
            generateCaptcha();  // fresh question on every wrong CAPTCHA answer
            return;
        }

        // ── Step 3: Field validation ──────────────────────────────────────────
        // Empty/short fields are NOT counted as a failed login attempt.
        String identifier = etIdentifier.getText() != null
                ? etIdentifier.getText().toString().trim() : "";
        String password   = etPassword.getText() != null
                ? etPassword.getText().toString().trim() : "";

        if (identifier.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            generateCaptcha();  // regenerate so the user faces a fresh challenge
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
            // Should never happen on Android — MessageDigest always has SHA-256
            Toast.makeText(this, "Security error. Please try again.", Toast.LENGTH_SHORT).show();
            generateCaptcha();
            return;
        }

        // ── Step 5: Database authentication ──────────────────────────────────
        User user = dbHelper.loginUser(identifier, hashedPassword);

        if (user != null) {
            // ╔══════ SUCCESS ══════╗

            // Block suspended members before doing anything else
            if (user.isSuspended()) {
                generateCaptcha();
                Toast.makeText(this,
                        "Your account has been suspended. Please contact the gym.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            resetFailCount();   // wipe attempt counter on any successful login

            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("user_id",        user.getId());
            editor.putString("username",    user.getUsername());
            editor.putString("full_name",   user.getFullName());
            editor.putString("email",       user.getEmail());
            editor.putString("role",        user.getRole());
            editor.putBoolean("isLoggedIn", true);
            editor.apply();

            Toast.makeText(this, "Welcome " + user.getFullName() + "!", Toast.LENGTH_SHORT).show();

            Class<?> destination = "admin".equals(user.getRole())
                    ? AdminDashboardActivity.class
                    : DashboardActivity.class;
            startActivity(new Intent(this, destination));
            finish();

        } else {
            // ╔══════ FAILURE ══════╗
            int fails     = incrementFailCount();
            int remaining = MAX_ATTEMPTS - fails;

            generateCaptcha();  // always give a new question after wrong credentials

            if (fails >= MAX_ATTEMPTS) {
                startLockout();
            } else {
                Toast.makeText(this,
                        "Invalid credentials. " + remaining
                                + " attempt" + (remaining == 1 ? "" : "s") + " remaining.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Attempt counter helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Increments the persisted fail counter and returns the new total. */
    private int incrementFailCount() {
        int count = prefs.getInt(PREF_FAIL_COUNT, 0) + 1;
        prefs.edit().putInt(PREF_FAIL_COUNT, count).apply();
        return count;
    }

    /** Clears the fail counter and any lockout end-time. Called on success. */
    private void resetFailCount() {
        prefs.edit()
                .putInt(PREF_FAIL_COUNT, 0)
                .putLong(PREF_LOCKOUT_TIME, 0)
                .apply();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Lockout helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns true if a lockout is currently active.
     *
     * BUG FIX (previous version): the old code called startLockout() here when the
     * timer reference was null, which recalculated lockoutUntil = now + 60s and
     * OVERWROTE the stored timestamp — effectively resetting the lockout every time
     * the user reopened the app. Now calls runCountdown() with the STORED end-time
     * so the remaining time is always respected.
     */
    private boolean isLockedOut() {
        long lockoutUntil = prefs.getLong(PREF_LOCKOUT_TIME, 0);
        if (System.currentTimeMillis() < lockoutUntil) {
            // Timer might be null if the activity was recreated mid-lockout
            if (countDownTimer == null) runCountdown(lockoutUntil); // FIX: not startLockout()
            return true;
        }
        return false;
    }

    /**
     * Writes the lockout end-time and starts the countdown UI.
     * Only called when a NEW lockout begins (5th failed attempt).
     */
    private void startLockout() {
        long lockoutUntil = System.currentTimeMillis() + LOCKOUT_MS;
        prefs.edit().putLong(PREF_LOCKOUT_TIME, lockoutUntil).apply();
        runCountdown(lockoutUntil);
    }

    /**
     * Called from onCreate — if a lockout was in progress when the app was closed,
     * resumes the countdown UI with the remaining time, not a fresh 60 seconds.
     */
    private void resumeLockoutIfActive() {
        long lockoutUntil = prefs.getLong(PREF_LOCKOUT_TIME, 0);
        if (System.currentTimeMillis() < lockoutUntil) {
            runCountdown(lockoutUntil);
        }
    }

    /**
     * Disables the login button, ticks down every second until {@code lockoutUntil},
     * then re-enables the button and generates a fresh CAPTCHA.
     *
     * @param lockoutUntil absolute epoch milliseconds when the lockout expires.
     */
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
                generateCaptcha();  // fresh question after lockout expires
                Toast.makeText(LoginActivity.this,
                        "You may try again now.", Toast.LENGTH_SHORT).show();
            }
        }.start();

        Toast.makeText(this,
                "Too many failed attempts. Try again after 60 seconds.",
                Toast.LENGTH_LONG).show();
    }
}