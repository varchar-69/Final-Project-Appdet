package com.example.spottermobile.model;

public class Booking {
    private int    id;
    private int    userId;
    private String workoutType;
    private String timeSlot;
    private String bookingDate;   // auto-generated timestamp (created_date)
    private String selectedDate;  // user-chosen date (yyyy-MM-dd)
    private String status;
    private String checkinTime;   // NEW — set when admin scans QR for check-in
    private String checkoutTime;  // NEW — set when admin scans QR for check-out

    public Booking() {}

    public Booking(int userId, String workoutType, String timeSlot, String selectedDate) {
        this.userId      = userId;
        this.workoutType = workoutType;
        this.timeSlot    = timeSlot;
        this.selectedDate = selectedDate;
        this.status      = "booked";
    }

    public int    getId()                          { return id; }
    public void   setId(int id)                    { this.id = id; }

    public int    getUserId()                      { return userId; }
    public void   setUserId(int userId)            { this.userId = userId; }

    public String getWorkoutType()                 { return workoutType; }
    public void   setWorkoutType(String w)         { this.workoutType = w; }

    public String getTimeSlot()                    { return timeSlot; }
    public void   setTimeSlot(String t)            { this.timeSlot = t; }

    public String getBookingDate()                 { return bookingDate; }
    public void   setBookingDate(String d)         { this.bookingDate = d; }

    public String getSelectedDate()                { return selectedDate; }
    public void   setSelectedDate(String d)        { this.selectedDate = d; }

    public String getStatus()                      { return status; }
    public void   setStatus(String s)              { this.status = s; }

    public String getCheckinTime()                 { return checkinTime; }
    public void   setCheckinTime(String t)         { this.checkinTime = t; }

    public String getCheckoutTime()                { return checkoutTime; }
    public void   setCheckoutTime(String t)        { this.checkoutTime = t; }
}