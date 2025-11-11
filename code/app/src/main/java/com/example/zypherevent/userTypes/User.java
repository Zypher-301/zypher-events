package com.example.zypherevent.userTypes;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * @author Elliot Chrystal
 * @version 1.0
 *
 * This class describes the shared features of Entrants, Organizers, and Administrators (For code
 * reusability). Users are identified primarily by their hardware ID.
 * This class implements parcelable so that It can be passed around with intent.
 */
public class User implements Serializable {
    /**
     * The unique hardware identifier associated with the user.
     */
    private String hardwareID;

    /**
     * The type of user (Entrant, Organizer, Administrator).
     */
    private UserType userType;

    /**
     * The user's first name.
     */
    private String firstName;

    /**
     * The user's last name.
     */
    private String lastName;

    /**
     * Required by Firestore: public no-arg constructor.
     * Subclasses should call setUserType(...) if they don't set it in their constructors.
     */
    public User() {
        // Leave blank — Firestore will populate fields
    }

    /**
     * Constructs a new User instance with the specified hardware ID, first name, and last name.
     *
     * @param hardwareID the unique hardware identifier for this user
     * @param firstName  the user's first name
     * @param lastName   the user's last name
     */
    public User(UserType userType, String hardwareID, String firstName, String lastName) {
        this.hardwareID = hardwareID;
        this.firstName = firstName;
        this.lastName = lastName;
        this.userType = userType;
    }

    /**
     * Returns the hardware ID associated with this user.
     *
     * @return the user's hardware ID
     */
    public String getHardwareID() {
        return hardwareID;
    }

    /**
     * Sets the hardware ID for this user.
     *
     * @param hardwareID the new hardware ID to assign
     */
    public void setHardwareID(String hardwareID) {
        this.hardwareID = hardwareID;
    }

    /**
     * Returns the user's first name.
     *
     * @return the user's first name
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Updates the user's first name.
     *
     * @param firstName the new first name to set
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Returns the user's last name.
     *
     * @return the user's last name
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Updates the user's last name.
     *
     * @param lastName the new last name to set
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Returns the type of user (Entrant, Organizer, Administrator).
     *
     * @return the user's type
     */
    public UserType getUserType() {
        return userType;
    }

    /**
     * sets the user type (required for Firestore deserialization)
     *
     * @param userType userType to be set
     */
    public void setUserType(UserType userType) {
        this.userType = userType;
    }


    /**
     * Implements equality checks for the class.
     *
     * @param o the object to compare to
     * @return if the given object and this instance are logically equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        return java.util.Objects.equals(hardwareID, user.hardwareID)
                && java.util.Objects.equals(userType, user.userType)
                && java.util.Objects.equals(firstName, user.firstName)
                && java.util.Objects.equals(lastName, user.lastName);
    }

    /**
     * This is required for collections like HashMap or HashSet to work correctly.
     * Also should be updated together equals?
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(hardwareID, userType, firstName, lastName);
    }

}