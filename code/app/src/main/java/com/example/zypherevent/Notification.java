package com.example.zypherevent;

import com.example.zypherevent.userTypes.User;
import com.google.firebase.firestore.PropertyName;

import java.util.Objects;

/**
 * @author Elliot Chrystal
 * @version 1.0
 * @see User
 *
 *      Represents a notification within the Zypher Event system. Notifications
 *      are used
 *      to communicate from Organizers to Entrants. Each notification includes
 *      identifying information
 *      about the sender and receiver, a header, a body message, and a flag
 *      indicating whether the
 *      notification has been dismissed.
 *
 */
public class Notification {

    /** The unique identifier assigned to this notification. */
    private Long notificationID;

    /** The hardware ID of the user (Organizer) who sent the notification. */
    private String sendingUserHardwareID;

    /** The hardware ID of the user (Entrant) who received the notification. */
    private String receivingUserHardwareID;

    /**
     * Indicates whether the notification has been dismissed.
     * If true, the notification will not be shown in the entrant's notifications
     * page.
     */
    private boolean dismissed;

    /** The title or header of the notification. */
    private String notificationHeader;

    /** The body text or main message content of the notification. */
    private String notificationBody;

    /**
     * The unique identifier of the event associated with this notification
     * (optional).
     */
    private Long eventID;

    /**
     * Constructs a new Notification instance with all attributes specified.
     * The notification is not dismissed by default.
     *
     * @param notificationID          the unique identifier for this notification
     * @param sendingUserHardwareID   the hardware ID of the user sending the
     *                                notification
     * @param receivingUserHardwareID the hardware ID of the user receiving the
     *                                notification
     * @param notificationHeader      the notification's title or summary
     * @param notificationBody        the detailed body text of the notification
     * @param eventID                 the unique identifier of the event associated
     *                                with this notification (can be null)
     */
    public Notification(Long notificationID, String sendingUserHardwareID, String receivingUserHardwareID,
            String notificationHeader, String notificationBody, Long eventID) {
        this.notificationID = notificationID;
        this.sendingUserHardwareID = sendingUserHardwareID;
        this.receivingUserHardwareID = receivingUserHardwareID;
        this.notificationHeader = notificationHeader;
        this.notificationBody = notificationBody;
        this.eventID = eventID;
        this.dismissed = false;
    }

    /**
     * Constructs a new Notification instance with all attributes specified,
     * excluding eventID.
     * The notification is not dismissed by default.
     *
     * @param notificationID          the unique identifier for this notification
     * @param sendingUserHardwareID   the hardware ID of the user sending the
     *                                notification
     * @param receivingUserHardwareID the hardware ID of the user receiving the
     *                                notification
     * @param notificationHeader      the notification's title or summary
     * @param notificationBody        the detailed body text of the notification
     */
    public Notification(Long notificationID, String sendingUserHardwareID, String receivingUserHardwareID,
            String notificationHeader, String notificationBody) {
        this(notificationID, sendingUserHardwareID, receivingUserHardwareID, notificationHeader, notificationBody,
                null);
    }

    /**
     * Returns the notification's unique identifier.
     *
     * @return the notification ID
     */
    @PropertyName("notificationID")
    public Long getUniqueNotificationID() {
        return notificationID;
    }

    /**
     * Updates the notification's unique identifier.
     *
     * @param notificationID the new notification ID to set
     */
    @PropertyName("notificationID")
    public void setUniqueNotificationID(Long notificationID) {
        this.notificationID = notificationID;
    }

    /**
     * Returns the hardware ID of the user who sent the notification.
     *
     * @return the sender's hardware ID
     */
    public String getSendingUserHardwareID() {
        return sendingUserHardwareID;
    }

    /**
     * Updates the hardware ID of the user who sent the notification.
     *
     * @param sendingUserHardwareID the new sender hardware ID to set
     */
    public void setSendingUserHardwareID(String sendingUserHardwareID) {
        this.sendingUserHardwareID = sendingUserHardwareID;
    }

    /**
     * Returns the hardware ID of the user who received the notification.
     *
     * @return the receiver's hardware ID
     */
    public String getReceivingUserHardwareID() {
        return receivingUserHardwareID;
    }

    /**
     * Updates the hardware ID of the user who received the notification.
     *
     * @param receivingUserHardwareID the new receiver hardware ID to set
     */
    public void setReceivingUserHardwareID(String receivingUserHardwareID) {
        this.receivingUserHardwareID = receivingUserHardwareID;
    }

    /**
     * Returns whether the notification has been dismissed.
     *
     * @return true if the notification is dismissed, false otherwise
     */
    public boolean isDismissed() {
        return dismissed;
    }

    /**
     * Updates the notification's dismissed status.
     *
     * @param dismissed true to mark the notification as dismissed, false otherwise
     */
    public void setDismissed(boolean dismissed) {
        this.dismissed = dismissed;
    }

    /**
     * Returns the notification's header or title.
     *
     * @return the notification header
     */
    public String getNotificationHeader() {
        return notificationHeader;
    }

    /**
     * Updates the notification's header or title.
     *
     * @param notificationHeader the new header text to set
     */
    public void setNotificationHeader(String notificationHeader) {
        this.notificationHeader = notificationHeader;
    }

    /**
     * Returns the main message content of the notification.
     *
     * @return the notification body
     */
    public String getNotificationBody() {
        return notificationBody;
    }

    /**
     * Updates the main message content of the notification.
     *
     * @param notificationBody the new body text to set
     */
    public void setNotificationBody(String notificationBody) {
        this.notificationBody = notificationBody;
    }

    /**
     * Returns the event ID associated with the notification.
     *
     * @return the event ID, or null if none
     */
    public Long getEventID() {
        return eventID;
    }

    /**
     * Updates the event ID associated with the notification.
     *
     * @param eventID the new event ID to set
     */
    public void setEventID(Long eventID) {
        this.eventID = eventID;
    }

    /**
     * Checks if this Notification is equal to another object.
     * 
     * @param o the object to compare with this Notification.
     * @return {@code true} if the objects are equal, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Notification notification = (Notification) o;
        return Objects.equals(notificationID, notification.notificationID) &&
                Objects.equals(sendingUserHardwareID, notification.sendingUserHardwareID) &&
                Objects.equals(receivingUserHardwareID, notification.receivingUserHardwareID) &&
                dismissed == notification.dismissed &&
                Objects.equals(notificationHeader, notification.notificationHeader) &&
                Objects.equals(notificationBody, notification.notificationBody) &&
                Objects.equals(eventID, notification.eventID);
    }

    /**
     * Generates a hash code for this Notification.
     * 
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(notificationID, sendingUserHardwareID, receivingUserHardwareID,
                dismissed, notificationHeader, notificationBody, eventID);
    }

    // ONWARDS: SHOULD ONLY BE USED BY FIRESTORE!!!!!!

    /**
     * Required by Firestore: public no-arg constructor.
     */
    public Notification() {
    }

}
