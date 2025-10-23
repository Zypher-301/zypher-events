package com.example.zypherevent.userTypes;

import com.example.zypherevent.Event;
import java.util.ArrayList;

/**
 * @author Elliot Chrystal
 * @version 1.0
 * @see User
 * @see Event
 *
 * Represents an organizer in the Zypher Event system. Organizers are users responsible for
 * creating and managing events. Each organizer has all attributes of a user, including
 * a first name, last name, and hardware ID. This class also tracks the list of events
 * that the organizer has created.
 *
 */
public class Organizer extends User {

    /** A list of events the organizer has created. */
    private ArrayList<Event> createdEvents;

    /**
     * Constructs a new Organizer instance with the specified hardware ID, first name, and last name.
     * The organizer’s list of created events is initialized as an empty list.
     *
     * @param hardwareID the unique hardware identifier for this organizer
     * @param firstName  the organizer's first name
     * @param lastName   the organizer's last name
     */
    public Organizer(String hardwareID, String firstName, String lastName) {
        super(hardwareID, firstName, lastName);
        this.createdEvents = new ArrayList<Event>();
    }

    /**
     * Returns a list of events created by this organizer.
     *
     * @return a list containing the organizer's created events
     */
    public ArrayList<Event> getCreatedEvents() {
        return createdEvents;
    }

    /**
     * Adds an event to the organizer's list of created events.
     * The event will only be added if it is not already in the list.
     *
     * @param event the event to add to the organizer's created event list
     */
    public void addCreatedEvent(Event event) {
        if (!createdEvents.contains(event)) {
            createdEvents.add(event);
        }
    }

    /**
     * Removes an event from the organizer's list of created events.
     * If the event is not present, no changes are made.
     *
     * @param event the event to remove from the organizer's created event list
     */
    public void removeCreatedEvent(Event event) {
        createdEvents.remove(event);
    }
}
