package com.example.zypherevent.userTypes;

import com.example.zypherevent.Event;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    /** A list of event IDs the organizer has created. */
    private ArrayList<Long> createdEvents;

    /**
     * Constructs a new Organizer instance with the specified hardware ID, first name, and last name.
     * The organizer’s list of created events is initialized as an empty list.
     *
     * @param hardwareID the unique hardware identifier for this organizer
     * @param firstName  the organizer's first name
     * @param lastName   the organizer's last name
     */
    public Organizer(String hardwareID, String firstName, String lastName) {
        super(UserType.ORGANIZER, hardwareID, firstName, lastName);
        this.createdEvents = new ArrayList<Long>();
    }

    /**
     * Returns a list of event IDs created by this organizer.
     *
     * @return a list containing the organizer's created event IDs
     */
    public ArrayList<Long> getCreatedEvents() {
        return createdEvents;
    }

    /**
     * Adds an event ID to the organizer's list of created events.
     * The event will only be added if it is not already in the list.
     *
     * @param eventID the event ID to add to the organizer's created event list
     */
    public void addCreatedEvent(Long eventID) {
        if (!createdEvents.contains(eventID)) {
            createdEvents.add(eventID);
        }
    }

    /**
     * Removes an event ID from the organizer's list of created events.
     * If the event is not present, no changes are made.
     *
     * @param eventID the event ID to remove from the organizer's created event list
     */
    public void removeCreatedEvent(Long eventID) {
        createdEvents.remove(eventID);
    }

    /**
     * Checks if this Organizer is equal to another object.
     * @param o the object to compare with this Organizer.
     * @return {@code true} if the objects are equal, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false; // parent fields equality

        Organizer organizer = (Organizer) o;

        return java.util.Objects.equals(createdEvents, organizer.createdEvents);
    }

    /**
     * Generates a hash code for this Organizer.
     * <p>
     * This hash code is consistent with the {@link #equals(Object)} method,
     * combining the hash code from the superclass with the hash code
     * of the created events list.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(super.hashCode(), createdEvents);
    }

    // -------------------- ONWARDS: SHOULD ONLY BE USED BY FIRESTORE!!!!!! --------------------

    /**
     * Required by Firestore: public no-arg constructor. ONLY to be used by firestore.
     * Initializes collections to avoid null checks when deserialized.
     */
    public Organizer() {
        // Ensure the type is set
        setUserType(UserType.ORGANIZER);
        this.createdEvents = new ArrayList<Long>();
    }

    /**
     * Sets the organizer's list of created events. ONLY to be used by firestore.
     *
     * @param createdEvents the new list of created events
     */
    public void setCreatedEvents(ArrayList<Long> createdEvents) {
        this.createdEvents = Objects.requireNonNullElseGet(createdEvents, ArrayList::new);
    }
}
