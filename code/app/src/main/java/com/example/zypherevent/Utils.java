package com.example.zypherevent;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

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

    /**
     * Generates a QR code bitmap for the given event ID.
     * The QR code encodes the event ID which can be scanned to view event details.
     *
     * @param eventId The unique identifier of the event
     * @param width   The width of the QR code bitmap in pixels
     * @param height  The height of the QR code bitmap in pixels
     * @return A Bitmap containing the QR code, or null if generation fails
     */
    public static Bitmap generateQRCode(Long eventId, int width, int height) {
        if (eventId == null) {
            return null;
        }

        try {
            // Encode the event ID as a string with EVENT: prefix
            String qrContent = "EVENT:" + eventId;
            
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, width, height);
            
            // Create bitmap from bit matrix
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Extracts the event ID from a scanned QR code content.
     *
     * @param qrContent The content string from the scanned QR code
     * @return The event ID, or null if the content is invalid
     */
    public static Long extractEventId(String qrContent) {
        if (qrContent == null || !qrContent.startsWith("EVENT:")) {
            return null;
        }
        
        try {
            String idString = qrContent.substring(6); // Remove "EVENT:" prefix
            return Long.parseLong(idString);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return null;
        }
    }
}
