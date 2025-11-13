package com.example.zypherevent.model;

import com.google.firebase.firestore.GeoPoint;

/**
 * @author Elliot Chrystal
 * @version 1.0
 * @see GeoPoint
 *
 * Represents a geographic point on a map within the Zypher Event system.
 * A MapPoint contains a GeoPoint from Firebase for storing latitude and longitude coordinates,
 * along with a descriptive label to identify the location.
 */
public class MapPoint {

    /** The geographic point stored as a Firebase GeoPoint. */
    private GeoPoint firebaseGeoPoint;

    /** The descriptive label for this map point (Name of Entrant on map). */
    private String label;

    /**
     * Represents the acceptance status of an entrant shown on the map.
     */
    public enum Status {
        /** Entrant has been waitlisted. */
        WAITLISTED,

        /** Entrant has been accepted. */
        ACCEPTED,

        /** Entrant has been denied. */
        DENIED
    }

    /** The acceptance status of the entrant represented by this point. */
    private Status status;

    /**
     * Constructs a new MapPoint with the specified coordinates, label, and status.
     *
     * @param firebaseGeoPoint the GeoPoint representing the location's coordinates
     * @param label            a human-readable label describing the point
     * @param status           the acceptance status of the entrant
     */
    public MapPoint(GeoPoint firebaseGeoPoint, String label, Status status) {
        this.firebaseGeoPoint = firebaseGeoPoint;
        this.label = label;
        this.status = status;
    }

    /**
     * Returns the GeoPoint representing this map point's coordinates.
     *
     * @return the Firebase GeoPoint for this map point
     */
    public GeoPoint getFirebaseGeoPoint() {
        return firebaseGeoPoint;
    }

    /**
     * Updates the GeoPoint representing this map point's coordinates.
     *
     * @param firebaseGeoPoint the new GeoPoint to assign
     */
    public void setFirebaseGeoPoint(GeoPoint firebaseGeoPoint) {
        this.firebaseGeoPoint = firebaseGeoPoint;
    }

    /**
     * Returns the descriptive label associated with this map point.
     *
     * @return the label for this map point
     */
    public String getLabel() {
        return label;
    }

    /**
     * Updates the descriptive label associated with this map point.
     *
     * @param label the new label to assign
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Returns the acceptance status of this entrant.
     *
     * @return the current status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Updates the acceptance status of this entrant.
     *
     * @param status the new status to assign
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Returns the latitude of this map point.
     *
     * @return the latitude value from the GeoPoint
     */
    public double getLatitude() {
        return firebaseGeoPoint.getLatitude();
    }

    /**
     * Returns the longitude of this map point.
     *
     * @return the longitude value from the GeoPoint
     */
    public double getLongitude() {
        return firebaseGeoPoint.getLongitude();
    }
}
