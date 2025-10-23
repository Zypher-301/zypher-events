package com.example.zypherevent.userTypes;

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
        super(hardwareID, firstName, lastName);
    }
}
