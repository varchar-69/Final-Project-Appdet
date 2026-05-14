package com.example.spottermobile.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for SHA-256 password hashing.
 * NEVER store or compare plaintext passwords — always use this class.
 */
public class PasswordUtils {

    /**
     * Hashes a plaintext password using SHA-256.
     *
     * @param plainPassword the raw password string entered by the user
     * @return the SHA-256 hex string, or null if hashing fails
     */
    public static String hashPassword(String plainPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plainPassword.getBytes("UTF-8"));

            // Convert byte array to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Verifies a plaintext password against a stored SHA-256 hash.
     *
     * @param plainPassword  the raw password the user typed
     * @param hashedPassword the SHA-256 hash stored in the database
     * @return true if the password matches the stored hash
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) return false;
        String hashed = hashPassword(plainPassword);
        return hashedPassword.equals(hashed);
    }
}