package com.example.spottermobile.model;

public class Booking {
    private int id;
    private int userId;
    private String workoutType;
    private String timeSlot;
    private String bookingDate;  // auto-generated timestamp
    private String selectedDate; // user-chosen date (yyyy-MM-dd)
    private String status;

    public Booking() {}

    public Booking(int userId, String workoutType, String timeSlot, String selectedDate) {
        this.userId = userId;
        this.workoutType = workoutType;
        this.timeSlot = timeSlot;
        this.selectedDate = selectedDate;
        this.status = "booked";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getWorkoutType() { return workoutType; }
    public void setWorkoutType(String workoutType) { this.workoutType = workoutType; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    public String getBookingDate() { return bookingDate; }
    public void setBookingDate(String bookingDate) { this.bookingDate = bookingDate; }

    public String getSelectedDate() { return selectedDate; }
    public void setSelectedDate(String selectedDate) { this.selectedDate = selectedDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}