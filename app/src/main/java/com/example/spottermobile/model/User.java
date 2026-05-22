package com.example.spottermobile.model;

public class User {
    private String id; // Firestore document ID (String)
    private String username;
    private String fullName;
    private String email;
    private String gender;
    private String contactNumber;
    private String address;
    private String emergencyContactName;
    private String emergencyContactNumber;
    private String password;
    private String userType; // "member" or "admin"
    private String role;     // legacy alias kept for callers using getRole()
    private String createdDate;
    private boolean isSuspended;

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
        this.userType = "member";
        this.role = "user";
    }

    // ── ID (single String field — no more int overload) ───────────────────────
    public String getId()               { return id; }
    public void   setId(String id)      { this.id = id; }

    /** Legacy alias so any code calling getFirestoreId() still compiles. */
    public String getFirestoreId()          { return id; }
    public void   setFirestoreId(String id) { this.id = id; }

    // ── Regular fields ────────────────────────────────────────────────────────
    public String getUsername()                         { return username; }
    public void   setUsername(String username)          { this.username = username; }

    public String getFullName()                         { return fullName; }
    public void   setFullName(String fullName)          { this.fullName = fullName; }

    /** Alias so code calling getName() still compiles. */
    public String getName()                             { return fullName; }
    public void   setName(String name)                  { this.fullName = name; }

    public String getEmail()                            { return email; }
    public void   setEmail(String email)                { this.email = email; }

    public String getGender()                           { return gender; }
    public void   setGender(String gender)              { this.gender = gender; }

    public String getContactNumber()                    { return contactNumber; }
    public void   setContactNumber(String v)            { this.contactNumber = v; }

    public String getAddress()                          { return address; }
    public void   setAddress(String address)            { this.address = address; }

    public String getEmergencyContactName()             { return emergencyContactName; }
    public void   setEmergencyContactName(String v)     { this.emergencyContactName = v; }

    public String getEmergencyContactNumber()           { return emergencyContactNumber; }
    public void   setEmergencyContactNumber(String v)   { this.emergencyContactNumber = v; }

    public String getPassword()                         { return password; }
    public void   setPassword(String password)          { this.password = password; }

    public String getUserType()                         { return userType; }
    public void   setUserType(String userType)          {
        this.userType = userType;
        this.role = "admin".equals(userType) ? "admin" : "user";
    }

    /** Legacy alias — maps to userType. */
    public String getRole()                             { return role != null ? role : userType; }
    public void   setRole(String role)                  {
        this.role = role;
        this.userType = "admin".equals(role) ? "admin" : "member";
    }

    public String getCreatedDate()                      { return createdDate; }
    public void   setCreatedDate(String createdDate)    { this.createdDate = createdDate; }

    public boolean isSuspended()                        { return isSuspended; }
    public void    setSuspended(boolean suspended)      { this.isSuspended = suspended; }
}