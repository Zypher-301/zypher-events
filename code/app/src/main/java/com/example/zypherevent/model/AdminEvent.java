
package com.example.zypherevent.model;

public class AdminEvent {
    private String name;
    private String time;
    private String details;

    public AdminEvent(String name, String time, String details) {
        this.name = name;
        this.time = time;
        this.details = details;
    }

    // Getters
    public String getName() { return name; }
    public String getTime() { return time; }
    public String getDetails() { return details; }
}