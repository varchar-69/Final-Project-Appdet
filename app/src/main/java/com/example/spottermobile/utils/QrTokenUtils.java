package com.example.spottermobile.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Generates and verifies HMAC-SHA256 tokens embedded in booking QR codes.
 *
 * WHY: Without this, anyone can open a text editor, type "Booking #5", generate
 * a QR code, and check in as any booking. With HMAC, the token can only be
 * produced by this app — forged QRs will fail verification.
 *
 * QR FORMAT:  SPOTTER|<bookingId>|<userId>|<selectedDate>|<hmacToken>
 */
public class QrTokenUtils {

    // In a production app, move this to BuildConfig or encrypted storage.
    // For a capstone, a hardcoded secret is acceptable — just mention it in your limitations.
    private static final String SECRET = "SpotterGym_HMAC_Secret_2025";

    /**
     * Builds the full QR string for a confirmed booking.
     */
    public static String buildQrContent(int bookingId, int userId, String selectedDate, String timeSlot) {
        String payload = bookingId + "|" + userId + "|" + selectedDate + "|" + timeSlot;
        String token   = hmacSha256(payload);
        return "SPOTTER|" + payload + "|" + token;
    }

    /**
     * Parses and verifies a scanned QR string.
     * Returns the booking ID if valid, or -1 if the QR is invalid or forged.
     */
    public static int verifyAndExtractBookingId(String qrContent) {
        if (qrContent == null || !qrContent.startsWith("SPOTTER|")) return -1;
        try {
            // Expected: SPOTTER|bookingId|userId|date|slot|token  → 6 parts
            String[] parts = qrContent.split("\\|");
            if (parts.length != 6) return -1;

            int    bookingId    = Integer.parseInt(parts[1]);
            String userId       = parts[2];
            String selectedDate = parts[3];
            String timeSlot     = parts[4];
            String receivedToken = parts[5];

            // Re-compute expected token and compare
            String payload       = parts[1] + "|" + userId + "|" + selectedDate + "|" + timeSlot;
            String expectedToken = hmacSha256(payload);

            if (!expectedToken.equals(receivedToken)) return -1; // tampered or forged
            return bookingId;

        } catch (NumberFormatException e) {
            return -1;
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
            // Should never happen on Android — both algorithm and charset are guaranteed
            throw new RuntimeException("HMAC-SHA256 unavailable", e);
        }
    }
}
