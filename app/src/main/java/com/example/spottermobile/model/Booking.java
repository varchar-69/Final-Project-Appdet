package com.example.spottermobile.model;

public class Booking {

    /*
     * Firestore document ID
     */
    private String firestoreId;

    /*
     * Legacy SQLite IDs (temporary compatibility)
     */
    private int id;
    private int userId;

    /*
     * Firestore user document ID
     */
    private String userIdStr;

    private String workoutType;
    private String timeSlot;
    private String bookingDate;
    private String selectedDate;
    private String status;

    private String checkinTime;
    private String checkoutTime;

    private String paymentMethod;
    private String paymentReference;
    private String paymentStatus;

    private String memberName;

    private int queuePosition;

    public Booking() {
        // Required empty constructor for Firestore
    }

    public Booking(
            int userId,
            String workoutType,
            String timeSlot,
            String selectedDate
    ) {

        this.userId = userId;
        this.workoutType = workoutType;
        this.timeSlot = timeSlot;
        this.selectedDate = selectedDate;
        this.status = "confirmed"; //was booked
    }


    public void setId(String id) {
        this.firestoreId = id;
    }

    public String getId() {
        return firestoreId;
    }

    public String getFirestoreId() {
        return firestoreId;
    }

    public void setFirestoreId(String id) {
        this.firestoreId = id;
    }


    public int getLegacyId() {
        return id;
    }

    public void setLegacyId(int id) {
        this.id = id;
    }


    public String getUserId() {
        return userIdStr;
    }

    public void setUserId(String userId) {
        this.userIdStr = userId;
    }

    public String getUserIdStr() {
        return userIdStr;
    }

    public void setUserIdStr(String userId) {
        this.userIdStr = userId;
    }

    public int getLegacyUserId() {
        return userId;
    }

    public void setLegacyUserId(int userId) {
        this.userId = userId;
    }


    public String getWorkoutType() {
        return workoutType;
    }

    public void setWorkoutType(String workoutType) {
        this.workoutType = workoutType;
    }


    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }


    public String getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(String bookingDate) {
        this.bookingDate = bookingDate;
    }


    public String getSelectedDate() {
        return selectedDate;
    }

    public void setSelectedDate(String selectedDate) {
        this.selectedDate = selectedDate;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public String getCheckinTime() {
        return checkinTime;
    }

    public void setCheckinTime(String checkinTime) {
        this.checkinTime = checkinTime;
    }


    public String getCheckoutTime() {
        return checkoutTime;
    }

    public void setCheckoutTime(String checkoutTime) {
        this.checkoutTime = checkoutTime;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }


    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }


    public int getQueuePosition() {
        return queuePosition;
    }

    public void setQueuePosition(int queuePosition) {
        this.queuePosition = queuePosition;
    }


    public boolean isWaitlisted() {
        return "waitlisted".equals(status);
    }
}