package com.example.spottermobile.database;

import android.util.Log;

import com.example.spottermobile.model.Booking;
import com.example.spottermobile.model.User;
import com.example.spottermobile.model.WorkoutHistory;
import com.example.spottermobile.utils.SlotUtils;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class FirestoreHelper {

    private static final String TAG = "FirestoreHelper";

    private final FirebaseFirestore db;

    // Collection names
    private static final String USERS_COLLECTION    = "users";
    private static final String BOOKINGS_COLLECTION = "bookings";
    private static final String WAITLIST_COLLECTION = "waitlist";
    private static final String HISTORY_COLLECTION  = "workoutHistory";

    public FirestoreHelper() {
        db = FirebaseFirestore.getInstance();
    }

    // ==================== USER OPERATIONS ====================

    public void seedAdminIfNotExists() {
        db.collection("users")
                .whereEqualTo("username", "admin")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        Map<String, Object> admin = new HashMap<>();
                        admin.put("username",   "admin");
                        admin.put("password",   "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9");
                        admin.put("userType",   "admin");
                        admin.put("role",       "admin");
                        admin.put("fullName",   "Administrator");
                        admin.put("email",      "admin@spotter.com");
                        admin.put("isSuspended", false);
                        admin.put("createdAt",  System.currentTimeMillis());
                        db.collection("users").add(admin);
                    }
                });
    }

    /**
     * Register a new user.
     * FIX: added gender, contactNumber, address, emergencyContactName,
     * emergencyContactNumber — these were missing so profile fields showed
     * null for all registered users.
     */
    public void registerUser(String name, String email, String username,
                             String hashedPassword, String userType,
                             String gender, String contactNumber,
                             String address, String emergencyContactName,
                             String emergencyContactNumber,
                             FirestoreCallback<String> callback) {
        Map<String, Object> user = new HashMap<>();
        user.put("name",                    name);
        user.put("fullName",                name);
        user.put("email",                   email);
        user.put("username",                username);
        user.put("password",                hashedPassword);
        user.put("userType",                userType);
        user.put("gender",                  gender != null ? gender : "");
        user.put("contactNumber",           contactNumber != null ? contactNumber : "");
        user.put("address",                 address != null ? address : "");
        user.put("emergencyContactName",    emergencyContactName != null ? emergencyContactName : "");
        user.put("emergencyContactNumber",  emergencyContactNumber != null ? emergencyContactNumber : "");
        user.put("isSuspended",             false);
        user.put("createdAt",               System.currentTimeMillis());

        db.collection(USERS_COLLECTION)
                .add(user)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "User registered: " + docRef.getId());
                    callback.onSuccess(docRef.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error registering user", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Check if username already exists.
     */
    public void isUserExists(String username, FirestoreCallback<Boolean> callback) {
        db.collection(USERS_COLLECTION)
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(snapshot -> callback.onSuccess(!snapshot.isEmpty()))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking user existence", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Login user — find by username and password.
     */
    public void loginUser(String identifier, String hashedPassword,
                          FirestoreCallback<User> callback) {
        db.collection(USERS_COLLECTION)
                .whereEqualTo("username", identifier)
                .whereEqualTo("password", hashedPassword)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        User user = docToUser(snapshot.getDocuments().get(0));
                        if (user != null) callback.onSuccess(user);
                        else              callback.onFailure("Failed to parse user");
                    } else {
                        callback.onFailure("User not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error logging in", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Get user by Firestore document ID.
     */
    public void getUserById(String userId, FirestoreCallback<User> callback) {
        db.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        User user = docToUser(doc);
                        if (user != null) callback.onSuccess(user);
                        else              callback.onFailure("Failed to parse user");
                    } else {
                        callback.onFailure("User not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Get all users (for admin).
     */
    public void getAllUsers(FirestoreCallback<List<User>> callback) {
        db.collection(USERS_COLLECTION)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<User> users = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        User user = docToUser(doc);
                        if (user != null) users.add(user);
                    }
                    callback.onSuccess(users);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting all users", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Count users whose userType == "member".
     */
    public void getMemberCount(FirestoreCallback<Integer> callback) {
        db.collection(USERS_COLLECTION)
                .whereEqualTo("userType", "member")
                .get()
                .addOnSuccessListener(snapshot -> callback.onSuccess(snapshot.size()))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting member count", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Suspend a user (set isSuspended = true).
     */
    public void suspendUser(String userId, FirestoreCallback<Void> callback) {
        db.collection(USERS_COLLECTION)
                .document(userId)
                .update("isSuspended", true)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User suspended: " + userId);
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error suspending user", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Unsuspend a user (set isSuspended = false).
     */
    public void unsuspendUser(String userId, FirestoreCallback<Void> callback) {
        db.collection(USERS_COLLECTION)
                .document(userId)
                .update("isSuspended", false)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User unsuspended: " + userId);
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error unsuspending user", e);
                    callback.onFailure(e.getMessage());
                });
    }

    // ==================== BOOKING OPERATIONS ====================

    /**
     * Add a booking with capacity check.
     */
    public void addBooking(String userId, String timeSlot, String date, String gymName,
                           FirestoreCallback<String> callback) {
        db.collection(BOOKINGS_COLLECTION)
                .whereEqualTo("timeSlot", timeSlot)
                .whereEqualTo("date", date)
                .whereIn("status", Arrays.asList("confirmed", "checked_in"))
                .get()
                .addOnSuccessListener(slotSnapshot -> {
                    int bookedCount = slotSnapshot.size();

                    db.collection(BOOKINGS_COLLECTION)
                            .whereEqualTo("userId", userId)
                            .whereEqualTo("date", date)
                            .whereIn("status", Arrays.asList("confirmed", "checked_in"))
                            .get()
                            .addOnSuccessListener(dailySnapshot -> {
                                int dailyCount = dailySnapshot.size();

                                String status = "confirmed";
                                if (bookedCount >= 2 || dailyCount >= 1) {
                                    status = "waitlisted";
                                }

                                final String finalStatus = status;
                                String docId = db.collection(BOOKINGS_COLLECTION).document().getId();

                                Map<String, Object> booking = new HashMap<>();
                                booking.put("userId",        userId);
                                booking.put("timeSlot",      timeSlot);
                                booking.put("date",          date);
                                booking.put("gymName",       gymName);
                                booking.put("status",        finalStatus);
                                booking.put("paymentStatus", "pending");
                                booking.put("checkinTime",   0);
                                booking.put("checkoutTime",  0);
                                booking.put("createdAt",     System.currentTimeMillis());

                                db.collection(BOOKINGS_COLLECTION).document(docId)
                                        .set(booking)
                                        .addOnSuccessListener(v -> {
                                            Log.d(TAG, "Booking added: " + docId);
                                            callback.onSuccess(docId);
                                        })
                                        .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                            })
                            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Get a single booking by its Firestore document ID.
     */
    public void getBookingById(String bookingId, FirestoreCallback<Booking> callback) {
        db.collection(BOOKINGS_COLLECTION)
                .document(bookingId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Booking b = docToBooking(doc);
                        if (b != null) callback.onSuccess(b);
                        else           callback.onFailure("Failed to parse booking");
                    } else {
                        callback.onFailure("Booking not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting booking by ID", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Fetch the current status string of a booking doc.
     */
    public void getBookingStatus(String bookingId, FirestoreCallback<String> callback) {
        db.collection(BOOKINGS_COLLECTION)
                .document(bookingId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String status = doc.getString("status");
                        callback.onSuccess(status != null ? status : "");
                    } else {
                        callback.onFailure("Booking not found: " + bookingId);
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Get all bookings for a user, newest first.
     */
    public void getUserBookings(String userId, FirestoreCallback<List<Booking>> callback) {
        db.collection(BOOKINGS_COLLECTION)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Booking> bookings = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Booking b = docToBooking(doc);
                        if (b != null) bookings.add(b);
                    }
                    callback.onSuccess(bookings);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user bookings", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Get bookings by status for a specific user.
     */
    public void getBookingsByStatus(String userId, String status,
                                    FirestoreCallback<List<Booking>> callback) {
        db.collection(BOOKINGS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", status)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Booking> bookings = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Booking b = docToBooking(doc);
                        if (b != null) bookings.add(b);
                    }
                    callback.onSuccess(bookings);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting bookings by status", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Fetch ALL bookings across all users with a given status.
     */
    public void getAllBookingsByStatus(String status, FirestoreCallback<List<Booking>> callback) {
        db.collection(BOOKINGS_COLLECTION)
                .whereEqualTo("status", status)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Booking> bookings = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Booking b = docToBooking(doc);
                        if (b != null) bookings.add(b);
                    }
                    callback.onSuccess(bookings);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getAllBookingsByStatus failed", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Count bookings on a given date that match any of the provided statuses.
     */
    public void getBookingsByDateAndStatuses(String date, List<String> statuses,
                                             FirestoreCallback<Integer> callback) {
        db.collection(BOOKINGS_COLLECTION)
                .whereEqualTo("date", date)
                .whereIn("status", statuses)
                .get()
                .addOnSuccessListener(snapshot -> callback.onSuccess(snapshot.size()))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting bookings by date+statuses", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Returns true if the user has any booking with status "confirmed" or "checked_in".
     */
    public void hasAnyActiveBooking(String userId, FirestoreCallback<Boolean> callback) {
        db.collection(BOOKINGS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereIn("status", Arrays.asList("confirmed", "checked_in"))
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> callback.onSuccess(!snapshot.isEmpty()))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking active booking", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Returns true if the user already has a booking on the given date.
     */
    public void isUserBookedOnDate(String userId, String date,
                                   FirestoreCallback<Boolean> callback) {
        db.collection(BOOKINGS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("date",   date)
                .whereIn("status",      Arrays.asList("confirmed", "checked_in", "waitlisted"))
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> callback.onSuccess(!snapshot.isEmpty()))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking date booking", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Returns true if the user is already on the waitlist for a specific slot.
     */
    public void isUserWaitlistedForSlot(String userId, String date, String timeSlot,
                                        FirestoreCallback<Boolean> callback) {
        db.collection(WAITLIST_COLLECTION)
                .whereEqualTo("userId",   userId)
                .whereEqualTo("date",     date)
                .whereEqualTo("timeSlot", timeSlot)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> callback.onSuccess(!snapshot.isEmpty()))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking waitlist", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Get count of confirmed/checked-in bookings for a specific slot.
     */
    public void getSlotBookingCount(String timeSlot, String date,
                                    FirestoreCallback<Integer> callback) {
        db.collection(BOOKINGS_COLLECTION)
                .whereEqualTo("timeSlot", timeSlot)
                .whereEqualTo("date",     date)
                .whereIn("status",        Arrays.asList("confirmed", "checked_in"))
                .get()
                .addOnSuccessListener(snapshot -> callback.onSuccess(snapshot.size()))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting slot booking count", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Get count of confirmed/checked-in bookings for the whole day.
     */
    public void getDailyBookingCount(String date, FirestoreCallback<Integer> callback) {
        db.collection(BOOKINGS_COLLECTION)
                .whereEqualTo("date",  date)
                .whereIn("status",     Arrays.asList("confirmed", "checked_in"))
                .get()
                .addOnSuccessListener(snapshot -> callback.onSuccess(snapshot.size()))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting daily booking count", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Cancel a booking and promote the first waitlisted user for that slot (if any).
     */
    public void cancelBooking(String bookingId, FirestoreCallback<Void> callback) {
        db.collection(BOOKINGS_COLLECTION).document(bookingId).get()
                .addOnSuccessListener(bookingDoc -> {
                    if (!bookingDoc.exists()) {
                        callback.onFailure("Booking not found");
                        return;
                    }
                    String timeSlot = bookingDoc.getString("timeSlot");
                    String date     = bookingDoc.getString("date");

                    db.collection(BOOKINGS_COLLECTION)
                            .whereEqualTo("timeSlot", timeSlot)
                            .whereEqualTo("date",     date)
                            .whereEqualTo("status",   "waitlisted") //new
                            .limit(1)
                            .get()
                            .addOnSuccessListener(waitlistSnapshot -> {
                                WriteBatch batch = db.batch();

                                batch.update(
                                        db.collection(BOOKINGS_COLLECTION).document(bookingId),
                                        "status", "cancelled"
                                );

                                if (!waitlistSnapshot.isEmpty()) {
                                    DocumentSnapshot waitlistDoc =
                                            waitlistSnapshot.getDocuments().get(0);
                                    String waitlistedUserId = waitlistDoc.getString("userId");

                                    Map<String, Object> promotion = new HashMap<>();
                                    promotion.put("userId",        waitlistedUserId);
                                    promotion.put("timeSlot",      timeSlot);
                                    promotion.put("date",          date);
                                    promotion.put("gymName",       bookingDoc.get("gymName"));
                                    promotion.put("status",        "confirmed");
                                    promotion.put("paymentStatus", "pending");
                                    promotion.put("checkinTime",   0);
                                    promotion.put("checkoutTime",  0);
                                    promotion.put("createdAt",     System.currentTimeMillis());

                                    String newBookingId =
                                            db.collection(BOOKINGS_COLLECTION).document().getId();
                                    batch.set(
                                            db.collection(BOOKINGS_COLLECTION).document(newBookingId),
                                            promotion
                                    );
                                    batch.delete(waitlistDoc.getReference());
                                }

                                batch.commit()
                                        .addOnSuccessListener(v -> {
                                            Log.d(TAG, "Booking cancelled: " + bookingId);
                                            callback.onSuccess(null);
                                        })
                                        .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                            })
                            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Check in a booking.
     */
    public void checkInBooking(String bookingId, FirestoreCallback<Void> callback) {
        db.collection(BOOKINGS_COLLECTION)
                .document(bookingId)
                .update(
                        "status",      "checked_in",
                        "checkinTime", System.currentTimeMillis()
                )
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Booking checked in: " + bookingId);
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking in booking", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Check out a booking and record workout history.
     */
    public void checkOutBooking(String bookingId, FirestoreCallback<Void> callback) {
        long checkoutTime = System.currentTimeMillis();
        db.runTransaction(transaction -> {
            DocumentSnapshot bookingDoc = transaction.get(
                    db.collection(BOOKINGS_COLLECTION).document(bookingId)
            );

            transaction.update(
                    db.collection(BOOKINGS_COLLECTION).document(bookingId),
                    "status",       "completed",
                    "checkoutTime", checkoutTime
            );

            Map<String, Object> history = new HashMap<>();
            history.put("userId",     bookingDoc.get("userId"));
            history.put("date",       bookingDoc.get("date"));
            history.put("timeSlot",   bookingDoc.get("timeSlot"));
            history.put("gymName",    bookingDoc.get("gymName"));
            history.put("duration",   checkoutTime - (long) bookingDoc.get("checkinTime"));
            history.put("recordedAt", System.currentTimeMillis());

            String historyId = db.collection(HISTORY_COLLECTION).document().getId();
            transaction.set(db.collection(HISTORY_COLLECTION).document(historyId), history);

            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Booking checked out: " + bookingId);
            callback.onSuccess(null);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error checking out booking", e);
            callback.onFailure(e.getMessage());
        });
    }

    /**
     * Create a confirmed booking with payment info in a single Firestore write.
     */
    public void createBookingWithPayment(String userId, String timeSlot, String date,
                                         String workoutType, String paymentReference,
                                         String paymentMethod,
                                         FirestoreCallback<String> callback) {
        Map<String, Object> booking = new HashMap<>();
        booking.put("userId",           userId);
        booking.put("timeSlot",         timeSlot);
        booking.put("date",             date);
        booking.put("gymName",          workoutType);
        booking.put("status",           "confirmed");
        booking.put("paymentStatus",    "paid");
        booking.put("paymentReference", paymentReference != null ? paymentReference : "");
        booking.put("paymentMethod",    paymentMethod    != null ? paymentMethod    : "");
        booking.put("checkinTime",      0);
        booking.put("checkoutTime",     0);
        booking.put("createdAt",        System.currentTimeMillis());

        db.collection(BOOKINGS_COLLECTION)
                .add(booking)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "Booking created: " + docRef.getId());
                    callback.onSuccess(docRef.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating booking", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Mark a booking as paid.
     */
    public void markBookingAsPaid(String bookingId, FirestoreCallback<Void> callback) {
        db.collection(BOOKINGS_COLLECTION)
                .document(bookingId)
                .update("paymentStatus", "paid")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Booking marked as paid: " + bookingId);
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error marking booking as paid", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Marks no-shows for all slots whose grace period has expired.
     * Called by NoShowWorker every 30 minutes.
     */
    public void markNoShowsForAllLockedSlots(FirestoreCallback<Integer> callback) {
        db.collection(BOOKINGS_COLLECTION)
                .whereEqualTo("status", "confirmed")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onSuccess(0);
                        return;
                    }

                    List<DocumentSnapshot> expired = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String date     = doc.getString("date");
                        String timeSlot = doc.getString("timeSlot");
                        if (date == null || timeSlot == null) continue;

                        Calendar slotStart = SlotUtils.getSlotStartCalendar(date, timeSlot);
                        if (slotStart == null) continue;

                        Calendar graceEnd = (Calendar) slotStart.clone();
                        graceEnd.add(Calendar.MINUTE, SlotUtils.GRACE_PERIOD_MINUTES);

                        if (Calendar.getInstance().after(graceEnd)) {
                            expired.add(doc);
                        }
                    }

                    if (expired.isEmpty()) {
                        callback.onSuccess(0);
                        return;
                    }

                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : expired) {
                        batch.update(doc.getReference(), "status", "no_show");
                    }

                    batch.commit()
                            .addOnSuccessListener(v -> {
                                Log.d(TAG, "Marked " + expired.size() + " no-shows");
                                callback.onSuccess(expired.size());
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "markNoShowsForAllLockedSlots batch failed", e);
                                callback.onFailure(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "markNoShowsForAllLockedSlots query failed", e);
                    callback.onFailure(e.getMessage());
                });
    }

    //  WAITLIST OPERATIONS

    /**
     * Add user to waitlist (basic, no workout type).
     */
    public void addToWaitlist(String userId, String timeSlot, String date, String gymName,
                              FirestoreCallback<Void> callback) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("userId",   userId);
        entry.put("timeSlot", timeSlot);
        entry.put("date",     date);
        entry.put("gymName",  gymName);
        entry.put("addedAt",  System.currentTimeMillis());

        db.collection(WAITLIST_COLLECTION)
                .add(entry)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "User added to waitlist: " + userId);
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding to waitlist", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Add user to waitlist with workoutType, stored as a booking doc with
     * status="waitlisted". Used by BookingActivity.
     */
    public void addToWaitlistWithWorkout(String userId, String timeSlot, String date,
                                         String workoutType,
                                         FirestoreCallback<String> callback) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("userId",    userId);
        entry.put("timeSlot",  timeSlot);
        entry.put("date",      date);
        entry.put("gymName",   workoutType);
        entry.put("status",    "waitlisted");
        entry.put("addedAt",   System.currentTimeMillis());
        entry.put("createdAt", System.currentTimeMillis());

        db.collection(BOOKINGS_COLLECTION)
                .add(entry)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "Added to waitlist (booking doc): " + docRef.getId());
                    callback.onSuccess(docRef.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding to waitlist", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Get the ordered waitlist for a time slot.
     */
    public void getWaitlist(String timeSlot, String date,
                            FirestoreCallback<List<Map<String, Object>>> callback) {
        db.collection(WAITLIST_COLLECTION)
                .whereEqualTo("timeSlot", timeSlot)
                .whereEqualTo("date",     date)
                .orderBy("addedAt", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Map<String, Object>> waitlist = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Map<String, Object> e = doc.getData();
                        if (e != null) {
                            e.put("id", doc.getId());
                            waitlist.add(e);
                        }
                    }
                    callback.onSuccess(waitlist);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting waitlist", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Get count of waitlisted entries for a slot.
     */
    public void getWaitlistCount(String timeSlot, String date,
                                 FirestoreCallback<Integer> callback) {
        db.collection(WAITLIST_COLLECTION)
                .whereEqualTo("timeSlot", timeSlot)
                .whereEqualTo("date",     date)
                .get()
                .addOnSuccessListener(snapshot -> callback.onSuccess(snapshot.size()))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting waitlist count", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Delete the waitlist/booking entry for a (bookingId, userId) pair.
     */
    public void cancelWaitlistEntry(String bookingId, String userId,
                                    FirestoreCallback<Void> callback) {
        db.collection(WAITLIST_COLLECTION)
                .whereEqualTo("bookingId", bookingId)
                .whereEqualTo("userId",    userId)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onSuccess(null);
                        return;
                    }
                    snapshot.getDocuments().get(0).getReference()
                            .delete()
                            .addOnSuccessListener(v -> callback.onSuccess(null))
                            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ==================== ADMIN OPERATIONS ====================

    /**
     * Get all bookings with user name/email joined in (N+1 fetches).
     */
    public void getAllBookingsWithNames(FirestoreCallback<List<Map<String, Object>>> callback) {
        db.collection(BOOKINGS_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<DocumentSnapshot> docs = snapshot.getDocuments();
                    if (docs.isEmpty()) {
                        callback.onSuccess(new ArrayList<>());
                        return;
                    }

                    final List<Map<String, Object>> results = new CopyOnWriteArrayList<>();
                    final AtomicInteger remaining = new AtomicInteger(docs.size());

                    for (DocumentSnapshot doc : docs) {
                        String userId = doc.getString("userId");
                        db.collection(USERS_COLLECTION).document(userId).get()
                                .addOnSuccessListener(userDoc -> {
                                    Map<String, Object> data = doc.getData();
                                    if (data != null) {
                                        data.put("id",        doc.getId());
                                        data.put("userName",  userDoc.exists() ? userDoc.getString("fullName") : "Unknown");
                                        data.put("userEmail", userDoc.exists() ? userDoc.getString("email")    : "");
                                        results.add(data);
                                    }
                                    if (remaining.decrementAndGet() == 0)
                                        callback.onSuccess(new ArrayList<>(results));
                                })
                                .addOnFailureListener(e -> {
                                    Map<String, Object> data = doc.getData();
                                    if (data != null) {
                                        data.put("id",        doc.getId());
                                        data.put("userName",  "Unknown");
                                        data.put("userEmail", "");
                                        results.add(data);
                                    }
                                    if (remaining.decrementAndGet() == 0)
                                        callback.onSuccess(new ArrayList<>(results));
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting all bookings", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Get bookings filtered by status AND date with user names joined in.
     */
    public void getFilteredBookingsWithNames(String status, String date,
                                             FirestoreCallback<List<Map<String, Object>>> callback) {
        db.collection(BOOKINGS_COLLECTION)
                .whereEqualTo("status", status)
                .whereEqualTo("date",   date)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<DocumentSnapshot> docs = snapshot.getDocuments();
                    if (docs.isEmpty()) {
                        callback.onSuccess(new ArrayList<>());
                        return;
                    }

                    final List<Map<String, Object>> results = new CopyOnWriteArrayList<>();
                    final AtomicInteger remaining = new AtomicInteger(docs.size());

                    for (DocumentSnapshot doc : docs) {
                        String userId = doc.getString("userId");
                        db.collection(USERS_COLLECTION).document(userId).get()
                                .addOnSuccessListener(userDoc -> {
                                    Map<String, Object> data = doc.getData();
                                    if (data != null) {
                                        data.put("id",        doc.getId());
                                        data.put("userName",  userDoc.exists() ? userDoc.getString("fullName") : "Unknown");
                                        data.put("userEmail", userDoc.exists() ? userDoc.getString("email")    : "");
                                        results.add(data);
                                    }
                                    if (remaining.decrementAndGet() == 0)
                                        callback.onSuccess(new ArrayList<>(results));
                                })
                                .addOnFailureListener(e -> {
                                    Map<String, Object> data = doc.getData();
                                    if (data != null) {
                                        data.put("id",        doc.getId());
                                        data.put("userName",  "Unknown");
                                        data.put("userEmail", "");
                                        results.add(data);
                                    }
                                    if (remaining.decrementAndGet() == 0)
                                        callback.onSuccess(new ArrayList<>(results));
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting filtered bookings", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Get total revenue from paid bookings (₱200 per booking).
     */
    public void getTotalRevenue(FirestoreCallback<Double> callback) {
        db.collection(BOOKINGS_COLLECTION)
                .whereEqualTo("paymentStatus", "paid")
                .get()
                .addOnSuccessListener(snapshot -> callback.onSuccess(snapshot.size() * 200.0))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting revenue", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Get all paid bookings with user names joined in.
     */
    public void getPaidBookingsWithNames(FirestoreCallback<List<Map<String, Object>>> callback) {
        db.collection(BOOKINGS_COLLECTION)
                .whereEqualTo("paymentStatus", "paid")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<DocumentSnapshot> docs = snapshot.getDocuments();
                    if (docs.isEmpty()) {
                        callback.onSuccess(new ArrayList<>());
                        return;
                    }

                    final List<Map<String, Object>> results = new CopyOnWriteArrayList<>();
                    final AtomicInteger remaining = new AtomicInteger(docs.size());

                    for (DocumentSnapshot doc : docs) {
                        String userId = doc.getString("userId");
                        db.collection(USERS_COLLECTION).document(userId).get()
                                .addOnSuccessListener(userDoc -> {
                                    Map<String, Object> data = doc.getData();
                                    if (data != null) {
                                        data.put("id",        doc.getId());
                                        data.put("userName",  userDoc.exists() ? userDoc.getString("fullName") : "Unknown");
                                        data.put("userEmail", userDoc.exists() ? userDoc.getString("email")    : "");
                                        results.add(data);
                                    }
                                    if (remaining.decrementAndGet() == 0)
                                        callback.onSuccess(new ArrayList<>(results));
                                })
                                .addOnFailureListener(e -> {
                                    Map<String, Object> data = doc.getData();
                                    if (data != null) {
                                        data.put("id",        doc.getId());
                                        data.put("userName",  "Unknown");
                                        data.put("userEmail", "");
                                        results.add(data);
                                    }
                                    if (remaining.decrementAndGet() == 0)
                                        callback.onSuccess(new ArrayList<>(results));
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting paid bookings", e);
                    callback.onFailure(e.getMessage());
                });
    }

    // ==================== WORKOUT HISTORY ====================

    /**
     * Get workout history for a user, newest first.
     */
    public void getWorkoutHistory(String userId,
                                  FirestoreCallback<List<WorkoutHistory>> callback) {
        db.collection(HISTORY_COLLECTION)
                .whereEqualTo("userId", userId)
                .orderBy("recordedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<WorkoutHistory> histories = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        WorkoutHistory h = doc.toObject(WorkoutHistory.class);
                        if (h != null) {
                            h.setId(doc.getId());
                            histories.add(h);
                        }
                    }
                    callback.onSuccess(histories);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting workout history", e);
                    callback.onFailure(e.getMessage());
                });
    }

    // ==================== PRIVATE HELPERS ====================

    /**
     * FIX: manually map all User fields instead of relying on doc.toObject(User.class).
     * Firestore's auto-mapper silently returns null for fields whose Java name doesn't
     * exactly match the Firestore field name (e.g. isSuspended → "suspended" mismatch,
     * contactNumber, emergencyContactName etc. were never written before this fix).
     */
    private User docToUser(DocumentSnapshot doc) {
        if (!doc.exists()) return null;

        User user = new User();

        // ID — always the Firestore document ID
        user.setFirestoreId(doc.getId());

        // Core identity fields
        user.setUsername(doc.getString("username"));
        user.setEmail(doc.getString("email"));
        user.setPassword(doc.getString("password"));

        // fullName — stored as "fullName", fallback to legacy "name" field
        String fullName = doc.getString("fullName");
        if (fullName == null || fullName.isEmpty()) fullName = doc.getString("name");
        user.setFullName(fullName);

        // Profile fields — these were missing from registerUser before the fix
        user.setGender(doc.getString("gender"));
        user.setContactNumber(doc.getString("contactNumber"));
        user.setAddress(doc.getString("address"));
        user.setEmergencyContactName(doc.getString("emergencyContactName"));
        user.setEmergencyContactNumber(doc.getString("emergencyContactNumber"));

        // Role / userType
        String userType = doc.getString("userType");
        String role     = doc.getString("role");
        if (userType != null) {
            user.setUserType(userType);
        } else if (role != null) {
            // legacy docs only had "role"
            user.setUserType("admin".equals(role) ? "admin" : "member");
        }

        // isSuspended — Firestore stores as boolean field "isSuspended"
        // doc.toObject() maps it as "suspended" due to is-prefix stripping — read manually
        Boolean suspended = doc.getBoolean("isSuspended");
        user.setSuspended(suspended != null && suspended);

        return user;
    }

    /**
     * FIX: manually map Booking fields instead of doc.toObject(Booking.class)
     * to avoid crashes when checkinTime/checkoutTime are stored as Long vs String.
     */
    private Booking docToBooking(DocumentSnapshot doc) {
        Booking booking = new Booking();

        booking.setId(doc.getId());

        booking.setStatus(doc.getString("status"));
        booking.setTimeSlot(doc.getString("timeSlot"));
        booking.setWorkoutType(doc.getString("gymName"));
        booking.setBookingDate(doc.getString("bookingDate"));
        booking.setPaymentStatus(doc.getString("paymentStatus"));
        booking.setPaymentMethod(doc.getString("paymentMethod"));
        booking.setPaymentReference(doc.getString("paymentReference"));
        booking.setMemberName(doc.getString("memberName"));

        // selectedDate — stored as "date" or "selectedDate"
        String selectedDate = doc.getString("selectedDate");
        if (selectedDate == null) selectedDate = doc.getString("date");
        booking.setSelectedDate(selectedDate);

        // userId — String in Firestore
        String userIdStr = doc.getString("userId");
        if (userIdStr != null) booking.setUserIdStr(userIdStr);

        // checkinTime / checkoutTime — handle both Long (legacy) and String
        booking.setCheckinTime(readAsString(doc, "checkinTime"));
        booking.setCheckoutTime(readAsString(doc, "checkoutTime"));

        return booking;
    }

    /**
     * Safely reads a Firestore field as String regardless of stored type.
     * Long (epoch ms) → "yyyy-MM-dd HH:mm" formatted string.
     * 0L (default unset value) → null (treat as not set).
     */
    private String readAsString(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);
        if (value == null) return null;
        if (value instanceof String) return (String) value;
        if (value instanceof Long) {
            long ms = (Long) value;
            if (ms == 0) return null; // 0 = not set
            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US);
            return sdf.format(new java.util.Date(ms));
        }
        return value.toString();
    }

    // ==================== CALLBACK INTERFACE ====================

    public interface FirestoreCallback<T> {
        void onSuccess(T result);
        void onFailure(String errorMessage);
    }
}