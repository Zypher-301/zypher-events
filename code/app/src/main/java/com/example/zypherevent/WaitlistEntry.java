package com.example.zypherevent;

import com.example.zypherevent.userTypes.Entrant;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.Objects;

/**
 * Represents an entry in an event's waitlist, associating an entrant with the time they joined.
 * <p>
 * This class wraps an {@link Entrant} object along with a timestamp indicating when they joined
 * the waitlist. The timestamp is automatically set by Firestore when the entry is created.
 * This allows for tracking waitlist order and displaying join times to organizers.
 * </p>
 *
 * @author Aaron
 * @version 1.0
 * @see Entrant
 * @see Event
 */
public class WaitlistEntry {

    /**
     * The entrant who joined the waitlist.
     */
    private Entrant entrant;

    /**
     * The timestamp when the entrant joined the waitlist.
     */
    private Date timeJoined;

    /**
     * Public no-argument constructor required for Firestore deserialization.
     */
    public WaitlistEntry() {}

    /**
     * Creates a new waitlist entry for the specified entrant.
     * <p>
     * The {@code timeJoined} field will be automatically populated by Firestore
     * when this entry is saved to the database using the @ServerTimestamp annotation.
     * </p>
     *
     * @param entrant the entrant to add to the waitlist
     */
    public WaitlistEntry(Entrant entrant) {
        this.entrant = entrant;
        this.timeJoined = new Date();
    }

    /**
     * Creates a new waitlist entry with an explicit join time.
     * <p>
     * This constructor is typically used when reconstructing entries from the database
     * or for testing purposes where a specific timestamp is needed.
     * </p>
     *
     * @param entrant the entrant to add to the waitlist
     * @param timeJoined the time they joined the waitlist
     */
    public WaitlistEntry(Entrant entrant, Date timeJoined) {
        this.entrant = entrant;
        this.timeJoined = timeJoined;
    }

    /**
     * Gets the entrant associated with this waitlist entry.
     *
     * @return the entrant who joined the waitlist
     */
    public Entrant getEntrant() {
        return entrant;
    }

    /**
     * Gets the timestamp when the entrant joined the waitlist.
     *
     * @return the date and time when the entrant joined, or null if not yet set
     */
    public Date getTimeJoined() {
        return timeJoined;
    }

    /**
     * Sets the entrant for this waitlist entry.
     * Required by Firestore for deserialization.
     *
     * @param entrant the entrant to set
     */
    public void setEntrant(Entrant entrant) {
        this.entrant = entrant;
    }

    /**
     * Sets the join timestamp for this waitlist entry.
     * Required by Firestore for deserialization.
     *
     * @param timeJoined the timestamp to set
     */
    public void setTimeJoined(Date timeJoined) {
        this.timeJoined = timeJoined;
    }

    /**
     * Compares this waitlist entry to another object for equality.
     * <p>
     * Two waitlist entries are considered equal if they have the same entrant
     * and the same join time.
     * </p>
     *
     * @param o the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WaitlistEntry waitlistEntry = (WaitlistEntry) o;
        return Objects.equals(this.entrant, waitlistEntry.entrant) && Objects.equals(this.timeJoined, waitlistEntry.timeJoined);
    }

    /**
     * Generates a hash code for this waitlist entry.
     *
     * @return a hash code value based on the entrant and join time
     */
    @Override
    public int hashCode() {
       return Objects.hash(this.timeJoined, entrant);
    }

}
