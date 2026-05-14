package com.example.spottermobile.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.spottermobile.model.Booking;
import com.example.spottermobile.model.User;
import com.example.spottermobile.model.WorkoutHistory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME    = "SpotterMobile.db";
    private static final int    DATABASE_VERSION = 5; // bumped: daily cap, removed slot logic

    private static final String TABLE_USERS           = "users";
    private static final String TABLE_BOOKINGS        = "bookings";
    private static final String TABLE_FEEDBACK        = "feedback";
    private static final String TABLE_WORKOUT_HISTORY = "workout_history";

    private static final String COLUMN_ID                = "id";
    private static final String COLUMN_USER_ID           = "user_id";
    private static final String COLUMN_CREATED_DATE      = "created_date";
    private static final String COLUMN_USERNAME          = "username";
    private static final String COLUMN_PASSWORD          = "password";
    private static final String COLUMN_ROLE              = "role";
    private static final String COLUMN_WORKOUT_TYPE      = "workout_type";
    private static final String COLUMN_TIME_SLOT         = "time_slot";
    private static final String COLUMN_SELECTED_DATE     = "selected_date";
    private static final String COLUMN_STATUS            = "status";
    private static final String COLUMN_WORKOUT_NAME      = "workout_name";
    private static final String COLUMN_DURATION          = "duration";
    private static final String COLUMN_CALORIES          = "calories";
    private static final String COLUMN_FULL_NAME         = "full_name";
    private static final String COLUMN_EMAIL             = "email";
    private static final String COLUMN_GENDER            = "gender";
    private static final String COLUMN_CONTACT           = "contact_number";
    private static final String COLUMN_ADDRESS           = "address";
    private static final String COLUMN_EMERGENCY_NAME    = "emergency_contact_name";
    private static final String COLUMN_EMERGENCY_CONTACT = "emergency_contact_number";
    private static final String COLUMN_CHECKIN_TIME      = "checkin_time";
    private static final String COLUMN_CHECKOUT_TIME     = "checkout_time";

    // 30 people max per day (no per-slot limit anymore)
    public static final int MAX_DAILY_CAPACITY = 30;

    // Booking statuses
    public static final String STATUS_BOOKED      = "booked";
    public static final String STATUS_CANCELLED   = "cancelled";
    public static final String STATUS_CHECKED_IN  = "checked_in";
    public static final String STATUS_COMPLETED   = "completed";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
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
                + COLUMN_SELECTED_DATE + " TEXT,"
                + COLUMN_CREATED_DATE + " TEXT,"
                + COLUMN_STATUS + " TEXT,"
                + COLUMN_CHECKIN_TIME + " TEXT,"
                + COLUMN_CHECKOUT_TIME + " TEXT" + ")");

        db.execSQL("CREATE TABLE " + TABLE_WORKOUT_HISTORY + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_USER_ID + " INTEGER,"
                + COLUMN_WORKOUT_NAME + " TEXT,"
                + COLUMN_DURATION + " INTEGER,"
                + COLUMN_CALORIES + " INTEGER,"
                + COLUMN_CREATED_DATE + " TEXT" + ")");

        // Default admin account
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
        if (cursor.moveToFirst()) user = cursorToUser(cursor);
        cursor.close();
        db.close();
        return user;
    }

    public boolean isUserExists(String username) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null,
                COLUMN_USERNAME + "=?", new String[]{username}, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    public User getUserById(int userId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null,
                COLUMN_ID + "=?", new String[]{String.valueOf(userId)}, null, null, null);
        User user = null;
        if (cursor.moveToFirst()) user = cursorToUser(cursor);
        cursor.close();
        db.close();
        return user;
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null, null, null, null, null, COLUMN_ID + " ASC");
        while (cursor.moveToNext()) users.add(cursorToUser(cursor));
        cursor.close();
        db.close();
        return users;
    }

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

    /**
     * Returns total confirmed (booked + checked_in) bookings for a date.
     * Used to enforce the 30-person daily cap.
     */
    public int getDailyBookingCount(String selectedDate) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_BOOKINGS,
                new String[]{"COUNT(*) AS cnt"},
                COLUMN_SELECTED_DATE + "=? AND (" + COLUMN_STATUS + "=? OR " + COLUMN_STATUS + "=?)",
                new String[]{selectedDate, STATUS_BOOKED, STATUS_CHECKED_IN},
                null, null, null);
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        db.close();
        return count;
    }

    /**
     * Returns true if this user already has an active booking on the given date.
     */
    public boolean isUserBookedOnDate(int userId, String selectedDate) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_BOOKINGS, null,
                COLUMN_USER_ID + "=? AND " + COLUMN_SELECTED_DATE + "=? AND ("
                        + COLUMN_STATUS + "=? OR " + COLUMN_STATUS + "=?)",
                new String[]{String.valueOf(userId), selectedDate, STATUS_BOOKED, STATUS_CHECKED_IN},
                null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    /**
     * Adds a booking. Enforces 1-per-day and 30/day cap before inserting.
     * Returns the saved Booking with id set, or null on failure/violation.
     */
    public Booking addBooking(Booking booking) {
        if (isUserBookedOnDate(booking.getUserId(), booking.getSelectedDate())) return null;
        if (getDailyBookingCount(booking.getSelectedDate()) >= MAX_DAILY_CAPACITY) return null;

        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID,       booking.getUserId());
        values.put(COLUMN_WORKOUT_TYPE,  booking.getWorkoutType());
        values.put(COLUMN_TIME_SLOT,     booking.getTimeSlot());
        values.put(COLUMN_SELECTED_DATE, booking.getSelectedDate());
        values.put(COLUMN_CREATED_DATE,  getCurrentDate());
        values.put(COLUMN_STATUS,        STATUS_BOOKED);
        long newId = db.insert(TABLE_BOOKINGS, null, values);
        db.close();
        if (newId == -1) return null;
        booking.setId((int) newId);
        booking.setStatus(STATUS_BOOKED);
        return booking;
    }

    public boolean cancelBooking(int bookingId, int userId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_STATUS, STATUS_CANCELLED);
        int rows = db.update(TABLE_BOOKINGS, values,
                COLUMN_ID + "=? AND " + COLUMN_USER_ID + "=?",
                new String[]{String.valueOf(bookingId), String.valueOf(userId)});
        db.close();
        return rows > 0;
    }

    // ── CHECK-IN / CHECK-OUT ───────────────────────────────────────────────────

    public Booking getBookingById(int bookingId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_BOOKINGS, null,
                COLUMN_ID + "=?", new String[]{String.valueOf(bookingId)}, null, null, null);
        Booking booking = null;
        if (cursor.moveToFirst()) booking = cursorToBooking(cursor);
        cursor.close();
        db.close();
        return booking;
    }

    public boolean checkInBooking(int bookingId) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.query(TABLE_BOOKINGS, new String[]{COLUMN_STATUS},
                COLUMN_ID + "=?", new String[]{String.valueOf(bookingId)}, null, null, null);
        if (!cursor.moveToFirst()) { cursor.close(); db.close(); return false; }
        String status = cursor.getString(0);
        cursor.close();
        if (!STATUS_BOOKED.equals(status)) { db.close(); return false; }

        ContentValues values = new ContentValues();
        values.put(COLUMN_STATUS,       STATUS_CHECKED_IN);
        values.put(COLUMN_CHECKIN_TIME, getCurrentDate());
        int rows = db.update(TABLE_BOOKINGS, values,
                COLUMN_ID + "=?", new String[]{String.valueOf(bookingId)});
        db.close();
        return rows > 0;
    }

    public boolean checkOutBooking(int bookingId) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.query(TABLE_BOOKINGS, new String[]{COLUMN_STATUS},
                COLUMN_ID + "=?", new String[]{String.valueOf(bookingId)}, null, null, null);
        if (!cursor.moveToFirst()) { cursor.close(); db.close(); return false; }
        String status = cursor.getString(0);
        cursor.close();
        if (!STATUS_CHECKED_IN.equals(status)) { db.close(); return false; }

        ContentValues values = new ContentValues();
        values.put(COLUMN_STATUS,        STATUS_COMPLETED);
        values.put(COLUMN_CHECKOUT_TIME, getCurrentDate());
        int rows = db.update(TABLE_BOOKINGS, values,
                COLUMN_ID + "=?", new String[]{String.valueOf(bookingId)});
        db.close();
        return rows > 0;
    }

    // ── ADMIN: BOOKINGS WITH USERNAME ──────────────────────────────────────────

    /**
     * Returns all bookings joined with the users table so admin can see the member name.
     * Each Booking in the list has memberName set (via the transient field).
     */
    public List<Booking> getAllBookingsWithNames() {
        List<Booking> bookings = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        String query = "SELECT b.*, u." + COLUMN_USERNAME + " AS member_username, "
                + "u." + COLUMN_FULL_NAME + " AS member_fullname "
                + "FROM " + TABLE_BOOKINGS + " b "
                + "LEFT JOIN " + TABLE_USERS + " u ON b." + COLUMN_USER_ID + " = u." + COLUMN_ID
                + " ORDER BY b." + COLUMN_ID + " DESC";

        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            Booking booking = cursorToBooking(cursor);

            // Pull joined name columns
            int unIdx = cursor.getColumnIndex("member_username");
            int fnIdx = cursor.getColumnIndex("member_fullname");
            String username  = (unIdx != -1) ? cursor.getString(unIdx) : null;
            String fullName  = (fnIdx != -1) ? cursor.getString(fnIdx) : null;

            // Prefer full name; fall back to username
            String displayName = (fullName != null && !fullName.isEmpty())
                    ? fullName : (username != null ? username : "User #" + booking.getUserId());
            booking.setMemberName(displayName);

            bookings.add(booking);
        }
        cursor.close();
        db.close();
        return bookings;
    }

    // ── USER BOOKINGS ──────────────────────────────────────────────────────────

    public List<Booking> getUserBookings(int userId) {
        List<Booking> bookings = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_BOOKINGS, null,
                COLUMN_USER_ID + "=?", new String[]{String.valueOf(userId)},
                null, null, COLUMN_ID + " DESC");
        while (cursor.moveToNext()) bookings.add(cursorToBooking(cursor));
        cursor.close();
        db.close();
        return bookings;
    }

    public List<Booking> getAllBookings() {
        List<Booking> bookings = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_BOOKINGS, null, null, null, null, null, COLUMN_ID + " DESC");
        while (cursor.moveToNext()) bookings.add(cursorToBooking(cursor));
        cursor.close();
        db.close();
        return bookings;
    }

    private Booking cursorToBooking(Cursor cursor) {
        Booking booking = new Booking();
        booking.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
        booking.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)));
        booking.setWorkoutType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WORKOUT_TYPE)));
        booking.setTimeSlot(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME_SLOT)));
        booking.setSelectedDate(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SELECTED_DATE)));
        booking.setBookingDate(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_DATE)));
        booking.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS)));
        booking.setCheckinTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CHECKIN_TIME)));
        booking.setCheckoutTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CHECKOUT_TIME)));
        return booking;
    }

    // ── WORKOUT HISTORY ────────────────────────────────────────────────────────

    public boolean addWorkoutHistory(WorkoutHistory history) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID,      history.getUserId());
        values.put(COLUMN_WORKOUT_NAME, history.getWorkoutName());
        values.put(COLUMN_DURATION,     history.getDuration());
        values.put(COLUMN_CALORIES,     history.getCalories());
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