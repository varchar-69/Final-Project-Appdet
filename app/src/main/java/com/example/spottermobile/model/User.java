package com.example.spottermobile.model;

public class User {
    private int id;
    private String username;
    private String fullName;
    private String email;
    private String gender;
    private String contactNumber;
    private String address;
    private String emergencyContactName;
    private String emergencyContactNumber;
    private String password;
    private String role; // "user" or "admin"
    private String createdDate;

    public User() {}

    // Constructor for registration
    public User(String username, String fullName, String email, String gender,
                String contactNumber, String address, String emergencyName,
                String emergencyNumber, String password) {
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.gender = gender;
        this.contactNumber = contactNumber;
        this.address = address;
        this.emergencyContactName = emergencyName;
        this.emergencyContactNumber = emergencyNumber;
        this.password = password;
        this.role = "user"; // Default role
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }

    public String getEmergencyContactNumber() { return emergencyContactNumber; }
    public void setEmergencyContactNumber(String emergencyContactNumber) { this.emergencyContactNumber = emergencyContactNumber; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getCreatedDate() { return createdDate; }
    public void setCreatedDate(String createdDate) { this.createdDate = createdDate; }
}