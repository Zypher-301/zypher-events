package com.example.zypherevent;

import com.example.zypherevent.userTypes.Entrant;
import com.example.zypherevent.userTypes.Organizer;
import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * @author Elliot Chrystal
 * @version 1.0
 * @see Organizer
 * @see Entrant
 *
 * Represents an event within the Zypher Event system. Each event has identifying details such as
 * name, description, start time, and location, along with registration start and end times. Events
 * are created and managed by an Organizer, and maintain lists of Entrants who have been waitlisted,
 * accepted, or declined. The class also supports optional event promotional posters via Firebase
 * paths, and provides methods for managing entrant lists.
 *
 */
public class Event {

    /** The event's name. */
    private String eventName;

    /** A brief description of the event. */
    private String eventDescription;

    /** The start time of the event. */
    private LocalDateTime startTime;

    /** The event's physical or virtual location. */
    private String location;

    /** The time when registration for the event begins. */
    private LocalDateTime registrationStartTime;

    /** The time when registration for the event closes. */
    private LocalDateTime registrationEndTime;

    /** The Firebase storage path for the event's optional promotional poster. */
    // TODO: in database, make sure that uploading a poster image returns the firebase poster path!
    private String firebasePosterPath;

    /** The organizer responsible for managing the event. */
    private Organizer eventOrganizer;

    /** A list of entrants currently on the event's waitlist. */
    private ArrayList<Entrant> waitListEntrants;

    /** A list of entrants who have been accepted into the event. */
    private ArrayList<Entrant> acceptedEntrants;

    /** A list of entrants who have been declined from the event. */
    private ArrayList<Entrant> declinedEntrants;

    /**
     * Constructs a new Event instance with all event details specified.
     *
     * @param eventName               the name of the event
     * @param eventDescription        a brief description of the event
     * @param startTime               the start time of the event
     * @param location                the location where the event will take place
     * @param registrationStartTime   the time when registration opens
     * @param registrationEndTime     the time when registration closes
     * @param eventOrganizer          the organizer responsible for the event
     * @param firebasePosterPath      the Firebase path to the event's optional promotional poster
     */
    public Event(String eventName, String eventDescription, LocalDateTime startTime, String location,
                 LocalDateTime registrationStartTime, LocalDateTime registrationEndTime,
                 Organizer eventOrganizer, String firebasePosterPath) {
        this.eventName = eventName;
        this.eventDescription = eventDescription;
        this.startTime = startTime;
        this.location = location;
        this.registrationStartTime = registrationStartTime;
        this.registrationEndTime = registrationEndTime;
        this.firebasePosterPath = firebasePosterPath;
        this.eventOrganizer = eventOrganizer;

        // Initialize entrant lists
        this.waitListEntrants = new ArrayList<>();
        this.acceptedEntrants = new ArrayList<>();
        this.declinedEntrants = new ArrayList<>();
    }

    /**
     * Constructs a new Event instance with all event details specified.
     *
     * @param eventName               the name of the event
     * @param eventDescription        a brief description of the event
     * @param startTime               the start time of the event
     * @param location                the location where the event will take place
     * @param registrationStartTime   the time when registration opens
     * @param registrationEndTime     the time when registration closes
     * @param eventOrganizer          the organizer responsible for the event
     */
    public Event(String eventName, String eventDescription, LocalDateTime startTime, String location,
                 LocalDateTime registrationStartTime, LocalDateTime registrationEndTime,
                 Organizer eventOrganizer) {
        this.eventName = eventName;
        this.eventDescription = eventDescription;
        this.startTime = startTime;
        this.location = location;
        this.registrationStartTime = registrationStartTime;
        this.registrationEndTime = registrationEndTime;
        this.eventOrganizer = eventOrganizer;

        // Initialize entrant lists
        this.waitListEntrants = new ArrayList<>();
        this.acceptedEntrants = new ArrayList<>();
        this.declinedEntrants = new ArrayList<>();
    }

    /**
     * Returns the event's name.
     *
     * @return the event's name
     */
    public String getEventName() {
        return eventName;
    }

    /**
     * Updates the event's name.
     *
     * @param eventName the new event name to set
     */
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    /**
     * Returns the event's description.
     *
     * @return the event's description
     */
    public String getEventDescription() {
        return eventDescription;
    }

    /**
     * Updates the event's description.
     *
     * @param eventDescription the new event description to set
     */
    public void setEventDescription(String eventDescription) {
        this.eventDescription = eventDescription;
    }

