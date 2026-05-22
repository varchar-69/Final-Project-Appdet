package com.example.spottermobile.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.spottermobile.R;
import com.example.spottermobile.database.FirestoreHelper;
import com.example.spottermobile.model.Booking;
import com.example.spottermobile.notifications.NotificationHelper;
import com.example.spottermobile.utils.SlotUtils;

import java.util.Calendar;

public class BookingActivity extends AppCompatActivity {
    private static final int REQUEST_PAYMENT = 1001;

    // Capacity constants
    private static final int MAX_SLOT_CAPACITY  = 2;
    private static final int MAX_DAILY_CAPACITY = 1;

    private Spinner spinnerSplit;
    private Spinner spinnerTimeSlot;

    private TextView tvSplitDescription;
    private TextView tvSelectedDate;
    private TextView tvSlotStatus;

    private CardView cardDatePicker;
    private CardView cardCustomSplit;

    private EditText etCustomSplit;

    // CHANGED: FirestoreHelper replaces DatabaseHelper
    private FirestoreHelper firestoreHelper;
    private SharedPreferences sharedPreferences;

    // CHANGED: userId is now a String (Firestore document ID)
    private String userId;

    private String selectedDate = null;
    private String selectedTime = null;
    private String pendingWorkoutType = null;

    // FIXED TIME SLOTS

    private static final String[] TIME_SLOTS = {
            "Select a time slot",
            "5:00 AM - 7:00 AM",
            "7:00 AM - 9:00 AM",
            "9:00 AM - 11:00 AM",
            "11:00 AM - 1:00 PM",
            "1:00 PM - 3:00 PM",
            "3:00 PM - 5:00 PM",
            "5:00 PM - 7:00 PM",
            "7:00 PM - 9:00 PM",
            "9:00 PM - 11:00 PM"
    };

    // WORKOUT SPLITS

    private static final String[] SPLIT_NAMES = {
            "Push — Chest / Shoulders / Triceps",
            "Pull — Back / Biceps / Rear Delts",
            "Legs — Quads / Hamstrings / Glutes / Calves",
            "Upper Body — Chest, Back, Shoulders, Arms",
            "Lower Body — Full Legs + Core",
            "Full Body — Compound Lifts",
            "Core & Cardio — Abs / Obliques / Cardio",
            "Mobility & Stretching",
            "Custom — I'll describe my own"
    };

    private static final String[] SPLIT_DESCRIPTIONS = {
            "Pressing movements: bench, overhead press, dips, lateral raises, push-downs.",
            "Pulling movements: rows, pull-ups/downs, curls, face pulls.",
            "Squat, Romanian deadlift, leg press, lunges, calf raises.",
            "Balanced upper session: push + pull + shoulders + arms.",
            "Leg-focused with added core work: planks, leg raises, deadlifts.",
            "Full-body compound session: squat, bench, deadlift, rows, OHP.",
            "Core stability, ab work, and steady-state or interval cardio.",
            "Foam rolling, static stretches, and joint mobility drills.",
            "Enter your own workout focus below."
    };

