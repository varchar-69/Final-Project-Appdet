package com.example.spottermobile.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Generates and verifies HMAC-SHA256 tokens embedded in booking QR codes.
 *
 * QR FORMAT:  SPOTTER|<bookingId>|<userId>|<selectedDate>|<timeSlot>|<hmacToken>
 *
 * CHANGED: bookingId and userId are now Strings (Firestore document IDs)
 * instead of ints (SQLite row IDs).
 */
public class QrTokenUtils {

    private static final String SECRET = "SpotterGym_HMAC_Secret_2025";

    /**
     * Builds the full QR string for a confirmed booking.
     * CHANGED: bookingId and userId are now Strings.
     */
    public static String buildQrContent(String bookingId, String userId,
                                        String selectedDate, String timeSlot) {
        String payload = bookingId + "|" + userId + "|" + selectedDate + "|" + timeSlot;
        String token   = hmacSha256(payload);
        return "SPOTTER|" + payload + "|" + token;
    }

    /**
     * Parses and verifies a scanned QR string.
     * CHANGED: returns the booking ID as a String instead of int.
     * Returns null if the QR is invalid or forged (was -1).
     */
    public static String verifyAndExtractBookingId(String qrContent) {
        if (qrContent == null || !qrContent.startsWith("SPOTTER|")) return null;
        try {
            // Expected: SPOTTER|bookingId|userId|date|slot|token → 6 parts
            String[] parts = qrContent.split("\\|");
            if (parts.length != 6) return null;

            String bookingId     = parts[1];
            String userId        = parts[2];
            String selectedDate  = parts[3];
            String timeSlot      = parts[4];
            String receivedToken = parts[5];

            // Re-compute expected token and compare
            String payload       = bookingId + "|" + userId + "|" + selectedDate + "|" + timeSlot;
            String expectedToken = hmacSha256(payload);

            if (!expectedToken.equals(receivedToken)) return null; // tampered or forged
            return bookingId;

        } catch (Exception e) {
            return null;
        }
    }

    /** Computes HMAC-SHA256 of payload using the app secret. Returns lowercase hex string. */
    private static String hmacSha256(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes("UTF-8"), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : raw) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 unavailable", e);
        }
    }
}