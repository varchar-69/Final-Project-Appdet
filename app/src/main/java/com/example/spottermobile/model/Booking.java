package com.example.spottermobile.model;

public class Booking {
    private int id;
    private int userId;
    private String workoutType;
    private String timeSlot;
    private String gymBranch;
    private String bookingDate;
    private String status;

    public Booking() {}

    public Booking(int userId, String workoutType, String timeSlot, String gymBranch) {
        this.userId = userId;
        this.workoutType = workoutType;
        this.timeSlot = timeSlot;
        this.gymBranch = gymBranch;
        this.status = "Confirmed";
        this.bookingDate = getCurrentDate();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getWorkoutType() { return workoutType; }
    public void setWorkoutType(String workoutType) { this.workoutType = workoutType; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    public String getGymBranch() { return gymBranch; }
    public void setGymBranch(String gymBranch) { this.gymBranch = gymBranch; }

    public String getBookingDate() { return bookingDate; }
    public void setBookingDate(String bookingDate) { this.bookingDate = bookingDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    private String getCurrentDate() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date());
    }
}