package com.example.zypherevent.userTypes;

import com.example.zypherevent.Event;

import java.util.ArrayList;

/**
 * @author Elliot Chrystal
 * @version 1.0
 * @see User
 *
 * Represents an administrator in the Zypher Event system. Administrators are users with elevated
 * privileges who oversee and manage system operations. This class primarily exists to differentiate
 * user types within the system, extending user for consistency and type representation purposes.
 *
 */
public class Administrator extends User {

    /**
     * Constructs a new Administrator instance with the specified hardware ID, first name, and last name.
     *
     * @param hardwareID the unique hardware identifier for this administrator
     * @param firstName  the administrator's first name
     * @param lastName   the administrator's last name
     */
    public Administrator(String hardwareID, String firstName, String lastName) {
        super(UserType.ADMINISTRATOR, hardwareID, firstName, lastName);
    }

    // -------------------- ONWARDS: SHOULD ONLY BE USED BY FIRESTORE!!!!!! --------------------

    /**
     * Required by Firestore: public no-arg constructor.
     * Initializes collections to avoid null checks when deserialized.
     */
    public Administrator() {
        // Ensure the type is set
        setUserType(UserType.ADMINISTRATOR);
    }

}
