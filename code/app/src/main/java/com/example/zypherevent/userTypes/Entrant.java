package com.example.zypherevent.userTypes;

import com.example.zypherevent.Event;

import java.util.ArrayList;

/**
 * @author Elliot Chrystal
 * @version 1.0
 * @see User
 * @see Event
 *
 * Represents an entrant in the Zypher Event system. Entrants are users who register
 * for and participate in events. Each entrant has all attributes of a user, including a first
 * name, last name, and hardware ID, along with an email address, and an optional phone number.
 * This class also tracks the entrant’s registered event history.
 *
 */
public class Entrant extends User {

    /** The entrant's email address. */
    private String email;

    /** The entrant's phone number (optional). */
    private String phoneNumber;

    /** A list of events the entrant has registered for. */
    private ArrayList<Event> registeredEventHistory;

    /**
     * Constructs a new Entrant instance with all attributes specified. (Including phone number)
     *
     * @param hardwareID  the unique hardware identifier for this entrant
     * @param firstName   the entrant's first name
     * @param lastName    the entrant's last name
     * @param email       the entrant's email address
     * @param phoneNumber the entrant's phone number
     */
    public Entrant(String hardwareID, String firstName, String lastName, String email, String phoneNumber) {
        super(hardwareID, firstName, lastName);
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.registeredEventHistory = new ArrayList<Event>();
    }

    /**
     * Constructs a new Entrant instance without a phone number.
     *
     * @param hardwareID the unique hardware identifier for this entrant
     * @param firstName  the entrant's first name
     * @param lastName   the entrant's last name
     * @param email      the entrant's email address
     */
    public Entrant(String hardwareID, String firstName, String lastName, String email) {
        super(hardwareID, firstName, lastName);
        this.email = email;
        this.registeredEventHistory = new ArrayList<Event>();
    }

    /**
     * Returns the entrant's email address.
     *
     * @return the entrant's email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Updates the entrant's email address.
     *
     * @param email the new email address to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returns the entrant's phone number, or "N/A" if no phone number is set.
     *
     * @return the entrant's phone number or "N/A" if unavailable
     */
    public String getPhoneNumber() {
        if (phoneNumber == null) {
            return "N/A";
        }
        return phoneNumber;
    }

    /**
     * Updates the entrant's phone number.
     *
     * @param phoneNumber the new phone number to set
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * Returns a list of events that the entrant has registered for.
     *
     * @return a list containing the entrant's registered events
     */
    public ArrayList<Event> getRegisteredEventHistory() {
        return registeredEventHistory;
    }

    /**
     * Adds a new event to the entrant's registered event history.
     * The event will only be added if it is not already in the list.
     *
     * @param event the event to add to the entrant's registration history
     */
    public void addEventToRegisteredEventHistory(Event event) {
        // Only add event if it's not already in the history
        if (!registeredEventHistory.contains(event)) {
            registeredEventHistory.add(event);
        }
    }

    /**
     * Removes an event from the entrant's registered event history.
     * If the event is not present, no changes are made.
     *
     * @param event the event to remove from the entrant's registration history
     */
    public void removeEventFromRegisteredEventHistory(Event event) {
        // No contains check needed, as .remove already checks for existence internally
        registeredEventHistory.remove(event);
    }
}
