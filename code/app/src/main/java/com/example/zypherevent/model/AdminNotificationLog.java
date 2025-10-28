
package com.example.zypherevent.model;

public class AdminNotificationLog {
    private String message;
    private String event;
    private String group;
    private String timestamp;
    private String sender;

    public AdminNotificationLog(String message, String event, String group, String timestamp, String sender) {
        this.message = message;
        this.event = event;
        this.group = group;
        this.timestamp = timestamp;
        this.sender = sender;
    }

    // Getters
    public String getMessage() { return message; }
    public String getEvent() { return event; }
    public String getGroup() { return group; }
    public String getTimestamp() { return timestamp; }
    public String getSender() { return sender; }
}