package com.example.spottermobile.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.spottermobile.model.Booking;
import com.example.spottermobile.model.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "SpotterMobile.db";
    private static final int DATABASE_VERSION = 2; // ← bumped from 1 to 2

    // Users Table
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_ROLE = "role";
    private static final String COLUMN_CREATED_DATE = "created_date";

    // Bookings Table  ← ADD THESE
    private static final String TABLE_BOOKINGS = "bookings";
    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_WORKOUT_TYPE = "workout_type";
    private static final String COLUMN_TIME_SLOT = "time_slot";
    private static final String COLUMN_GYM_BRANCH = "gym_branch";
    private static final String COLUMN_BOOKING_DATE = "booking_date";
    private static final String COLUMN_STATUS = "status";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Users table (unchanged)
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_USERNAME + " TEXT UNIQUE,"
                + COLUMN_PASSWORD + " TEXT,"
                + COLUMN_ROLE + " TEXT,"
                + COLUMN_CREATED_DATE + " TEXT" + ")";
        db.execSQL(CREATE_USERS_TABLE);

        // Bookings table  ← ADD THIS
        String CREATE_BOOKINGS_TABLE = "CREATE TABLE " + TABLE_BOOKINGS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_USER_ID + " INTEGER,"
                + COLUMN_WORKOUT_TYPE + " TEXT,"
                + COLUMN_TIME_SLOT + " TEXT,"
                + COLUMN_GYM_BRANCH + " TEXT,"
                + COLUMN_BOOKING_DATE + " TEXT,"
                + COLUMN_STATUS + " TEXT" + ")";
        db.execSQL(CREATE_BOOKINGS_TABLE);

        // Insert default admin
        insertDefaultAdmin(db);
    }

    private void insertDefaultAdmin(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, "admin");
        values.put(COLUMN_PASSWORD, "admin123");
        values.put(COLUMN_ROLE, "admin");
        values.put(COLUMN_CREATED_DATE, getCurrentDate());
        db.insert(TABLE_USERS, null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // If upgrading from v1 → v2, just create the bookings table (don't wipe users!)
        if (oldVersion < 2) {
            String CREATE_BOOKINGS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_BOOKINGS + "("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_USER_ID + " INTEGER,"
                    + COLUMN_WORKOUT_TYPE + " TEXT,"
                    + COLUMN_TIME_SLOT + " TEXT,"
                    + COLUMN_GYM_BRANCH + " TEXT,"
                    + COLUMN_BOOKING_DATE + " TEXT,"
                    + COLUMN_STATUS + " TEXT" + ")";
            db.execSQL(CREATE_BOOKINGS_TABLE);
        }
    }

    // ─── User Operations ────────────────────────────────────────────────────────

    public boolean registerUser(String username, String password, String role) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_PASSWORD, password);
        values.put(COLUMN_ROLE, role);
        values.put(COLUMN_CREATED_DATE, getCurrentDate());

        long result = db.insert(TABLE_USERS, null, values);
        db.close();
        return result != -1;
    }

    public User loginUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null,
                COLUMN_USERNAME + "=? AND " + COLUMN_PASSWORD + "=?",
                new String[]{username, password}, null, null, null);

        User user = null;
        if (cursor.moveToFirst()) {
            user = new User();
            user.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
            user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)));
            user.setPassword(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD)));
            user.setRole(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROLE)));
            user.setCreatedDate(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_DATE)));
        }
        cursor.close();
        db.close();
        return user;
    }

    public boolean isUserExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null,
                COLUMN_USERNAME + "=?", new String[]{username}, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    // ─── Booking Operations  ← ADD THESE ────────────────────────────────────────

    public boolean addBooking(int userId, String workoutType, String timeSlot, String gymBranch) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, userId);
        values.put(COLUMN_WORKOUT_TYPE, workoutType);
        values.put(COLUMN_TIME_SLOT, timeSlot);
        values.put(COLUMN_GYM_BRANCH, gymBranch);
        values.put(COLUMN_STATUS, "Confirmed");
        values.put(COLUMN_BOOKING_DATE, getCurrentDate());

        long result = db.insert(TABLE_BOOKINGS, null, values);
        db.close();
        return result != -1;
    }

    public List<Booking> getUserBookings(int userId) {
        List<Booking> bookings = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_BOOKINGS, null,
                COLUMN_USER_ID + "=?", new String[]{String.valueOf(userId)},
                null, null, COLUMN_BOOKING_DATE + " DESC");

        if (cursor.moveToFirst()) {
            do {
                Booking booking = new Booking();
                booking.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                booking.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)));
                booking.setWorkoutType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WORKOUT_TYPE)));
                booking.setTimeSlot(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME_SLOT)));
                booking.setGymBranch(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GYM_BRANCH)));
                booking.setBookingDate(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BOOKING_DATE)));
                booking.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS)));
                bookings.add(booking);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return bookings;
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }
}