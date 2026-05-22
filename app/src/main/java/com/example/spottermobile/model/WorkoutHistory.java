package com.example.spottermobile.model;

public class WorkoutHistory {
    private String id;       // CHANGED: int -> String for Firestore Document ID
    private String userId;   // CHANGED: int -> String for Firestore User ID
    private String workoutName;
    private int duration;
    private int calories;
    private String date;

    public WorkoutHistory() {}

    // CHANGED: userId parameter is now a String
    public WorkoutHistory(String userId, String workoutName, int duration, int calories) {
        this.userId = userId;
        this.workoutName = workoutName;
        this.duration = duration;
        this.calories = calories;
    }

    // Getters and Setters
    // CHANGED: getId and setId now use String
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    // CHANGED: getUserId and setUserId now use String
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getWorkoutName() { return workoutName; }
    public void setWorkoutName(String workoutName) { this.workoutName = workoutName; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public int getCalories() { return calories; }
    public void setCalories(int calories) { this.calories = calories; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}