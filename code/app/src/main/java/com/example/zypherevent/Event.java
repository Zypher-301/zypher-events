package com.example.zypherevent;

import com.example.zypherevent.userTypes.Entrant;
import com.example.zypherevent.userTypes.Organizer;
import com.example.zypherevent.userTypes.UserType;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

/**
 * @author Elliot Chrystal
 * @version 1.0
 * @see Organizer
 * @see Entrant
 * <p>
 * Represents an event within the Zypher Event system. Each event has identifying details such as
 * name, description, start time, and location, along with registration start and end times. Events
 * are created and managed by an Organizer, and maintain lists of Entrants who have been waitlisted,
 * accepted, or declined. The class also supports optional event promotional posters via a poster's
 * external URL, and provides methods for managing entrant lists.
 */
public class Event implements Serializable {

    private Long uniqueEventID;

    /**
     * The event's name.
     */
    private String eventName;

    /**
     * A brief description of the event.
     */
    private String eventDescription;

    /**
     * The start time of the event.
     */
    private Date startTime;

    /**
     * The event's physical or virtual location.
     */
    private String location;

    /**
     * The time when registration for the event begins.
     */
    private Date registrationStartTime;

    /**
     * The time when registration for the event closes.
     */
    private Date registrationEndTime;

    /**
     * The URL for the event's optional promotional poster.
     */
    private String posterURL;

    /**
     * The organizer responsible for managing the event.
     */
    private String eventOrganizerHardwareID;

    /**
     * The maximum number of entrants in the waitlist.
     * Can be null so we use Integer class instead of int primitive
     */
    private Integer waitlistLimit;

    /**
     * A list of entrants currently on the event's waitlist.
     */
    private ArrayList<WaitlistEntry> waitListEntrants;

    /**
     * A list of entrants who have been accepted into the event.
     */
    private ArrayList<Entrant> acceptedEntrants;

    /**
     * A list of entrants who have been declined from the event.
     */
    private ArrayList<Entrant> declinedEntrants;

    /**
     * Constructs a new Event instance with all event details specified.
     *
     * @param uniqueEventID            the unique identifier for the event
     * @param eventName                the name of the event
     * @param eventDescription         a brief description of the event
     * @param startTime                the start time of the event
     * @param location                 the location where the event will take place
     * @param registrationStartTime    the time when registration opens
     * @param registrationEndTime      the time when registration closes
     * @param eventOrganizerHardwareID the organizer responsible for the event
     * @param posterURL                the Firebase path to the event's optional promotional poster
     */
    public Event(Long uniqueEventID, String eventName, String eventDescription, Date startTime, String location,
                 Date registrationStartTime, Date registrationEndTime,
                 String eventOrganizerHardwareID, String posterURL) {
        this.uniqueEventID = uniqueEventID;
        this.eventName = eventName;
        this.eventDescription = eventDescription;
        this.startTime = startTime;
        this.location = location;
        this.registrationStartTime = registrationStartTime;
        this.registrationEndTime = registrationEndTime;
        this.posterURL = posterURL;
        this.eventOrganizerHardwareID = eventOrganizerHardwareID;

        // Initialize entrant lists
        this.waitListEntrants = new ArrayList<>();
        this.acceptedEntrants = new ArrayList<>();
        this.declinedEntrants = new ArrayList<>();
    }

    /**
     * Constructs a new Event instance without a poster URL specified.
     *
     * @param uniqueEventID            the unique identifier for the event
     * @param eventName                the name of the event
     * @param eventDescription         a brief description of the event
     * @param startTime                the start time of the event
     * @param location                 the location where the event will take place
     * @param registrationStartTime    the time when registration opens
     * @param registrationEndTime      the time when registration closes
     * @param eventOrganizerHardwareID the organizer responsible for the event
     */
    public Event(Long uniqueEventID, String eventName, String eventDescription, Date startTime, String location,
                 Date registrationStartTime, Date registrationEndTime,
                 String eventOrganizerHardwareID) {
        this.uniqueEventID = uniqueEventID;
        this.eventName = eventName;
        this.eventDescription = eventDescription;
        this.startTime = startTime;
        this.location = location;
        this.registrationStartTime = registrationStartTime;
        this.registrationEndTime = registrationEndTime;
        this.eventOrganizerHardwareID = eventOrganizerHardwareID;

        // Initialize entrant lists
        this.waitListEntrants = new ArrayList<>();
        this.acceptedEntrants = new ArrayList<>();
        this.declinedEntrants = new ArrayList<>();
    }

