package com.example.zypherevent.userTypes;

import android.os.Parcel;
import android.os.Parcelable;

import com.example.zypherevent.Event;
import com.google.firebase.firestore.GeoPoint;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

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
public class Entrant extends User implements Serializable {

    /** The entrant's email address. */
    private String email;

    /** The entrant's phone number (optional). */
    private String phoneNumber;

    /** Whether the entrant has opted into geolocation. */
    private boolean useGeolocation;

    /** The entrant's location (optional). */
    private transient GeoPoint location; // Firestore-native location

    /** Whether the entrant wants notifications. */
    private boolean wantsNotifications;

    /** A list of event IDs the entrant has registered for. */
    private ArrayList<Long> registeredEventHistory;

    /**
     * Constructs a new Entrant instance with all attributes specified. (Including phone number)
     *
     * @param hardwareID     the unique hardware identifier for this entrant
     * @param firstName      the entrant's first name
     * @param lastName       the entrant's last name
     * @param email          the entrant's email address
     * @param phoneNumber    the entrant's phone number
     * @param useGeolocation whether the entrant has opted into geolocation
     */
    public Entrant(String hardwareID, String firstName, String lastName, String email, String phoneNumber, boolean useGeolocation) {
        super(UserType.ENTRANT, hardwareID, firstName, lastName);
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.useGeolocation = useGeolocation;
        this.wantsNotifications = true;
        this.registeredEventHistory = new ArrayList<Long>();
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
        super(UserType.ENTRANT, hardwareID, firstName, lastName);
        this.email = email;
        this.phoneNumber = null;
        this.useGeolocation = false;
        this.wantsNotifications = true;
        this.registeredEventHistory = new ArrayList<>();
    }

    /**
     * Writes the object to the output stream.
     *
     * @param out the output stream to write to
     * @throws IOException if an I/O error occurs
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        // write all non-transient fields
        out.defaultWriteObject();

        // then manually write location
        if (location != null) {
            out.writeBoolean(true);
            out.writeDouble(location.getLatitude());
            out.writeDouble(location.getLongitude());
        } else {
            out.writeBoolean(false);
        }
    }

    /**
     * Reads the object from the input stream.
     *
     * @param in the input stream to read from
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if the class of a serialized object cannot be found
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        // read all non-transient fields
        in.defaultReadObject();

        // then manually read location
        boolean hasLocation = in.readBoolean();
        if (hasLocation) {
            double lat = in.readDouble();
            double lng = in.readDouble();
            location = new GeoPoint(lat, lng);
        } else {
            location = null;
        }
    }

    /**
     * Returns the entrant's location (optional).
     *
     * @return the entrant's location
     */
    public GeoPoint getLocation() {
        return location;
    }

    /**
     * Updates the entrant's location.
     *
     * @param location the new location to set
     */
    public void setLocation(GeoPoint location) {
        this.location = location;
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
     * Returns a list of event IDs that the entrant has registered for.
     *
     * @return a list containing the entrant's registered event IDs
     */
    public ArrayList<Long> getRegisteredEventHistory() {
        return registeredEventHistory;
    }

    /**
     * Adds a new event ID to the entrant's registered event history.
     * The event will only be added if it is not already in the list.
     *
     * @param eventID the event ID to add to the entrant's registration history
     */
    public void addEventToRegisteredEventHistory(Long eventID) {
        // Only add event if it's not already in the history
        if (!registeredEventHistory.contains(eventID)) {
            registeredEventHistory.add(eventID);
        }
    }

    /**
     * Removes an event ID from the entrant's registered event history.
     * If the event ID is not present, no changes are made.
     *
     * @param eventID the event ID to remove from the entrant's registration history
     */
    public void removeEventFromRegisteredEventHistory(Long eventID) {
        // No contains check needed, as .remove already checks for existence internally
        registeredEventHistory.remove(eventID);
    }

    /**
     * Checks if this Entrant is equal to another object.
     * @param o the object to compare with this Entrant.
     * @return {@code true} if the objects are equal, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        // check parent equality
        if (!super.equals(o)) return false;

        Entrant entrant = (Entrant) o;

        return Objects.equals(email, entrant.email)
                && Objects.equals(phoneNumber, entrant.phoneNumber);
    }

    /**
     * Generates a hash code for this Entrant. Has to be implemented
     * when equals is implemented.
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), email, phoneNumber);
    }


    /**
     * Returns whether the entrant has opted into geolocation.
     *
     * @return true if geolocation is enabled, false otherwise
     */
    public boolean getUseGeolocation() {
        return useGeolocation;
    }

    /**
     * Updates the entrant's geolocation setting.
     *
     * @param useGeolocation true to enable geolocation, false to disable
     */
    public void setUseGeolocation(boolean useGeolocation) {
        this.useGeolocation = useGeolocation;
    }

    /**
     * Returns whether the entrant wants notifications.
     *
     * @return true if notifications are enabled, false otherwise
     */
    public boolean getWantsNotifications() {
        return wantsNotifications;
    }

    /**
     * Updates the entrant's notification setting.
     *
     * @param wantsNotifications true to enable notifications, false to disable
     */
    public void setWantsNotifications(boolean wantsNotifications) {
        this.wantsNotifications = wantsNotifications;
    }

    // -------------------- ONWARDS: SHOULD ONLY BE USED BY FIRESTORE!!!!!! --------------------

    /**
     * Required by Firestore: public no-arg constructor. ONLY to be used by firestore!
     * Initializes collections to avoid null checks when deserialized.
     */
    public Entrant() {
        // Ensure the type is set
        setUserType(UserType.ENTRANT);
        this.registeredEventHistory = new ArrayList<Long>();
    }

    /**
     * Sets the entrant's registered event history. ONLY to be used by firestore!
     *
     * @param registeredEventHistory the new list of registered event IDs
     */
    public void setRegisteredEventHistory(ArrayList<Long> registeredEventHistory) {
        this.registeredEventHistory =
                Objects.requireNonNullElseGet(registeredEventHistory, ArrayList::new);
    }
}