    /**
     * Returns the start time of the event.
     *
     * @return the event's start time
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }

    /**
     * Updates the start time of the event.
     *
     * @param startTime the new start time to set
     */
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    /**
     * Returns the event's location.
     *
     * @return the event's location
     */
    public String getLocation() {
        return location;
    }

    /**
     * Updates the event's location.
     *
     * @param location the new location to set
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Returns the registration start time for the event.
     *
     * @return the registration start time
     */
    public LocalDateTime getRegistrationStartTime() {
        return registrationStartTime;
    }

    /**
     * Updates the registration start time for the event.
     *
     * @param registrationStartTime the new registration start time to set
     */
    public void setRegistrationStartTime(LocalDateTime registrationStartTime) {
        this.registrationStartTime = registrationStartTime;
    }

    /**
     * Returns the registration end time for the event.
     *
     * @return the registration end time
     */
    public LocalDateTime getRegistrationEndTime() {
        return registrationEndTime;
    }

    /**
     * Updates the registration end time for the event.
     *
     * @param registrationEndTime the new registration end time to set
     */
    public void setRegistrationEndTime(LocalDateTime registrationEndTime) {
        this.registrationEndTime = registrationEndTime;
    }

    /**
     * Returns the Firebase storage path for the event's poster, or "N/A" if no poster path is set.
     *
     * @return the Firebase poster path or "N/A" if unavailable
     */
    public String getFirebasePosterPath() {
        if (firebasePosterPath == null) {
            return "N/A";
        }
        return firebasePosterPath;
    }

    /**
     * Updates the Firebase storage path for the event's poster.
     *
     * @param firebasePosterPath the new Firebase poster path to set
     */
    public void setFirebasePosterPath(String firebasePosterPath) {
        this.firebasePosterPath = firebasePosterPath;
    }

    /**
     * Returns the organizer responsible for the event.
     *
     * @return the event organizer
     */
    public Organizer getEventOrganizer() {
        return eventOrganizer;
    }

    /**
     * Updates the organizer responsible for the event.
     *
     * @param eventOrganizer the new organizer to assign
     */
    public void setEventOrganizer(Organizer eventOrganizer) {
        this.eventOrganizer = eventOrganizer;
    }

    /**
     * Returns a list of entrants currently on the waitlist.
     *
     * @return the list of waitlisted entrants
     */
    public ArrayList<Entrant> getWaitListEntrants() {
        return waitListEntrants;
    }

    /**
     * Returns a list of entrants accepted into the event.
     *
     * @return the list of accepted entrants
     */
    public ArrayList<Entrant> getAcceptedEntrants() {
        return acceptedEntrants;
    }

    /**
     * Returns a list of entrants declined from the event.
     *
     * @return the list of declined entrants
     */
    public ArrayList<Entrant> getDeclinedEntrants() {
        return declinedEntrants;
    }

    /**
     * Adds an entrant to the event's waitlist.
     * The entrant will only be added if they are not already in the list.
     *
     * @param entrant the entrant to add to the waitlist
     */
    public void addEntrantToWaitList(Entrant entrant) {
        if (!waitListEntrants.contains(entrant)) {
            waitListEntrants.add(entrant);
        }
    }

    /**
     * Removes an entrant from the event's waitlist.
     * If the entrant is not present, no changes are made.
     *
     * @param entrant the entrant to remove from the waitlist
     */
    public void removeEntrantFromWaitList(Entrant entrant) {
        waitListEntrants.remove(entrant);
    }

    /**
     * Adds an entrant to the event's accepted list.
     * The entrant will only be added if they are not already in the list.
     *
     * @param entrant the entrant to add to the accepted list
     */
    public void addEntrantToAcceptedList(Entrant entrant) {
        if (!acceptedEntrants.contains(entrant)) {
            acceptedEntrants.add(entrant);
        }
    }

    /**
     * Removes an entrant from the event's accepted list.
     * If the entrant is not present, no changes are made.
     *
     * @param entrant the entrant to remove from the accepted list
     */
    public void removeEntrantFromAcceptedList(Entrant entrant) {
        acceptedEntrants.remove(entrant);
    }

    /**
     * Adds an entrant to the event's declined list.
     * The entrant will only be added if they are not already in the list.
     *
     * @param entrant the entrant to add to the declined list
     */
    public void addEntrantToDeclinedList(Entrant entrant) {
        if (!declinedEntrants.contains(entrant)) {
            declinedEntrants.add(entrant);
        }
    }

    /**
     * Removes an entrant from the event's declined list.
     * If the entrant is not present, no changes are made.
     *
     * @param entrant the entrant to remove from the declined list
     */
    public void removeEntrantFromDeclinedList(Entrant entrant) {
        declinedEntrants.remove(entrant);
    }
}