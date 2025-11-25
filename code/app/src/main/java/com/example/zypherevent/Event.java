package com.example.zypherevent;

import com.example.zypherevent.userTypes.Entrant;
import com.example.zypherevent.userTypes.Organizer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

/**
 * @author Elliot Chrystal
 * @version 1.0
 * @see Organizer
 * @see Entrant
 *
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
     * Event specific criteria/guidelines for the lottery
     * e.g age range, skill level
     */
    private String lotteryCriteria;



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
     * True or false depending on if the event is only avaliable to Entrants with GeoLocation enabled
     */
    private boolean requiresGeolocation;

    /**
     * The maximum number of entrants in the waitlist.
     * Can be null so we use Integer class instead of int primitive
     */
    private Integer waitlistLimit;

    /**
     * A list of WaitlistEntry objects storing the entrant's hardware ID and the timestamp
     * when they joined the waitlist.
     */
    private ArrayList<WaitlistEntry> waitListEntrants;

    /**
     * A list of entrant hardware IDs who have been invited into the event.
     */
    private ArrayList<String> invitedEntrants;

    /**
     * A list of entrant hardware IDs who have been accepted into the event.
     */
    private ArrayList<String> acceptedEntrants;

    /**
     * A list of entrant hardware IDs who have been declined from the event.
     */
    private ArrayList<String> declinedEntrants;

    /**
     * The status of the join and leave waitlist operation.
     */
    public enum WaitlistOperationResult {
        SUCCESS,
        ALREADY_INVITED,
        ALREADY_ACCEPTED,
        ALREADY_DECLINED,
        ALREADY_ON_WAITLIST,
        NOT_ON_WAITLIST,
        REGISTRATION_NOT_STARTED,
        REGISTRATION_CLOSED,
        WAITLIST_FULL
    }

    /**
     * The status of the entrant's registration.
     */
    public enum EntrantStatus {
        NONE,
        WAITLISTED,
        INVITED,
        ACCEPTED,
        DECLINED
    }

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
                 String eventOrganizerHardwareID, String posterURL, boolean requiresGeolocation) {
        this.uniqueEventID = uniqueEventID;
        this.eventName = eventName;
        this.eventDescription = eventDescription;
        this.startTime = startTime;
        this.location = location;
        this.registrationStartTime = registrationStartTime;
        this.registrationEndTime = registrationEndTime;
        this.posterURL = posterURL;
        this.eventOrganizerHardwareID = eventOrganizerHardwareID;
        this.requiresGeolocation = requiresGeolocation;

        // Initialize entrant lists
        this.waitListEntrants = new ArrayList<>();
        this.invitedEntrants = new ArrayList<>();
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
                 String eventOrganizerHardwareID, boolean requiresGeolocation) {
        this.uniqueEventID = uniqueEventID;
        this.eventName = eventName;
        this.eventDescription = eventDescription;
        this.startTime = startTime;
        this.location = location;
        this.registrationStartTime = registrationStartTime;
        this.registrationEndTime = registrationEndTime;
        this.eventOrganizerHardwareID = eventOrganizerHardwareID;
        this.requiresGeolocation = requiresGeolocation;

        // Initialize entrant lists
        this.waitListEntrants = new ArrayList<>();
        this.invitedEntrants = new ArrayList<>();
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
     * returns lottery criteria
     */
    public String getLotteryCriteria() {
        return lotteryCriteria;
    }

    public void setLotteryCriteria(String lotteryCriteria) {
        this.lotteryCriteria = lotteryCriteria;
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
     * Returns a list of entrants with pending invitations.
     *
     * @return the list of invited entrants
     */
    public ArrayList<String> getInvitedEntrants() {
        return invitedEntrants;
    }

    /**
     * Returns a list of entrant hardware IDs accepted into the event.
     *
     * @return the list of accepted entrants hardware IDs
     */
    public ArrayList<String> getAcceptedEntrants() {
        return acceptedEntrants;
    }

    /**
     * Returns a list of entrants harware IDs declined from the event.
     *
     * @return the list of declined entrants hardware IDs
     */
    public ArrayList<String> getDeclinedEntrants() {
        return declinedEntrants;
    }

    /**
     * Adds an entrant's hardware ID to the event's waitlist.
     * The entrant will only be added if they are not already in the list.
     * Makes sure that the waitlist limit has not been exceeded and
     * the registration happens within the allowed date range.
     *
     * @param entrantHardwareID the entrant hardware ID to add to the waitlist
     */
    public void addEntrantToWaitList(String entrantHardwareID) {
        if (entrantHardwareID == null || entrantHardwareID.isEmpty()) {
            throw new IllegalArgumentException("Entrant hardware ID cannot be null or empty");
        }

        if (this.waitListEntrants == null) {
            this.waitListEntrants = new ArrayList<>();
        }

        // Enforce waitlist limit (if set)
        if (this.waitlistLimit != null && this.waitListEntrants.size() >= this.waitlistLimit) {
            throw new IllegalStateException("Waitlist is full");
        }

        Date now = new Date();

        // Only enforce these if times are set
        if (this.registrationEndTime != null && now.after(this.registrationEndTime)) {
            throw new IllegalStateException("This event's registration window has ended");
        }
        if (this.registrationStartTime != null && now.before(this.registrationStartTime)) {
            throw new IllegalStateException("This event's registration window has not yet started");
        }

        // Check if entrant is already on waitlist by comparing hardware IDs
        boolean alreadyOnWaitlist = false;
        for (WaitlistEntry existingEntry : waitListEntrants) {
            if (existingEntry != null &&
                    entrantHardwareID.equals(existingEntry.getEntrantHardwareID())) {
                alreadyOnWaitlist = true;
                break;
            }
        }

        if (!alreadyOnWaitlist) {
            WaitlistEntry entry = new WaitlistEntry(entrantHardwareID);
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
     * Removes an entrant from the event's waitlist from a specific hardware ID.
     * If the entrant is not present, no changes are made.
     *
     * @param entrantHardwareID the entrant hardware ID to remove from the waitlist
     */
    public void removeEntrantFromWaitList(String entrantHardwareID) {
        if (waitListEntrants == null) return;
        waitListEntrants.removeIf(entry ->
                entrantHardwareID.equals(entry.getEntrantHardwareID()));
    }

    /**
     * Adds an entrant's hardware ID to the event's invited list.
     * The entrant will only be added if they are not already in the list.
     *
     * @param entrantHardwareID the entrant to add to the invited entrants list
     */
    public void addEntrantToInvitedList(String entrantHardwareID) {
        if (!invitedEntrants.contains(entrantHardwareID)) {
            invitedEntrants.add(entrantHardwareID);
        }
    }

    /**
     * Removes an entrant from the event's invited list.
     * If the entrant is not present, no changes are made.
     *
     * @param entrantHardwareID the entrant hardware ID to remove from the invited list
     */
    public void removeEntrantFromInvitedList(String entrantHardwareID) {
        invitedEntrants.remove(entrantHardwareID);
    }

    /**
     * Adds an entrant's hardware ID to the event's accepted list.
     * The entrant will only be added if they are not already in the list.
     *
     * @param entrantHardwareID the entrant to add to the accepted list
     */
    public void addEntrantToAcceptedList(String entrantHardwareID) {
        if (!acceptedEntrants.contains(entrantHardwareID)) {
            acceptedEntrants.add(entrantHardwareID);
        }
    }

    /**
     * Removes an entrant from the event's accepted list.
     * If the entrant is not present, no changes are made.
     *
     * @param entrantHardwareID the entrant hardware ID to remove from the accepted list
     */
    public void removeEntrantFromAcceptedList(String entrantHardwareID) {
        acceptedEntrants.remove(entrantHardwareID);
    }

    /**
     * Adds an entrant to the event's declined list.
     * The entrant will only be added if they are not already in the list.
     *
     * @param entrantHardwareID the entrant hardware ID to add to the declined list
     */
    public void addEntrantToDeclinedList(String entrantHardwareID) {
        if (!declinedEntrants.contains(entrantHardwareID)) {
            declinedEntrants.add(entrantHardwareID);
        }
    }

    /**
     * Removes an entrant from the event's declined list.
     * If the entrant is not present, no changes are made.
     *
     * @param entrantHardwareID the entrant hardware ID to remove from the declined list
     */
    public void removeEntrantFromDeclinedList(String entrantHardwareID) {
        declinedEntrants.remove(entrantHardwareID);
    }

    /**
     * Attempts to add an entrant to this event's waitlist.
     * This method enforces:
     *   The entrant is not already invited, accepted, or declined.
     *   The entrant is not already on the waitlist.
     *   The registration window is currently open (if defined).
     *   The waitlist limit has not been reached (if defined).
     *
     * @param entrantHardwareID the hardware ID of the entrant attempting to join
     * @return a WaitlistOperationResult describing the outcome
     */
    public WaitlistOperationResult joinWaitlist(String entrantHardwareID) {
        if (entrantHardwareID == null || entrantHardwareID.isEmpty()) {
            throw new IllegalArgumentException("Entrant hardware ID cannot be null or empty");
        }

        if (invitedEntrants == null) invitedEntrants = new ArrayList<>();
        if (acceptedEntrants == null) acceptedEntrants = new ArrayList<>();
        if (declinedEntrants == null) declinedEntrants = new ArrayList<>();
        if (waitListEntrants == null) waitListEntrants = new ArrayList<>();

        // Already in one of the lists?
        if (invitedEntrants.contains(entrantHardwareID)) {
            return WaitlistOperationResult.ALREADY_INVITED;
        }
        if (acceptedEntrants.contains(entrantHardwareID)) {
            return WaitlistOperationResult.ALREADY_ACCEPTED;
        }
        if (declinedEntrants.contains(entrantHardwareID)) {
            return WaitlistOperationResult.ALREADY_DECLINED;
        }

        // Already on waitlist?
        for (WaitlistEntry entry : waitListEntrants) {
            if (entry != null && entrantHardwareID.equals(entry.getEntrantHardwareID())) {
                return WaitlistOperationResult.ALREADY_ON_WAITLIST;
            }
        }

        // Check registration window
        Date now = new Date();
        if (registrationStartTime != null && now.before(registrationStartTime)) {
            return WaitlistOperationResult.REGISTRATION_NOT_STARTED;
        }
        if (registrationEndTime != null && now.after(registrationEndTime)) {
            return WaitlistOperationResult.REGISTRATION_CLOSED;
        }

        // Enforce waitlist limit (if set)
        if (waitlistLimit != null && waitListEntrants.size() >= waitlistLimit) {
            return WaitlistOperationResult.WAITLIST_FULL;
        }

        // All good – add to waitlist
        waitListEntrants.add(new WaitlistEntry(entrantHardwareID));
        return WaitlistOperationResult.SUCCESS;
    }

    /**
     * Attempts to remove an entrant from this event's waitlist.
     *
     * @param entrantHardwareID the hardware ID of the entrant attempting to leave
     * @return a WaitlistOperationResult describing the outcome (only SUCCESS or NOT_ON_WAITLIST)
     */
    public WaitlistOperationResult leaveWaitlist(String entrantHardwareID) {
        if (entrantHardwareID == null || entrantHardwareID.isEmpty()) {
            throw new IllegalArgumentException("Entrant hardware ID cannot be null or empty");
        }

        if (waitListEntrants == null || waitListEntrants.isEmpty()) {
            return WaitlistOperationResult.NOT_ON_WAITLIST;
        }

        boolean removed = waitListEntrants.removeIf(
                entry -> entry != null && entrantHardwareID.equals(entry.getEntrantHardwareID())
        );

        return removed ? WaitlistOperationResult.SUCCESS
                : WaitlistOperationResult.NOT_ON_WAITLIST;
    }

    /**
     * Determines this entrant's status relative to this event by checking
     * accepted, invited, declined, and waitlist collections.
     *
     * @param entrantHardwareID the entrant's hardware ID to look up
     * @return the EntrantStatus of the entrant for this event
     */
    public EntrantStatus getEntrantStatus(String entrantHardwareID) {
        if (entrantHardwareID == null || entrantHardwareID.isEmpty()) {
            return EntrantStatus.NONE;
        }

        if (acceptedEntrants != null && acceptedEntrants.contains(entrantHardwareID)) {
            return EntrantStatus.ACCEPTED;
        }

        if (invitedEntrants != null && invitedEntrants.contains(entrantHardwareID)) {
            return EntrantStatus.INVITED;
        }

        if (declinedEntrants != null && declinedEntrants.contains(entrantHardwareID)) {
            return EntrantStatus.DECLINED;
        }

        if (waitListEntrants != null) {
            for (WaitlistEntry entry : waitListEntrants) {
                if (entry != null &&
                        entrantHardwareID.equals(entry.getEntrantHardwareID())) {
                    return EntrantStatus.WAITLISTED;
                }
            }
        }

        return EntrantStatus.NONE;
    }

    // ONWARDS: SHOULD ONLY BE USED BY FIRESTORE!!!!!!

    /**
     * Required by Firestore: public no-arg constructor.
     * Initializes collections to avoid null checks when deserialized.
     */
    public Event() {
        this.waitListEntrants = new ArrayList<>();
        this.invitedEntrants = new ArrayList<>();
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
     * Sets the event's invited list. ONLY to be used by firestore.
     *
     * @param invitedEntrants the new list of waitlisted entrants
     */
    public void setInvitedEntrants(ArrayList<String> invitedEntrants) {
        this.invitedEntrants = Objects.requireNonNullElseGet(invitedEntrants, ArrayList::new);
    }

    /**
     * Sets the event's accepted list. ONLY to be used by firestore.
     *
     * @param acceptedEntrants the new list of accepted entrants
     */
    public void setAcceptedEntrants(ArrayList<String> acceptedEntrants) {
        this.acceptedEntrants = Objects.requireNonNullElseGet(acceptedEntrants, ArrayList::new);
    }

    /**
     * Sets the event's declined list. ONLY to be used by firestore.
     *
     * @param declinedEntrants the new list of declined entrants
     */
    public void setDeclinedEntrants(ArrayList<String> declinedEntrants) {
        this.declinedEntrants = Objects.requireNonNullElseGet(declinedEntrants, ArrayList::new);
    }

    /**
     * Checks if registration is currently open for this event.
     * Registration is open if the current time is between registrationStartTime and registrationEndTime.
     *
     * @return true if registration is open, false otherwise
     */
    public boolean isRegistrationOpen() {
        Date now = new Date();
        boolean afterStart = registrationStartTime == null || !now.before(registrationStartTime);
        boolean beforeEnd = registrationEndTime == null || !now.after(registrationEndTime);
        return afterStart && beforeEnd;
    }

    /**
     * Gets a user-friendly status message about the registration window.
     *
     * @return a status message like "Registration opens soon" or "Registration closed", or empty string if open
     */
    public String getRegistrationStatus() {
        Date now = new Date();
        if (registrationStartTime != null && now.before(registrationStartTime)) {
            return "Registration opens soon";
        } else if (registrationEndTime != null && now.after(registrationEndTime)) {
            return "Registration closed";
        }
        return ""; // Registration is open
    }

    /**
     * Checks if this Event requires Geolocation
     *
     * @return true if this Event requires Geolocation, false otherwise
     */
    public boolean getRequiresGeolocation() {
        return requiresGeolocation;
    }

    /**
     * Sets if this Event requires Geolocation
     *
     * @param requiresGeolocation true if this Event requires Geolocation, false otherwise
     */
    public void setRequiresGeolocation(boolean requiresGeolocation) {
        this.requiresGeolocation = requiresGeolocation;
    }

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