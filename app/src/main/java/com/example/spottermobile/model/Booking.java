package com.example.spottermobile.model;

public class Booking {
    private int    id;
    private int    userId;
    private String workoutType;   // stores split name or "Custom: ..."
    private String timeSlot;      // stores user-chosen arrival time
    private String bookingDate;   // auto-generated timestamp (created_date)
    private String selectedDate;  // user-chosen date (yyyy-MM-dd)
    private String status;
    private String checkinTime;
    private String checkoutTime;

    // Transient — populated only by getAllBookingsWithNames() JOIN query, not stored in DB
    private String memberName;

    public Booking() {}

    public Booking(int userId, String workoutType, String timeSlot, String selectedDate) {
        this.userId       = userId;
        this.workoutType  = workoutType;
        this.timeSlot     = timeSlot;
        this.selectedDate = selectedDate;
        this.status       = "booked";
    }

    public int    getId()                   { return id; }
    public void   setId(int id)             { this.id = id; }

    public int    getUserId()               { return userId; }
    public void   setUserId(int v)          { this.userId = v; }

    public String getWorkoutType()          { return workoutType; }
    public void   setWorkoutType(String v)  { this.workoutType = v; }

    public String getTimeSlot()             { return timeSlot; }
    public void   setTimeSlot(String v)     { this.timeSlot = v; }

    public String getBookingDate()          { return bookingDate; }
    public void   setBookingDate(String v)  { this.bookingDate = v; }

    public String getSelectedDate()         { return selectedDate; }
    public void   setSelectedDate(String v) { this.selectedDate = v; }

    public String getStatus()               { return status; }
    public void   setStatus(String v)       { this.status = v; }

    public String getCheckinTime()          { return checkinTime; }
    public void   setCheckinTime(String v)  { this.checkinTime = v; }

    public String getCheckoutTime()         { return checkoutTime; }
    public void   setCheckoutTime(String v) { this.checkoutTime = v; }

    public String getMemberName()           { return memberName; }
    public void   setMemberName(String v)   { this.memberName = v; }
}