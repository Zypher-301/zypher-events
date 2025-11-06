package com.example.zypherevent.model;

import android.os.Build;

import androidx.annotation.NonNull; // Or another appropriate annotation library
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Represents a log entry for a notification sent by an administrator.
 * This class is immutable to ensure thread-safety and data integrity.
 *
 * @author Arunavo Dutta
 * @version 2.0
 */
public final class AdminNotificationLog {

    private final String message;
    private final String event;
    private final String group;
    private final LocalDateTime timestamp; // Use a proper date-time object
    private final String sender;

    /**
     * Constructs a new AdminNotificationLog.
     *
     * @param message The content of the notification message. Cannot be null.
     * @param event   The event associated with the notification. Can be null.
     * @param group   The group to which the notification was sent. Can be null.
     * @param timestamp The time the notification was sent. Cannot be null.
     * @param sender  The administrator who sent the notification. Cannot be null.
     */
    public AdminNotificationLog(@NonNull String message, String event, String group, @NonNull LocalDateTime timestamp, @NonNull String sender) {
        // Use Objects.requireNonNull for clear and concise null checks
        this.message = Objects.requireNonNull(message, "Message cannot be null");
        this.event = event;
        this.group = group;
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        this.sender = Objects.requireNonNull(sender, "Sender cannot be null");
    }

    // Getters
    @NonNull
    public String getMessage() { return message; }

    public String getEvent() { return event; }

    public String getGroup() { return group; }

    @NonNull
    public LocalDateTime getTimestamp() { return timestamp; }

    /**
     * Returns the timestamp formatted as a String.
     *
     * @param formatter The DateTimeFormatter to use for formatting.
     * @return A formatted date-time string.
     */
    public String getFormattedTimestamp(@NonNull DateTimeFormatter formatter) {
        Objects.requireNonNull(formatter, "Formatter cannot be null");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return timestamp.format(formatter);
        }
    return timestamp.toString();
    }

    @NonNull
    public String getSender() { return sender; }


    @Override
    public String toString() {
        return "AdminNotificationLog{" +
                "message='" + message + '\'' +
                ", event='" + event + '\'' +
                ", group='" + group + '\'' +
                ", timestamp=" + timestamp +
                ", sender='" + sender + '\'' +
                '}';
    }
}
