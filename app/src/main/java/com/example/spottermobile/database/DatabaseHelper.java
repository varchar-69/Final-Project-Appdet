package com.example.spottermobile.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.spottermobile.model.Booking;
import com.example.spottermobile.model.Feedback;
import com.example.spottermobile.model.User;
import com.example.spottermobile.model.WorkoutHistory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "SpotterMobile.db";
    // FIX: Bumped to 2 so onUpgrade() rebuilds the schema with the new columns
    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_USERS = "users";
    private static final String TABLE_BOOKINGS = "bookings";
    private static final String TABLE_FEEDBACK = "feedback";
    private static final String TABLE_WORKOUT_HISTORY = "workout_history";

    private static final String COLUMN_ID = "id";
    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_CREATED_DATE = "created_date";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_ROLE = "role";
    private static final String COLUMN_WORKOUT_TYPE = "workout_type";
    private static final String COLUMN_TIME_SLOT = "time_slot";
    private static final String COLUMN_STATUS = "status";
    private static final String COLUMN_RATING = "rating";
    private static final String COLUMN_COMMENT = "comment";
    private static final String COLUMN_WORKOUT_NAME = "workout_name";
    private static final String COLUMN_DURATION = "duration";
    private static final String COLUMN_CALORIES = "calories";

    // FIX: Added all profile columns to match the User model
    private static final String COLUMN_FULL_NAME = "full_name";
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_GENDER = "gender";
    private static final String COLUMN_CONTACT = "contact_number";
    private static final String COLUMN_ADDRESS = "address";
    private static final String COLUMN_EMERGENCY_NAME = "emergency_contact_name";
    private static final String COLUMN_EMERGENCY_CONTACT = "emergency_contact_number";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // FIX: TABLE_USERS now includes all profile fields from the User model
        db.execSQL("CREATE TABLE " + TABLE_USERS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_USERNAME + " TEXT UNIQUE,"
                + COLUMN_PASSWORD + " TEXT,"
                + COLUMN_ROLE + " TEXT,"
                + COLUMN_FULL_NAME + " TEXT,"
                + COLUMN_EMAIL + " TEXT,"
                + COLUMN_GENDER + " TEXT,"
                + COLUMN_CONTACT + " TEXT,"
                + COLUMN_ADDRESS + " TEXT,"
                + COLUMN_EMERGENCY_NAME + " TEXT,"
                + COLUMN_EMERGENCY_CONTACT + " TEXT,"
                + COLUMN_CREATED_DATE + " TEXT" + ")");

        db.execSQL("CREATE TABLE " + TABLE_BOOKINGS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_USER_ID + " INTEGER,"
                + COLUMN_WORKOUT_TYPE + " TEXT,"
                + COLUMN_TIME_SLOT + " TEXT,"
                + COLUMN_CREATED_DATE + " TEXT,"
                + COLUMN_STATUS + " TEXT" + ")");

        db.execSQL("CREATE TABLE " + TABLE_FEEDBACK + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_USER_ID + " INTEGER,"
                + COLUMN_RATING + " REAL,"
                + COLUMN_COMMENT + " TEXT,"
                + COLUMN_CREATED_DATE + " TEXT" + ")");

        db.execSQL("CREATE TABLE " + TABLE_WORKOUT_HISTORY + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_USER_ID + " INTEGER,"
                + COLUMN_WORKOUT_NAME + " TEXT,"
                + COLUMN_DURATION + " INTEGER,"
                + COLUMN_CALORIES + " INTEGER,"
                + COLUMN_CREATED_DATE + " TEXT" + ")");

        // Default admin
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, "admin");
        values.put(COLUMN_PASSWORD, "admin123");
        values.put(COLUMN_ROLE, "admin");
        values.put(COLUMN_FULL_NAME, "Administrator");
        values.put(COLUMN_CREATED_DATE, getCurrentDate());
        db.insert(TABLE_USERS, null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKINGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FEEDBACK);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WORKOUT_HISTORY);
        onCreate(db);
    }

    // ── USERS ──────────────────────────────────────────────────────────────────

    // FIX: registerUser now accepts and saves all User model fields
    public boolean registerUser(User user) {
        if (isUserExists(user.getUsername())) return false;
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, user.getUsername());
        values.put(COLUMN_PASSWORD, user.getPassword());
        values.put(COLUMN_ROLE, user.getRole() != null ? user.getRole() : "user");
        values.put(COLUMN_FULL_NAME, user.getFullName());
        values.put(COLUMN_EMAIL, user.getEmail());
        values.put(COLUMN_GENDER, user.getGender());
        values.put(COLUMN_CONTACT, user.getContactNumber());
        values.put(COLUMN_ADDRESS, user.getAddress());
        values.put(COLUMN_EMERGENCY_NAME, user.getEmergencyContactName());
        values.put(COLUMN_EMERGENCY_CONTACT, user.getEmergencyContactNumber());
        values.put(COLUMN_CREATED_DATE, getCurrentDate());
        long result = db.insert(TABLE_USERS, null, values);
        db.close();
        return result != -1;
    }

    public User loginUser(String username, String password) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null,
                COLUMN_USERNAME + "=? AND " + COLUMN_PASSWORD + "=?",
                new String[]{username, password}, null, null, null);

        User user = null;
        if (cursor.moveToFirst()) {
            user = cursorToUser(cursor); // FIX: use shared helper
        }
        cursor.close();
        db.close();
        return user;
    }

    public boolean isUserExists(String username) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null,
                COLUMN_USERNAME + "=?", new String[]{username},
                null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    // FIX: Added getUserById() — this is what ProfileActivity calls
    public User getUserById(int userId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null,
                COLUMN_ID + "=?",
                new String[]{String.valueOf(userId)},
                null, null, null);

        User user = null;
        if (cursor.moveToFirst()) {
            user = cursorToUser(cursor);
        }
        cursor.close();
        db.close();
        return user;
    }

    // FIX: getAllUsers() now maps all profile fields via shared helper
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null, null, null, null, null,
                COLUMN_ID + " ASC");

        while (cursor.moveToNext()) {
            users.add(cursorToUser(cursor));
        }
        cursor.close();
        db.close();
        return users;
    }

    // FIX: Shared cursor-to-User mapping — maps all fields from User model
    private User cursorToUser(Cursor cursor) {
        User user = new User();
        user.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
        user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)));
        user.setPassword(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD)));
        user.setRole(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROLE)));
        user.setCreatedDate(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_DATE)));
        user.setFullName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FULL_NAME)));
        user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)));
        user.setGender(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER)));
        user.setContactNumber(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTACT)));
        user.setAddress(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS)));
        user.setEmergencyContactName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMERGENCY_NAME)));
        user.setEmergencyContactNumber(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMERGENCY_CONTACT)));
        return user;
    }

    // ── BOOKINGS ───────────────────────────────────────────────────────────────

    public boolean addBooking(Booking booking) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, booking.getUserId());
        values.put(COLUMN_WORKOUT_TYPE, booking.getWorkoutType());
        values.put(COLUMN_TIME_SLOT, booking.getTimeSlot());
        values.put(COLUMN_CREATED_DATE, getCurrentDate());
        values.put(COLUMN_STATUS, booking.getStatus());
        long result = db.insert(TABLE_BOOKINGS, null, values);
        db.close();
        return result != -1;
    }

    public List<Booking> getUserBookings(int userId) {
        List<Booking> bookings = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_BOOKINGS, null,
                COLUMN_USER_ID + "=?",
                new String[]{String.valueOf(userId)},
                null, null, COLUMN_ID + " DESC");

        while (cursor.moveToNext()) {
            bookings.add(cursorToBooking(cursor));
        }
        cursor.close();
        db.close();
        return bookings;
    }

    public List<Booking> getAllBookings() {
        List<Booking> bookings = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_BOOKINGS, null,
                null, null, null, null, COLUMN_ID + " DESC");

        while (cursor.moveToNext()) {
            bookings.add(cursorToBooking(cursor));
        }
        cursor.close();
        db.close();
        return bookings;
    }

    public boolean cancelBooking(int bookingId, int userId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_STATUS, "cancelled");
        int rows = db.update(TABLE_BOOKINGS, values,
                COLUMN_ID + "=? AND " + COLUMN_USER_ID + "=?",
                new String[]{String.valueOf(bookingId), String.valueOf(userId)});
        db.close();
        return rows > 0;
    }


    private Booking cursorToBooking(Cursor cursor) {
        Booking booking = new Booking();
        booking.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
        booking.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)));
        booking.setWorkoutType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WORKOUT_TYPE)));
        booking.setTimeSlot(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME_SLOT)));
        booking.setBookingDate(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_DATE)));
        booking.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS)));
        return booking;
    }

    // ── FEEDBACK ───────────────────────────────────────────────────────────────

    public boolean addFeedback(Feedback feedback) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, feedback.getUserId());
        values.put(COLUMN_RATING, feedback.getRating());
        values.put(COLUMN_COMMENT, feedback.getComment());
        values.put(COLUMN_CREATED_DATE, getCurrentDate());
        long result = db.insert(TABLE_FEEDBACK, null, values);
        db.close();
        return result != -1;
    }

    // ── WORKOUT HISTORY ────────────────────────────────────────────────────────

    public boolean addWorkoutHistory(WorkoutHistory history) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, history.getUserId());
        values.put(COLUMN_WORKOUT_NAME, history.getWorkoutName());
        values.put(COLUMN_DURATION, history.getDuration());
        values.put(COLUMN_CALORIES, history.getCalories());
        values.put(COLUMN_CREATED_DATE, getCurrentDate());
        long result = db.insert(TABLE_WORKOUT_HISTORY, null, values);
        db.close();
        return result != -1;
    }

    // ── UTILS ──────────────────────────────────────────────────────────────────

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date());
    }
}

