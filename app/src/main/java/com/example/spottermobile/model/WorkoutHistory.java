package com.example.spottermobile.model;

public class WorkoutHistory {
    private int id;
    private int userId;
    private String workoutName;
    private int duration;
    private int calories;
    private String date;

    public WorkoutHistory() {}

    public WorkoutHistory(int userId, String workoutName, int duration, int calories) {
        this.userId = userId;
        this.workoutName = workoutName;
        this.duration = duration;
        this.calories = calories;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getWorkoutName() { return workoutName; }
    public void setWorkoutName(String workoutName) { this.workoutName = workoutName; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public int getCalories() { return calories; }
    public void setCalories(int calories) { this.calories = calories; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}