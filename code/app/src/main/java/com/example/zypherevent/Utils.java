package com.example.zypherevent;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Utils {
    /**
     * The application does a lot of date comparisons but they are repetitive
     * to parse and create correctly in java.
     * This function takes a string in the format "yyyy-MM-dd" and returns a date
     * that covers the full day for accurate comparisons.
     * @param dateStr the date string
     * @throws java.text.ParseException if date cannot be parsed.
     * @return a new data object that incudes the whole day of the given date.
     */
    public static Date createWholeDayDate(String dateStr) throws ParseException {
        if (dateStr == null) return null;

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date date = formatter.parse(dateStr);
        if (date == null) return null; // shouldn't really happen

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }

    /**
     * Formats a Date object to a string in "yyyy-MM-dd" format for display.
     * @param date the date to format
     * @return a string representation of the date in "yyyy-MM-dd" format, or empty string if date is null
     */
    public static String formatDateForDisplay(Date date) {
        if (date == null) return "";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(date);
    }
}
