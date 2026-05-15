package com.example.spottermobile.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.spottermobile.model.Booking;
import com.example.spottermobile.model.User;
import com.example.spottermobile.model.WorkoutHistory;
import com.example.spottermobile.notifications.NotificationSender;
import com.example.spottermobile.utils.PasswordUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME    = "SpotterMobile.db";
    private static final int    DATABASE_VERSION = 7; // bumped: daily cap, removed slot logic

    private static final String TABLE_USERS           = "users";
    private static final String TABLE_BOOKINGS        = "bookings";
    private static final String TABLE_WORKOUT_HISTORY = "workout_history";
    private static final String TABLE_WAITLIST = "waitlist";

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
    // Waitlist-only columns
    private static final String COLUMN_SLOT_KEY       = "slot_key";       // "yyyy-MM-dd|HH:mm aa"
    private static final String COLUMN_QUEUE_POSITION = "queue_position"; // FIFO order within a slot

    // 30 people max per day (no per-slot limit anymore)
    public static final int MAX_DAILY_CAPACITY = 30;
    public static final int MAX_SLOT_CAPACITY = 30;
    /** Grace period in minutes after slot start. User must check in within this window. */
    public static final int GRACE_PERIOD_MINUTES = 15; //added_3

    // Booking statuses
    public static final String STATUS_BOOKED      = "booked";
    public static final String STATUS_CANCELLED   = "cancelled";
    public static final String STATUS_CHECKED_IN  = "checked_in";
    public static final String STATUS_COMPLETED   = "completed";
    public static final String STATUS_WAITLISTED  = "waitlisted"; //added_2
    public static final String STATUS_NO_SHOW     = "no_show"; //added_3

    /**
     * ACTIVE statuses for capacity counting = CONFIRMED (booked) + CHECKED_IN only.
     * CANCELLED, NO_SHOW, COMPLETED, and WAITLISTED are intentionally excluded.
     */
    private static final String[] ACTIVE_STATUSES = {STATUS_BOOKED, STATUS_CHECKED_IN};

    private final Context context;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context.getApplicationContext();
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


        db.execSQL("CREATE TABLE " + TABLE_WAITLIST + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_USER_ID + " INTEGER,"
                + COLUMN_SLOT_KEY + " TEXT,"            // "yyyy-MM-dd|HH:mm aa"
                + COLUMN_SELECTED_DATE + " TEXT,"       // for date-only queries
                + COLUMN_TIME_SLOT + " TEXT,"           // intended arrival time
                + COLUMN_WORKOUT_TYPE + " TEXT,"        // intended workout split
                + COLUMN_QUEUE_POSITION + " INTEGER,"   // FIFO order in slot
                + COLUMN_CREATED_DATE + " TEXT" + ")");

        // Default admin account
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, "admin");
        // SECURITY: store SHA-256 hash, never plaintext
        values.put(COLUMN_PASSWORD, PasswordUtils.hashPassword("admin123"));
        values.put(COLUMN_ROLE, "admin");
        values.put(COLUMN_FULL_NAME, "Administrator");
        values.put(COLUMN_CREATED_DATE, getCurrentDate());
        db.insert(TABLE_USERS, null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // ADDITIVE MIGRATIONS ONLY — never DROP tables (that wipes all user data).
        // Each if-block is cumulative: a user jumping from v4 → v7 runs all three.

        if (oldVersion < 5) {
            // v5: added checkin_time and checkout_time columns to bookings
            db.execSQL("ALTER TABLE " + TABLE_BOOKINGS
                    + " ADD COLUMN " + COLUMN_CHECKIN_TIME + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_BOOKINGS
                    + " ADD COLUMN " + COLUMN_CHECKOUT_TIME + " TEXT");
        }

        if (oldVersion < 6) {
            // v6: added waitlist table
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_WAITLIST + "("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_USER_ID + " INTEGER,"
                    + COLUMN_SLOT_KEY + " TEXT,"
                    + COLUMN_SELECTED_DATE + " TEXT,"
                    + COLUMN_TIME_SLOT + " TEXT,"
                    + COLUMN_WORKOUT_TYPE + " TEXT,"
                    + COLUMN_QUEUE_POSITION + " INTEGER,"
                    + COLUMN_CREATED_DATE + " TEXT)");
        }

        if (oldVersion < 7) {
            // v7: daily cap introduced — no schema change needed, just logic update.
            // Placeholder: add any v7 schema changes here if needed in future.
        }
        // Future bumps: add new if (oldVersion < N) { } blocks here.
        // Never use DROP TABLE — use ALTER TABLE ADD COLUMN or CREATE TABLE IF NOT EXISTS.
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

    /**
     * Authenticates a user by username OR email against a SHA-256 hashed password.
     * The caller (LoginActivity) is responsible for hashing the raw password before
     * passing it here — this method never receives or stores plaintext passwords.
     *
     * @param identifier   username or email address typed by the user
     * @param hashedPassword SHA-256 hex string produced by PasswordUtils.hashPassword()
     */
    public User loginUser(String identifier, String hashedPassword) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null,
                "(" + COLUMN_USERNAME + "=? OR " + COLUMN_EMAIL + "=?) AND "
                        + COLUMN_PASSWORD + "=?",
                new String[]{identifier, identifier, hashedPassword},
                null, null, null);
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
     * Returns total ACTIVE bookings for a date.
     * ACTIVE = CONFIRMED (booked) + CHECKED_IN only.
     * CANCELLED, NO_SHOW, COMPLETED, and WAITLISTED are NOT counted.
     * Used to enforce the daily cap.
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
     * Returns ACTIVE booking count for a specific date + timeSlot pair.
     * ACTIVE = CONFIRMED (booked) + CHECKED_IN only.
     * CANCELLED, NO_SHOW, COMPLETED, and WAITLISTED are NOT counted.
     */
    public int getSlotBookingCount(String selectedDate, String timeSlot) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_BOOKINGS,
                new String[]{"COUNT(*) AS cnt"},
                COLUMN_SELECTED_DATE + "=? AND " + COLUMN_TIME_SLOT + "=? AND ("
                        + COLUMN_STATUS + "=? OR " + COLUMN_STATUS + "=?)",
                new String[]{selectedDate, timeSlot, STATUS_BOOKED, STATUS_CHECKED_IN},
                null, null, null);
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        db.close();
        return count;
    }


    /**
     * Returns true if the user has an ACTIVE booking (CONFIRMED/booked or CHECKED_IN).
     *
     * CANCELLED, NO_SHOW, COMPLETED, and WAITLISTED are intentionally excluded —
     * a user who cancelled must be free to book again.
     *
     * Waitlist membership is checked separately in addBooking() via
     * isUserWaitlistedForSlot(), so TABLE_WAITLIST is NOT queried here.
     * Including it would wrongly block a cancelled user who once had a waitlist entry.
     */
    public boolean hasAnyActiveBooking(int userId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_BOOKINGS, new String[]{"COUNT(*) AS cnt"},
                COLUMN_USER_ID + "=? AND (" + COLUMN_STATUS + "=? OR " + COLUMN_STATUS + "=?)",
                new String[]{String.valueOf(userId), STATUS_BOOKED, STATUS_CHECKED_IN},
                null, null, null);
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        db.close();
        return count > 0;
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



    /** Returns true if the user is already queued for this exact date + timeSlot. */
    public boolean isUserWaitlistedForSlot(int userId, String selectedDate, String timeSlot) {
        String slotKey = selectedDate + "|" + timeSlot;
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_WAITLIST, null,
                COLUMN_USER_ID + "=? AND " + COLUMN_SLOT_KEY + "=?",
                new String[]{String.valueOf(userId), slotKey}, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }


    public Booking addBooking(Booking booking) {

        // Block if user already has a confirmed/checked-in booking OR is on any waitlist
        if (hasAnyActiveBooking(booking.getUserId())) return null;

        // Extra guard: block if user is already waitlisted for this exact slot
        if (isUserWaitlistedForSlot(booking.getUserId(),
                booking.getSelectedDate(), booking.getTimeSlot())) return null;

        // Only count CONFIRMED (booked) and CHECKED_IN bookings — never CANCELLED,
        // NO_SHOW, COMPLETED, or WAITLISTED — when evaluating slot/day capacity.
        boolean slotFull = getSlotBookingCount(booking.getSelectedDate(), booking.getTimeSlot())
                >= MAX_SLOT_CAPACITY;
        boolean dayFull  = getDailyBookingCount(booking.getSelectedDate())
                >= MAX_DAILY_CAPACITY;

        if (!slotFull && !dayFull) {

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
            NotificationSender.confirmed(context, booking.getSelectedDate(), booking.getTimeSlot());
            return booking;
        } else {
            Booking waitlisted = addToWaitlist(booking);
            if (waitlisted != null) {
                NotificationSender.waitlisted(context,
                        waitlisted.getSelectedDate(),
                        waitlisted.getTimeSlot(),
                        waitlisted.getQueuePosition());
            }
            return waitlisted;
        }
    }

    /**
     * Cancels a confirmed booking. If the slot hasn't started yet,
     * automatically promotes the first user on that slot's waitlist.
     */
    public boolean cancelBooking(int bookingId, int userId) {
        Booking booking = getBookingById(bookingId);
        if (booking == null) return false;
        if (booking.getUserId() != userId) return false;
        if (!STATUS_BOOKED.equals(booking.getStatus())
                && !STATUS_CHECKED_IN.equals(booking.getStatus())) return false;

        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_STATUS, STATUS_CANCELLED);
        int rows = db.update(TABLE_BOOKINGS, values,
                COLUMN_ID + "=? AND " + COLUMN_USER_ID + "=?",
                new String[]{String.valueOf(bookingId), String.valueOf(userId)});
        db.close();

        if (rows > 0) {
            NotificationSender.cancelled(context,
                    booking.getSelectedDate(), booking.getTimeSlot());

            if (PHASE_ACTIVE.equals(getBookingPhase(
                    booking.getSelectedDate(), booking.getTimeSlot()))) {
                promoteFirstWaitlisted(booking.getSelectedDate(), booking.getTimeSlot());
            }
        }
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
        Cursor cursor = db.query(TABLE_BOOKINGS,
                new String[]{COLUMN_STATUS, COLUMN_SELECTED_DATE, COLUMN_TIME_SLOT},
                COLUMN_ID + "=?", new String[]{String.valueOf(bookingId)}, null, null, null);
        if (!cursor.moveToFirst()) { cursor.close(); db.close(); return false; }
        String status       = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS));
        String date         = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SELECTED_DATE));
        String slot         = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME_SLOT));
        cursor.close();

        if (!STATUS_BOOKED.equals(status)) { db.close(); return false; }

        // Block check-in if grace period has already expired
        String phase = getBookingPhase(date, slot);
        if (PHASE_LOCKED.equals(phase)) { db.close(); return false; }

        ContentValues values = new ContentValues();
        values.put(COLUMN_STATUS,       STATUS_CHECKED_IN);
        values.put(COLUMN_CHECKIN_TIME, getCurrentDate());

        int rows = db.update(TABLE_BOOKINGS, values,
                COLUMN_ID + "=?", new String[]{String.valueOf(bookingId)});
        db.close();
        if (rows > 0) {
            NotificationSender.checkedIn(context, date, slot);
        }
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

    // ── TIME-LOCK PHASES ───────────────────────────────────────────────────────

    /**
     * Returns the current phase of a slot based on the current time.
     *
     * ACTIVE      → slot hasn't started yet (bookings, cancellations, promotions allowed)
     * GRACE       → slot has started but grace period hasn't expired (check-in allowed, no promotions)
     * LOCKED      → grace period has expired (no check-ins, no promotions, no cancellations)
     */
    public static final String PHASE_ACTIVE = "active";
    public static final String PHASE_GRACE  = "grace";
    public static final String PHASE_LOCKED = "locked";

    public String getBookingPhase(String selectedDate, String timeSlot) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm aa", Locale.getDefault());
            Date slotStart = sdf.parse(selectedDate + " " + timeSlot);
            if (slotStart == null) return PHASE_ACTIVE;

            Date now        = new Date();
            Date graceEnd   = new Date(slotStart.getTime() + GRACE_PERIOD_MINUTES * 60 * 1000L);

            if (now.before(slotStart)) return PHASE_ACTIVE;
            if (now.before(graceEnd))  return PHASE_GRACE;
            return PHASE_LOCKED;

        } catch (ParseException e) {
            return PHASE_ACTIVE; // conservative fallback
        }
    }
    /**
     * Scans all bookings with status BOOKED for a given date+timeSlot
     * and marks any that are still un-checked-in after the grace period as NO_SHOW.
     *
     * Call this: when admin opens the booking list, or on a scheduled check.
     * Returns the number of bookings marked as NO_SHOW.
     */
    public int markNoShows(String selectedDate, String timeSlot) {
        if (!PHASE_LOCKED.equals(getBookingPhase(selectedDate, timeSlot))) return 0;

        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_STATUS, STATUS_NO_SHOW);

        int rows = db.update(TABLE_BOOKINGS, values,
                COLUMN_SELECTED_DATE + "=? AND " + COLUMN_TIME_SLOT + "=? AND "
                        + COLUMN_STATUS + "=?",
                new String[]{selectedDate, timeSlot, STATUS_BOOKED});
        db.close();
        return rows;
    }
    // ── WAITLIST ───────────────────────────────────────────────────────────────

    /**
     * Enqueues a user on the FIFO waitlist for their requested slot.
     * Queue position = current max position for that slot + 1 (starts at 1).
     * Returns a Booking with status WAITLISTED and queuePosition set, or null on error.
     */
    public Booking addToWaitlist(Booking booking) {
        String slotKey = booking.getSelectedDate() + "|" + booking.getTimeSlot();

        if (isUserWaitlistedForSlot(booking.getUserId(),
                booking.getSelectedDate(), booking.getTimeSlot())) return null;

        SQLiteDatabase db = getWritableDatabase();

        // Determine next position in this slot's queue
        Cursor cursor = db.query(TABLE_WAITLIST,
                new String[]{"MAX(" + COLUMN_QUEUE_POSITION + ") AS max_pos"},
                COLUMN_SLOT_KEY + "=?", new String[]{slotKey},
                null, null, null);
        int nextPos = 1;
        if (cursor.moveToFirst() && !cursor.isNull(0)) {
            nextPos = cursor.getInt(0) + 1;
        }
        cursor.close();

        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID,        booking.getUserId());
        values.put(COLUMN_SLOT_KEY,       slotKey);
        values.put(COLUMN_SELECTED_DATE,  booking.getSelectedDate());
        values.put(COLUMN_TIME_SLOT,      booking.getTimeSlot());
        values.put(COLUMN_WORKOUT_TYPE,   booking.getWorkoutType());
        values.put(COLUMN_QUEUE_POSITION, nextPos);
        values.put(COLUMN_CREATED_DATE,   getCurrentDate());

        long newId = db.insert(TABLE_WAITLIST, null, values);
        db.close();
        if (newId == -1) return null;

        booking.setId((int) newId);
        booking.setStatus(STATUS_WAITLISTED);
        booking.setQueuePosition(nextPos);
        return booking;
    }

    /**
     * Promotes the first (lowest queue_position) waitlisted user for a slot
     * into a confirmed booking, then removes them from the waitlist.
     * Called automatically by cancelBooking() when the slot hasn't started.
     */
    public Booking promoteFirstWaitlisted(String selectedDate, String timeSlot) {
        String slotKey = selectedDate + "|" + timeSlot;
        SQLiteDatabase db = getWritableDatabase();

        Cursor cursor = db.query(TABLE_WAITLIST, null,
                COLUMN_SLOT_KEY + "=?", new String[]{slotKey},
                null, null, COLUMN_QUEUE_POSITION + " ASC", "1");

        if (!cursor.moveToFirst()) { cursor.close(); db.close(); return null; }

        int    waitlistRowId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
        int    promoteeId    = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID));
        String workoutType   = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WORKOUT_TYPE));
        cursor.close();

        // Re-check capacity in case another cancellation already filled the slot
        Cursor slotCursor = db.query(TABLE_BOOKINGS,
                new String[]{"COUNT(*) AS cnt"},
                COLUMN_SELECTED_DATE + "=? AND " + COLUMN_TIME_SLOT + "=? AND ("
                        + COLUMN_STATUS + "=? OR " + COLUMN_STATUS + "=?)",
                new String[]{selectedDate, timeSlot, STATUS_BOOKED, STATUS_CHECKED_IN},
                null, null, null);
        int slotCount = 0;
        if (slotCursor.moveToFirst()) slotCount = slotCursor.getInt(0);
        slotCursor.close();

        Cursor dayCursor = db.query(TABLE_BOOKINGS,
                new String[]{"COUNT(*) AS cnt"},
                COLUMN_SELECTED_DATE + "=? AND (" + COLUMN_STATUS + "=? OR " + COLUMN_STATUS + "=?)",
                new String[]{selectedDate, STATUS_BOOKED, STATUS_CHECKED_IN},
                null, null, null);
        int dayCount = 0;
        if (dayCursor.moveToFirst()) dayCount = dayCursor.getInt(0);
        dayCursor.close();

        if (slotCount >= MAX_SLOT_CAPACITY || dayCount >= MAX_DAILY_CAPACITY) {
            db.close();
            return null; // still full — leave waitlist intact
        }

        // Insert confirmed booking
        ContentValues bookingValues = new ContentValues();
        bookingValues.put(COLUMN_USER_ID,       promoteeId);
        bookingValues.put(COLUMN_WORKOUT_TYPE,  workoutType);
        bookingValues.put(COLUMN_TIME_SLOT,     timeSlot);
        bookingValues.put(COLUMN_SELECTED_DATE, selectedDate);
        bookingValues.put(COLUMN_CREATED_DATE,  getCurrentDate());
        bookingValues.put(COLUMN_STATUS,        STATUS_BOOKED);
        long newBookingId = db.insert(TABLE_BOOKINGS, null, bookingValues);

        if (newBookingId == -1) { db.close(); return null; }

        // Remove from waitlist
        db.delete(TABLE_WAITLIST, COLUMN_ID + "=?",
                new String[]{String.valueOf(waitlistRowId)});
        db.close();

        Booking promoted = new Booking();
        promoted.setId((int) newBookingId);
        promoted.setUserId(promoteeId);
        promoted.setWorkoutType(workoutType);
        promoted.setTimeSlot(timeSlot);
        promoted.setSelectedDate(selectedDate);
        promoted.setStatus(STATUS_BOOKED);
        NotificationSender.promoted(context, selectedDate, timeSlot);
        return promoted;
    }

    /** Returns all waitlist entries for a slot, ordered FIFO. For admin use. */
    public List<Booking> getWaitlistForSlot(String selectedDate, String timeSlot) {
        List<Booking> entries = new ArrayList<>();
        String slotKey = selectedDate + "|" + timeSlot;
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_WAITLIST, null,
                COLUMN_SLOT_KEY + "=?", new String[]{slotKey},
                null, null, COLUMN_QUEUE_POSITION + " ASC");
        while (cursor.moveToNext()) entries.add(cursorToWaitlistBooking(cursor));
        cursor.close();
        db.close();
        return entries;
    }

    /** Returns all waitlist entries for a user across all slots. */
    public List<Booking> getUserWaitlistEntries(int userId) {
        List<Booking> entries = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_WAITLIST, null,
                COLUMN_USER_ID + "=?", new String[]{String.valueOf(userId)},
                null, null, COLUMN_SELECTED_DATE + " ASC, " + COLUMN_QUEUE_POSITION + " ASC");
        while (cursor.moveToNext()) entries.add(cursorToWaitlistBooking(cursor));
        cursor.close();
        db.close();
        return entries;
    }

    /** Removes a user from a waitlist slot. Does not trigger promotion. */
    public boolean removeFromWaitlist(int waitlistId, int userId) {
        SQLiteDatabase db = getWritableDatabase();
        int rows = db.delete(TABLE_WAITLIST,
                COLUMN_ID + "=? AND " + COLUMN_USER_ID + "=?",
                new String[]{String.valueOf(waitlistId), String.valueOf(userId)});
        db.close();
        return rows > 0;
    }

    private Booking cursorToWaitlistBooking(Cursor cursor) {
        Booking b = new Booking();
        b.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
        b.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)));
        b.setWorkoutType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WORKOUT_TYPE)));
        b.setTimeSlot(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME_SLOT)));
        b.setSelectedDate(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SELECTED_DATE)));
        b.setBookingDate(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_DATE)));
        b.setStatus(STATUS_WAITLISTED);
        b.setQueuePosition(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_QUEUE_POSITION)));
        return b;
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

    // ── ADMIN STATS ────────────────────────────────────────────────────────────

    /** Count of members with role = "user" (excludes admin accounts). */
    public int getTotalMemberCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{"COUNT(*) AS cnt"},
                COLUMN_ROLE + "=?", new String[]{"user"}, null, null, null);
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        db.close();
        return count;
    }

    /** Count of bookings with status BOOKED or CHECKED_IN for today. */
    public int getTodayBookingCount() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        return getDailyBookingCount(today);
    }

    /** Count of bookings currently in CHECKED_IN status (across all dates). */
    public int getCurrentlyCheckedInCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_BOOKINGS, new String[]{"COUNT(*) AS cnt"},
                COLUMN_STATUS + "=?", new String[]{STATUS_CHECKED_IN}, null, null, null);
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        db.close();
        return count;
    }

    /** Count of NO_SHOW bookings for today. */
    public int getTodayNoShowCount() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_BOOKINGS, new String[]{"COUNT(*) AS cnt"},
                COLUMN_SELECTED_DATE + "=? AND " + COLUMN_STATUS + "=?",
                new String[]{today, STATUS_NO_SHOW}, null, null, null);
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        db.close();
        return count;
    }

    /** Count of active waitlist entries for today. */
    public int getTodayWaitlistCount() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_WAITLIST, new String[]{"COUNT(*) AS cnt"},
                COLUMN_SELECTED_DATE + "=?", new String[]{today}, null, null, null);
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        db.close();
        return count;
    }

    /**
     * Scans all LOCKED slots and marks eligible bookings as NO_SHOW.
     * Called by NoShowWorker (WorkManager) every 30 minutes.
     * Returns total number of bookings marked across all processed slots.
     */
    public int markNoShowsForAllLockedSlots() {
        // Collect distinct date+slot pairs that have BOOKED entries
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_BOOKINGS,
                new String[]{"DISTINCT " + COLUMN_SELECTED_DATE, COLUMN_TIME_SLOT},
                COLUMN_STATUS + "=?", new String[]{STATUS_BOOKED},
                null, null, null);

        List<String[]> slots = new ArrayList<>();
        while (cursor.moveToNext()) {
            slots.add(new String[]{
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SELECTED_DATE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME_SLOT))
            });
        }
        cursor.close();
        db.close();

        int total = 0;
        for (String[] slot : slots) {
            total += markNoShows(slot[0], slot[1]);
        }
        return total;
    }

    // ── UTILS ─────────────────────────────────────────────────────────────────
    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date());
    }
}