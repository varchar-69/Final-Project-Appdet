package com.example.spottermobile.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Pure time-calculation helpers extracted from DatabaseHelper.
 * No database access — safe to call on the main thread.
 */
public class SlotUtils {

    /** Minutes before slot start after which new bookings are rejected. */
    public static final int GRACE_PERIOD_MINUTES = 15;

    /**
     * Parses the START time from a slot string like "5:00 AM - 7:00 AM"
     * and returns a Calendar set to that time on the given date.
     */
    public static Calendar getSlotStartCalendar(String date, String timeSlot) {
        try {
            String[] parts = timeSlot.split("\\s*-\\s*");
            if (parts.length < 1) return null;
            String startPart = parts[0].trim();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.getDefault());
            Date startDate = sdf.parse(date + " " + startPart);
            if (startDate == null) return null;

            Calendar cal = Calendar.getInstance();
            cal.setTime(startDate);
            return cal;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parses the END time from a slot string like "5:00 AM – 7:00 AM"
     * and returns a Calendar set to that time on the given date.
     * Handles en-dash, em-dash, and regular hyphen.
     */
    public static Calendar getSlotEndCalendar(String date, String timeSlot) {
        try {
            String[] parts = timeSlot.split("\u2013|\u2014|-"); // en-dash, em-dash, hyphen
            if (parts.length < 2) return null;
            String endPart = parts[parts.length - 1].trim();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.getDefault());
            Date endDate = sdf.parse(date + " " + endPart);
            if (endDate == null) return null;

            Calendar cal = Calendar.getInstance();
            cal.setTime(endDate);
            return cal;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns true if a slot is still open for new bookings —
     * i.e. the current time is at least GRACE_PERIOD_MINUTES before slot start.
     */
    public static boolean isSlotBookingOpen(String selectedDate, String timeSlot) {
        Calendar slotStart = getSlotStartCalendar(selectedDate, timeSlot);
        if (slotStart == null) return false;

        Calendar cutoff = (Calendar) slotStart.clone();
        cutoff.add(Calendar.MINUTE, -GRACE_PERIOD_MINUTES);

        return Calendar.getInstance().before(cutoff);
    }
}