    private static final int CUSTOM_SPLIT_INDEX = 8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);
        setupBottomNav("book");

        spinnerSplit    = findViewById(R.id.spinnerSplit);
        spinnerTimeSlot = findViewById(R.id.spinnerTimeSlot);

        tvSplitDescription = findViewById(R.id.tvSplitDescription);
        tvSelectedDate     = findViewById(R.id.tvSelectedDate);
        tvSlotStatus       = findViewById(R.id.tvSlotStatus);

        cardDatePicker  = findViewById(R.id.cardDatePicker);
        cardCustomSplit = findViewById(R.id.cardCustomSplit);

        etCustomSplit = findViewById(R.id.etCustomSplit);

        firestoreHelper   = new FirestoreHelper();
        sharedPreferences = getSharedPreferences("SpotterPrefs", MODE_PRIVATE);

        userId = sharedPreferences.getString("user_id", null);

        if (userId == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        checkActiveBookingAndInit();
    }

    // ACTIVE BOOKING GUARD

    private void checkActiveBookingAndInit() {
        firestoreHelper.hasAnyActiveBooking(userId, new FirestoreHelper.FirestoreCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean hasActive) {
                if (hasActive) {
                    showAlreadyBookedDialog();
                } else {
                    // Safe to show the booking form
                    setupSplitSpinner();
                    setupTimeSlotSpinner();
                    setupDatePicker();

                    Button btnBook = findViewById(R.id.btnConfirmBooking);
                    btnBook.setOnClickListener(v -> confirmBooking());
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                // Fail open: let the user try to book; server will catch duplicates
                Toast.makeText(BookingActivity.this,
                        "Could not verify booking status. Proceed with caution.",
                        Toast.LENGTH_SHORT).show();
                setupSplitSpinner();
                setupTimeSlotSpinner();
                setupDatePicker();

                Button btnBook = findViewById(R.id.btnConfirmBooking);
                btnBook.setOnClickListener(v -> confirmBooking());
            }
        });
    }

    private void showAlreadyBookedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Active Booking Exists")
                .setMessage(
                        "You already have an active booking.\n\n" +
                                "You can only make a new booking after your current session " +
                                "is completed or cancelled.")
                .setPositiveButton("View My Bookings", (d, w) -> finish())
                .setNegativeButton("Go Back", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    // SPLIT SPINNER

    private void setupSplitSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                SPLIT_NAMES
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSplit.setAdapter(adapter);

        spinnerSplit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                tvSplitDescription.setText(SPLIT_DESCRIPTIONS[position]);
                cardCustomSplit.setVisibility(
                        position == CUSTOM_SPLIT_INDEX ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        tvSplitDescription.setText(SPLIT_DESCRIPTIONS[0]);
    }

    //  TIME SLOT SPINNER

    private void setupTimeSlotSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                TIME_SLOTS
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTimeSlot.setAdapter(adapter);

        spinnerTimeSlot.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedTime = null;
                    if (tvSlotStatus != null) tvSlotStatus.setVisibility(View.GONE);
                } else {
                    selectedTime = TIME_SLOTS[position];
                    refreshSlotStatus();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // DATE PICKER

    private void setupDatePicker() {
        cardDatePicker.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(
                this,
                (view, y, m, d) -> {
                    selectedDate = String.format("%04d-%02d-%02d", y, m + 1, d);

                    String[] months = {
                            "Jan","Feb","Mar","Apr","May","Jun",
                            "Jul","Aug","Sep","Oct","Nov","Dec"
                    };
                    tvSelectedDate.setText(months[m] + " " + d + ", " + y);
                    refreshSlotStatus();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ) {{
            getDatePicker().setMinDate(cal.getTimeInMillis());
        }}.show();
    }

    private void refreshSlotStatus() {
        if (tvSlotStatus == null) return;

        if (selectedDate == null || selectedTime == null) {
            tvSlotStatus.setVisibility(View.GONE);
            spinnerTimeSlot.setEnabled(true);
            return;
        }

        // Static time-based check — no network call needed
        boolean canBook = SlotUtils.isSlotBookingOpen(selectedDate, selectedTime);
        spinnerTimeSlot.setEnabled(canBook);

        if (!canBook) {
            tvSlotStatus.setText("Booking is now closed for this time - Pick other open time.");
            tvSlotStatus.setTextColor(Color.RED);
            tvSlotStatus.setVisibility(View.VISIBLE);
            return;
        }

        // Show a placeholder while Firestore loads
        tvSlotStatus.setText("Checking availability…");
        tvSlotStatus.setVisibility(View.VISIBLE);

        // CHANGED: async slot count from Firestore
        firestoreHelper.getSlotBookingCount(selectedTime, selectedDate,
                new FirestoreHelper.FirestoreCallback<Integer>() {
                    @Override
                    public void onSuccess(Integer slotBooked) {
                        int slotAvailable = MAX_SLOT_CAPACITY - slotBooked;

                        if (slotAvailable > 0) {
                            // Slot has space — show availability immediately
                            tvSlotStatus.setText(
                                    "☑ " + slotAvailable + " spot(s) open  ("
                                            + slotBooked + "/" + MAX_SLOT_CAPACITY + " booked)");
                            tvSlotStatus.setTextColor(
                                    getResources().getColor(R.color.primary_blue, null));
                            tvSlotStatus.setVisibility(View.VISIBLE);
                        } else {
                            // Slot is full — need waitlist count for the message
                            // CHANGED: async waitlist count from Firestore
                            firestoreHelper.getWaitlistCount(selectedTime, selectedDate,
                                    new FirestoreHelper.FirestoreCallback<Integer>() {
                                        @Override
                                        public void onSuccess(Integer waitlisted) {
                                            if (waitlisted > 0) {
                                                tvSlotStatus.setText(
                                                        "◔ Full - " + waitlisted
                                                                + " on waitlist. You'll be #"
                                                                + (waitlisted + 1));
                                                tvSlotStatus.setTextColor(
                                                        Color.parseColor("#FF8C00"));
                                            } else {
                                                tvSlotStatus.setText(
                                                        "🔴 Full — you'll be #1 on the waitlist");
                                                tvSlotStatus.setTextColor(Color.RED);
                                            }
                                            tvSlotStatus.setVisibility(View.VISIBLE);
                                        }

                                        @Override
                                        public void onFailure(String errorMessage) {
                                            tvSlotStatus.setText("🔴 Full — waitlist info unavailable");
                                            tvSlotStatus.setTextColor(Color.RED);
                                            tvSlotStatus.setVisibility(View.VISIBLE);
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        tvSlotStatus.setText("Could not load availability.");
                        tvSlotStatus.setTextColor(Color.GRAY);
                        tvSlotStatus.setVisibility(View.VISIBLE);
                    }
                });
    }


    private void confirmBooking() {
        if (selectedDate == null) {
            Toast.makeText(this, "Please select a date.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedTime == null) {
            Toast.makeText(this, "Please select a time slot.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!SlotUtils.isSlotBookingOpen(selectedDate, selectedTime)) {
            Toast.makeText(this,
                    "This slot has already started or is about to begin. " +
                            "Please choose another time.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        int splitIndex = spinnerSplit.getSelectedItemPosition();
        final String workoutSplit;

        if (splitIndex == CUSTOM_SPLIT_INDEX) {
            String custom = etCustomSplit.getText().toString().trim();
            if (custom.isEmpty()) {
                Toast.makeText(this, "Please describe your custom workout.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            workoutSplit = "Custom: " + custom;
        } else {
            workoutSplit = SPLIT_NAMES[splitIndex];
        }

        // Disable the button during async checks to prevent double-taps
        Button btnBook = findViewById(R.id.btnConfirmBooking);
        if (btnBook != null) btnBook.setEnabled(false);

        // ── Step 1: active booking guard ─────────────────────────────────────
        firestoreHelper.hasAnyActiveBooking(userId, new FirestoreHelper.FirestoreCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean hasActive) {
                if (hasActive) {
                    if (btnBook != null) btnBook.setEnabled(true);
                    showAlreadyBookedDialog();
                    return;
                }
                // ── Step 2: same-day duplicate guard ─────────────────────────
                firestoreHelper.isUserBookedOnDate(userId, selectedDate,
                        new FirestoreHelper.FirestoreCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean bookedOnDate) {
                                if (bookedOnDate) {
                                    if (btnBook != null) btnBook.setEnabled(true);
                                    Toast.makeText(BookingActivity.this,
                                            "You already have a booking on this date.",
                                            Toast.LENGTH_LONG).show();
                                    return;
                                }
                                // ── Step 3: duplicate waitlist guard ─────────
                                firestoreHelper.isUserWaitlistedForSlot(userId, selectedDate, selectedTime,
                                        new FirestoreHelper.FirestoreCallback<Boolean>() {
                                            @Override
                                            public void onSuccess(Boolean alreadyWaitlisted) {
                                                if (alreadyWaitlisted) {
                                                    if (btnBook != null) btnBook.setEnabled(true);
                                                    Toast.makeText(BookingActivity.this,
                                                            "You're already on the waitlist for this slot.",
                                                            Toast.LENGTH_LONG).show();
                                                    return;
                                                }
                                                // ── Step 4 & 5: capacity check ────────────
                                                checkCapacityAndRoute(workoutSplit, btnBook);
                                            }

                                            @Override
                                            public void onFailure(String err) {
                                                if (btnBook != null) btnBook.setEnabled(true);
                                                Toast.makeText(BookingActivity.this,
                                                        "Could not verify waitlist status. Try again.",
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }

                            @Override
                            public void onFailure(String err) {
                                if (btnBook != null) btnBook.setEnabled(true);
                                Toast.makeText(BookingActivity.this,
                                        "Could not verify date availability. Try again.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            public void onFailure(String err) {
                if (btnBook != null) btnBook.setEnabled(true);
                Toast.makeText(BookingActivity.this,
                        "Could not check booking status. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void checkCapacityAndRoute(String workoutSplit, Button btnBook) {
        firestoreHelper.getSlotBookingCount(selectedTime, selectedDate,
                new FirestoreHelper.FirestoreCallback<Integer>() {
                    @Override
                    public void onSuccess(Integer slotBooked) {
                        boolean slotFull = slotBooked >= MAX_SLOT_CAPACITY;

                        firestoreHelper.getDailyBookingCount(selectedDate,
                                new FirestoreHelper.FirestoreCallback<Integer>() {
                                    @Override
                                    public void onSuccess(Integer dailyBooked) {
                                        if (btnBook != null) btnBook.setEnabled(true);
                                        boolean dayFull = dailyBooked >= MAX_DAILY_CAPACITY;

                                        pendingWorkoutType = workoutSplit;

                                        if (slotFull || dayFull) {
                                            // Route to waitlist — no payment required
                                            addToWaitlist(workoutSplit);
                                        } else {
                                            // Route to payment
                                            launchPayment(workoutSplit);
                                        }
                                    }

                                    @Override
                                    public void onFailure(String err) {
                                        if (btnBook != null) btnBook.setEnabled(true);
                                        Toast.makeText(BookingActivity.this,
                                                "Could not check daily capacity. Try again.",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }

                    @Override
                    public void onFailure(String err) {
                        if (btnBook != null) btnBook.setEnabled(true);
                        Toast.makeText(BookingActivity.this,
                                "Could not check slot capacity. Try again.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // WAITLIST PATH


    private void addToWaitlist(String workoutSplit) {
        firestoreHelper.addToWaitlistWithWorkout(userId, selectedTime, selectedDate, workoutSplit,
                new FirestoreHelper.FirestoreCallback<String>() {
                    @Override
                    public void onSuccess(String bookingId) {
                        refreshSlotStatus();

                        // Build a Booking object just for the confirmation screen
                        Booking booking = new Booking();
                        booking.setId(bookingId);           // String Firestore ID
                        booking.setUserIdStr(userId);
                        booking.setWorkoutType(workoutSplit);
                        booking.setTimeSlot(selectedTime);
                        booking.setSelectedDate(selectedDate);
                        booking.setStatus("waitlisted");

                        NotificationHelper.notifyWaitlisted(BookingActivity.this, selectedDate, selectedTime, booking.getQueuePosition());

                        showWaitlistDialog(booking);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(BookingActivity.this,
                                "Could not add to waitlist: " + errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // PAYMENT PATH

    private void launchPayment(String workoutSplit) {
        Intent payIntent = new Intent(this, PaymentActivity.class);
        payIntent.putExtra("workout_type",  workoutSplit);
        payIntent.putExtra("selected_date", selectedDate);
        payIntent.putExtra("time_slot",     selectedTime);
        payIntent.putExtra("member_name",
                sharedPreferences.getString("full_name", "Member"));
        startActivityForResult(payIntent, REQUEST_PAYMENT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PAYMENT) return;

        if (resultCode == RESULT_OK && data != null) {
            String paymentStatus    = data.getStringExtra("payment_status");
            String paymentReference = data.getStringExtra("payment_reference");
            String paymentMethod    = data.getStringExtra("payment_method");

            if ("paid".equals(paymentStatus)) {
                saveBookingAndShowQr(paymentReference, paymentMethod);
                return;
            }
        }

        Toast.makeText(this, "Payment cancelled. Booking was not saved.",
                Toast.LENGTH_SHORT).show();
    }


    private void saveBookingAndShowQr(String ref, String method) {
        if (pendingWorkoutType == null || selectedDate == null || selectedTime == null) {
            Toast.makeText(this, "Booking details are incomplete.", Toast.LENGTH_SHORT).show();
            return;
        }

        firestoreHelper.createBookingWithPayment(
                userId, selectedTime, selectedDate, pendingWorkoutType, ref, method,
                new FirestoreHelper.FirestoreCallback<String>() {
                    @Override
                    public void onSuccess(String bookingId) {
                        refreshSlotStatus();

                        Booking booking = new Booking();
                        booking.setId(bookingId);       // String Firestore ID
                        booking.setUserIdStr(userId);
                        booking.setWorkoutType(pendingWorkoutType);
                        booking.setTimeSlot(selectedTime);
                        booking.setSelectedDate(selectedDate);
                        booking.setStatus("confirmed");
                        booking.setPaymentMethod(method);
                        booking.setPaymentReference(ref);

                        NotificationHelper.notifyBookingConfirmed(BookingActivity.this, selectedDate, selectedTime);

                        showQrDialog(booking);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(BookingActivity.this,
                                "The slot is no longer available. Booking was not saved.",
                                Toast.LENGTH_LONG).show();
                        refreshSlotStatus();
                    }
                });
    }

    // CONFIRMATION SCREENS

    private void showWaitlistDialog(Booking booking) {
        Intent intent = new Intent(this, BookingConfirmedActivity.class);
        intent.putExtra(BookingConfirmedActivity.EXTRA_BOOKING_ID,   booking.getFirestoreId());
        intent.putExtra(BookingConfirmedActivity.EXTRA_USER_ID,      booking.getUserIdStr());
        intent.putExtra(BookingConfirmedActivity.EXTRA_WORKOUT_TYPE, booking.getWorkoutType());
        intent.putExtra(BookingConfirmedActivity.EXTRA_DATE,         booking.getSelectedDate());
        intent.putExtra(BookingConfirmedActivity.EXTRA_TIME_SLOT,    booking.getTimeSlot());
        intent.putExtra(BookingConfirmedActivity.EXTRA_IS_WAITLIST,  true);
        intent.putExtra(BookingConfirmedActivity.EXTRA_QUEUE_POS,    booking.getQueuePosition());
        startActivity(intent);
        finish();
    }

    private void showQrDialog(Booking booking) {
        Intent intent = new Intent(this, BookingConfirmedActivity.class);
        intent.putExtra(BookingConfirmedActivity.EXTRA_BOOKING_ID,   booking.getFirestoreId());
        intent.putExtra(BookingConfirmedActivity.EXTRA_USER_ID,      booking.getUserIdStr());
        intent.putExtra(BookingConfirmedActivity.EXTRA_WORKOUT_TYPE, booking.getWorkoutType());
        intent.putExtra(BookingConfirmedActivity.EXTRA_DATE,         booking.getSelectedDate());
        intent.putExtra(BookingConfirmedActivity.EXTRA_TIME_SLOT,    booking.getTimeSlot());
        intent.putExtra(BookingConfirmedActivity.EXTRA_IS_WAITLIST,  false);
        startActivity(intent);
        finish();
    }

    // BOTTOM NAV

    private void setupBottomNav(String activeTab) {
        LinearLayout tabBook       = findViewById(R.id.tabBook);
        LinearLayout tabMyBookings = findViewById(R.id.tabMyBookings);
        LinearLayout tabWorkouts   = findViewById(R.id.tabWorkouts);
        LinearLayout tabBmi        = findViewById(R.id.tabBmi);
        LinearLayout tabProfile    = findViewById(R.id.tabProfile);

        highlightTab(tabBook,       "book".equals(activeTab));
        highlightTab(tabMyBookings, "mybookings".equals(activeTab));
        highlightTab(tabWorkouts,   "workouts".equals(activeTab));
        highlightTab(tabBmi,        "bmi".equals(activeTab));
        highlightTab(tabProfile,    "profile".equals(activeTab));

        tabBook.setOnClickListener(v       -> navigateToTab(activeTab, "book",       BookingActivity.class));
        tabMyBookings.setOnClickListener(v -> navigateToTab(activeTab, "mybookings", BookingHistoryActivity.class));
        tabWorkouts.setOnClickListener(v   -> navigateToTab(activeTab, "workouts",   WorkoutHistoryActivity.class));
        tabBmi.setOnClickListener(v        -> navigateToTab(activeTab, "bmi",        BMIActivity.class));
        tabProfile.setOnClickListener(v    -> navigateToTab(activeTab, "profile",    ProfileActivity.class));
    }

    private void navigateToTab(String activeTab, String targetTab, Class<?> activityClass) {
        if (targetTab.equals(activeTab)) return;
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
}