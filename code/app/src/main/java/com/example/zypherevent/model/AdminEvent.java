package com.example.zypherevent.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * This is not an actual class for the application. It is just a Mock class created for Demonstration
 * purposes.
 * @author Arunavo Dutta
 * @version 1.0
 */
public class AdminEvent implements Serializable {

    private final String eventId;
    private String name;
    private String time;
    private String details;

    public AdminEvent(@NonNull String name, @NonNull String time, @Nullable String details) {
        this.eventId = UUID.randomUUID().toString(); // Automatically generate a unique ID
        this.name = name;
        this.time = time;
        this.details = details;
    }

    // Getters
    @NonNull
    public String getEventId() { return eventId; }

    @NonNull
    public String getName() { return name; }

    @NonNull
    public String getTime() { return time; }

    @Nullable
    public String getDetails() { return details; }

    // Setters
    public void setName(@NonNull String name) {
        this.name = name;
    }

    public void setTime(@NonNull String time) {
        this.time = time;
    }

    public void setDetails(@Nullable String details) {
        this.details = details;
    }

    @NonNull
    @Override
    public String toString() {
        return "AdminEvent{" +
                "eventId='" + eventId + '\'' +
                ", name='" + name + '\'' +
                ", time='" + time + '\'' +
                ", details='" + details + '\'' +
                '}';
    }

}