    /**
     * Returns the unique identifier for the event.
     *
     * @return the event's unique identifier
     */
    public Long getUniqueEventID() {
        return uniqueEventID;
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
    public Date getStartTime() {
        return startTime;
    }

    /**
     * Updates the start time of the event.
     *
     * @param startTime the new start time to set
     */
    public void setStartTime(Date startTime) {
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
    public Date getRegistrationStartTime() {
        return registrationStartTime;
    }

    /**
     * Updates the registration start time for the event.
     *
     * @param registrationStartTime the new registration start time to set
     */
    public void setRegistrationStartTime(Date registrationStartTime) {
        this.registrationStartTime = registrationStartTime;
    }

    /**
     * Returns the registration end time for the event.
     *
     * @return the registration end time
     */
    public Date getRegistrationEndTime() {
        return registrationEndTime;
    }

    /**
     * Updates the registration end time for the event.
     *
     * @param registrationEndTime the new registration end time to set
     */
    public void setRegistrationEndTime(Date registrationEndTime) {
        this.registrationEndTime = registrationEndTime;
    }

    /**
     * Returns the event's poster URL, or null if no poster URL is set.
     *
     * @return the poster URL or null if not set
     */
    public String getPosterURL() {
        return posterURL;
    }

    /**
     * Updates the event's poster URL.
     *
     * @param posterURL the new poster URL to set for the event
     */
    public void setPosterURL(String posterURL) {
        this.posterURL = posterURL;
    }

    /**
     * Returns the organizer ID responsible for the event.
     *
     * @return the event organizer
     */
    public String getEventOrganizerHardwareID() {
        return eventOrganizerHardwareID;
    }

    /**
     * Updates the organizer responsible for the event.
     *
     * @param eventOrganizerHardwareID the new organizer ID to assign
     */
    public void setEventOrganizerHardwareID(String eventOrganizerHardwareID) {
        this.eventOrganizerHardwareID = eventOrganizerHardwareID;
    }

    /**
     * Gets the current waitlist limit
     * @return the current limit for entrants in a waitlist.
     */
    public Integer getWaitlistLimit() {
        return this.waitlistLimit;
    }

    /**
     * Sets the current waitlist limit
     */
    public void setWaitlistLimit(Integer waitlistLimit) {
        this.waitlistLimit = waitlistLimit;
    }

    /**
     * Returns a list of entrants currently on the waitlist.
     *
     * @return the list of waitlisted entrants
     */
    public ArrayList<WaitlistEntry> getWaitListEntrants() {
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
     * Makes sure that the waitlist limit has not been exceeded and
     * the registration happens within the allowed date range.
     *
     * @param entrant the entrant to add to the waitlist
     */
    public void addEntrantToWaitList(Entrant entrant) {
        if (this.waitlistLimit != null && this.waitListEntrants.size() >= this.waitlistLimit) {
            throw new IllegalStateException("Waitlist is full");
        }
        Date now = new Date();
        if (now.after(this.registrationEndTime)) {
            throw new IllegalStateException("This event's registration window has ended");
        }
        if (now.before(this.registrationStartTime)) {
            throw new IllegalStateException("This event's registration window has not yet started");
        }
        // Check if entrant is already on waitlist by comparing entrants (not WaitlistEntry which includes timestamp)
        boolean alreadyOnWaitlist = false;
        for (WaitlistEntry existingEntry : waitListEntrants) {
            if (existingEntry.getEntrant().equals(entrant)) {
                alreadyOnWaitlist = true;
                break;
            }
        }
        if (!alreadyOnWaitlist) {
            WaitlistEntry entry = new WaitlistEntry(entrant);
            waitListEntrants.add(entry);
        }
    }

    /**
     * Removes an entrant from the event's waitlist.
     * If the entrant is not present, no changes are made.
     *
     * @param entry the entrant to remove from the waitlist
     */
    public void removeEntrantFromWaitList(WaitlistEntry entry) {
        waitListEntrants.remove(entry);
    }

    /**
     * Adds an entrant to the event's accepted list.
     * The entrant will only be added if they are not already in the list.
     *
     * @param entry the entrant to add to the accepted list
     */
    public void addEntrantToAcceptedList(WaitlistEntry entry) {
        if (!acceptedEntrants.contains(entry.getEntrant())) {
            acceptedEntrants.add(entry.getEntrant());
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

    // ONWARDS: SHOULD ONLY BE USED BY FIRESTORE!!!!!!

    /**
     * Required by Firestore: public no-arg constructor.
     * Initializes collections to avoid null checks when deserialized.
     */
    public Event() {
        this.waitListEntrants = new ArrayList<>();
        this.acceptedEntrants = new ArrayList<>();
        this.declinedEntrants = new ArrayList<>();
    }

    /**
     * Sets the event's unique identifier. ONLY to be used by firestore.
     *
     * @param uniqueEventID the new unique event identifier
     */
    public void setUniqueEventID(Long uniqueEventID) {
        this.uniqueEventID = uniqueEventID;
    }

    /**
     * Sets the event's waitlist. ONLY to be used by firestore.
     *
     * @param waitListEntrants the new list of waitlisted entrants
     */
    public void setWaitListEntrants(ArrayList<WaitlistEntry> waitListEntrants) {
        this.waitListEntrants = Objects.requireNonNullElseGet(waitListEntrants, ArrayList::new);
    }

    /**
     * Sets the event's accepted list. ONLY to be used by firestore.
     *
     * @param acceptedEntrants the new list of accepted entrants
     */
    public void setAcceptedEntrants(ArrayList<Entrant> acceptedEntrants) {
        this.acceptedEntrants = Objects.requireNonNullElseGet(acceptedEntrants, ArrayList::new);
    }

    /**
     * Sets the event's declined list. ONLY to be used by firestore.
     *
     * @param declinedEntrants the new list of declined entrants
     */
    public void setDeclinedEntrants(ArrayList<Entrant> declinedEntrants) {
        this.declinedEntrants = Objects.requireNonNullElseGet(declinedEntrants, ArrayList::new);
    }

    // Comparison functions!

    /**
     * Checks if this Event is equal to another object.
     * @param o the object to compare with this Event.
     * @return {@code true} if the objects are equal, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Objects.equals(uniqueEventID, event.uniqueEventID);
    }

    /**
     * Generates a hash code for this Event.
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(uniqueEventID);
    }
}