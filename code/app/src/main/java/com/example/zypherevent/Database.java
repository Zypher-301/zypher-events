package com.example.zypherevent;

import android.util.Log;

import com.example.zypherevent.userTypes.Administrator;
import com.example.zypherevent.userTypes.Entrant;
import com.example.zypherevent.userTypes.Organizer;
import com.example.zypherevent.userTypes.User;
import com.example.zypherevent.userTypes.UserType;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * @author Elliot Chrystal
 * @version 1.0
 * @see User
 * @see Event
 * @see FirebaseFirestore
 *
 * Represents the database interface within the Zypher Event system. This class provides
 * methods for interacting with Firebase Firestore, allowing for storage, retrieval, and
 * removal of both User and Event data.
 * The Database class abstracts Firestore operations for user and event collections,
 * ensuring consistent handling of data throughout the system.
 *
 * Database implements serializable so that it can be passed between activities with intent.
 *
 */
public class Database {

    /** Reference to the Firebase Firestore instance. */
    private FirebaseFirestore db;

    /** Reference to the Firestore collection containing user data. */
    private CollectionReference usersCollection;

    /** Reference to the Firestore collection containing event data. */
    private CollectionReference eventsCollection;

    /** Reference to the Firestore collection containing notification data. */
    private CollectionReference notificationCollection;

    /** Reference to the Firestore collection containing extra data. */
    private CollectionReference extrasCollection;

    /**
     * Constructs a new Database instance and initializes references
     * to the Firestore database and its key collections.
     */
    public Database() {
        this.db = FirebaseFirestore.getInstance();
        usersCollection = db.collection("users");
        eventsCollection = db.collection("events");
        notificationCollection = db.collection("notifications");
        extrasCollection = db.collection("extras");
    }

    /**
     * Constructs a new Database instance and initializes references with custom names
     * to the Firestore database and its key collections with custom names.
     * This allows for testing using different collections that do not corrupt
     * production data.
     */
    public Database (String usersCollectionName, String eventsCollectionName, String notificationCollectionName, String extrasCollectionName) {
        this.db = FirebaseFirestore.getInstance();
        usersCollection = db.collection(usersCollectionName);
        eventsCollection = db.collection(eventsCollectionName);
        notificationCollection = db.collection(notificationCollectionName);
        extrasCollection = db.collection(extrasCollectionName);
    }

    /**
     * Stores or updates a user document in the Firestore "users" collection.
     *
     * @param hardwareID the unique hardware identifier of the user
     * @param user        the user object to be stored
     * @return a Task representing the asynchronous database operation
     */
    public Task<Void> setUserData(String hardwareID, User user) {
        return usersCollection
                .document(hardwareID)
                .set(user);
    }

    /**
     * Removes a user document from the Firestore "users" collection.
     *
     * @param hardwareID the unique hardware identifier of the user
     * @return a Task representing the asynchronous database operation
     */
    public Task<Void> removeUserData(String hardwareID) {
        return usersCollection
                .document(hardwareID)
                .delete();
    }

    /**
     * Retrieves a user from the Firestore "users" collection based on their hardware ID.
     * The returned User is automatically cast into the appropriate subtype (Entrant, Organizer,
     * Administrator) based on its UserType.
     * If the document does not exist or cannot be parsed, the result will be {@code null}.
     *
     * @param hardwareID the unique hardware identifier of the user
     * @return a Task that resolves to the retrieved User object, or null if not found
     */
    public Task<User> getUser(String hardwareID) {
        return usersCollection
                .document(hardwareID)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException();

                    DocumentSnapshot doc = task.getResult();

                    // If the user document doesn't exist, return null
                    if (doc == null || !doc.exists()) {
                        Log.e("Database", "Document doesn't exist for hardwareID \"" + hardwareID + "\"");
                        return null;
                    }

                    User userObject = doc.toObject(User.class);

                    if (userObject == null) {
                        Log.e("Database", "User object retrieved, but null?");
                        return null;
                    }

                    // Return correct type of object based on user type
                    if (userObject.getUserType() == UserType.ENTRANT) {
                        return doc.toObject(Entrant.class);

                    } else if (userObject.getUserType() == UserType.ORGANIZER) {
                        return doc.toObject(Organizer.class);

                    } else if (userObject.getUserType() == UserType.ADMINISTRATOR) {
                        return doc.toObject(Administrator.class);

                    } else {
                        // If all fails, unknown type
                        Log.e("Database", "Unknown user type. Not one of Entrant, Organizer, or Administrator");
                        return userObject;
                    }
                });
    }

    /**
     * Stores or updates an event document in the Firestore "events" collection.
     *
     * @param eventID the unique identifier of the event
     * @param event   the event object to be stored
     * @return a Task representing the asynchronous database operation
     */
    public Task<Void> setEventData(Long eventID, Event event) {
        return eventsCollection
                .document(String.valueOf(eventID))
                .set(event);
    }

    /**
     * Removes an event document from the Firestore "events" collection.
     *
     * @param eventID the unique identifier of the event
     * @return a Task representing the asynchronous database operation
     */
    public Task<Void> removeEventData(Long eventID) {
        return eventsCollection
                .document(String.valueOf(eventID))
                .delete();
    }

    /**
     * Retrieves an event from the Firestore "events" collection based on its event ID.
     * If the document does not exist or cannot be parsed, the result will be null.
     *
     * @param eventID the unique identifier of the event
     * @return a Task that resolves to the retrieved Event object, or null if not found
     */
    public Task<Event> getEvent(Long eventID) {
        return eventsCollection
                .document(String.valueOf(eventID))
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException();

                    DocumentSnapshot doc = task.getResult();

                    // If the event document doesn't exist, return null
                    if (doc == null || !doc.exists()) {
                        Log.e("Database", "Document doesn't exist for eventID \"" + eventID + "\"");
                        return null;
                    }

                    // Manual parsing to handle WaitlistEntry objects correctly
                    try {
                        Long uniqueEventID = doc.getLong("uniqueEventID");
                        String eventName = doc.getString("eventName");
                        String eventDescription = doc.getString("eventDescription");
                        String location = doc.getString("location");
                        String eventOrganizerHardwareID = doc.getString("eventOrganizerHardwareID");
                        String posterURL = doc.getString("posterURL");
                        
                        Date startTime = doc.getDate("startTime");
                        Date registrationStartTime = doc.getDate("registrationStartTime");
                        Date registrationEndTime = doc.getDate("registrationEndTime");
                        
                        Event event = new Event(
                                uniqueEventID,
                                eventName,
                                eventDescription,
                                startTime,
                                location,
                                registrationStartTime,
                                registrationEndTime,
                                eventOrganizerHardwareID,
                                posterURL
                        );
                        
                        // Parse optional fields
                        if (doc.contains("waitlistLimit")) {
                            Long limitLong = doc.getLong("waitlistLimit");
                            if (limitLong != null) {
                                event.setWaitlistLimit(limitLong.intValue());
                            }
                        }
                        
                        // Parse entrant lists using helper methods
                        ArrayList<WaitlistEntry> waitList = parseWaitlistEntryList(doc.get("waitListEntrants"));
                        ArrayList<Entrant> acceptedList = parseEntrantList(doc.get("acceptedEntrants"));
                        ArrayList<Entrant> declinedList = parseEntrantList(doc.get("declinedEntrants"));
                        
                        event.setWaitListEntrants(waitList);
                        event.setAcceptedEntrants(acceptedList);
                        event.setDeclinedEntrants(declinedList);
                        
                        return event;
                    } catch (Exception e) {
                        Log.e("Database", "Failed to parse event for eventID: " + eventID, e);
                        return null;
                    }
                });
    }

    /**
     * Retrieves an unused event number from firebase. Its just an off platform number incrementer
     * so that we can avoid collisions in the event IDs.
     *
     * @return a Task that resolves to the next unused event number
     */
    public Task<Long> getUniqueEventID() {

        DocumentReference uniqueRef = extrasCollection.document("uniqueIdentifierData");

        return db.runTransaction(transaction -> {
            // Read the current document snapshot
            DocumentSnapshot snapshot = transaction.get(uniqueRef);

            // Get the current event ID, or start at 0 if not present
            Long currentEventID = snapshot.getLong("curEvent");
            if (currentEventID == null) {
                throw new RuntimeException("Error calling getUniqueEventID: Current event ID is null");
            }

            // Increment by 1
            Long newEventID = currentEventID + 1;

            // Update Firestore with the new value
            transaction.update(uniqueRef, "curEvent", newEventID);

            // Return the new event ID
            return newEventID;
        });
    }

    /**
     * Stores or updates a notification in the Firestore "notifications" collection.
     *
     * @param notificationID the unique identifier of the notification
     * @param notification the notification object to be stored
     * @return a Task representing the asynchronous database operation
     */
    public Task<Void> setNotificationData(Long notificationID, Notification notification) {
        return notificationCollection
                .document(String.valueOf(notificationID))
                .set(notification);

    }

    /**
     * Removes a notification from the Firestore "notifications" collection.
     *
     * @param notificationID the unique identifier of the notification
     * @return a Task representing the asynchronous database operation
     */
    public Task<Void> removeNotificationData(Long notificationID) {
        return notificationCollection
                .document(String.valueOf(notificationID))
                .delete();
    }

    /**
     * Retrieves a notification from the Firestore "notifications" collection based on its ID.
     * If the document does not exist or cannot be parsed, the result will be null.
     *
     * @param notificationID the unique identifier of the event
     * @return a Task that resolves to the retrieved Notification object, or null if not found
     */
    public Task<Notification> getNotification(Long notificationID) {
        return notificationCollection
                .document(String.valueOf(notificationID))
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException();

                    DocumentSnapshot doc = task.getResult();

                    // If the event document doesn't exist, return null
                    if (doc == null || !doc.exists()) {
                        Log.e("Database", "Document doesn't exist for notificationID \"" + notificationID + "\"");
                        return null;
                    }

                    return doc.toObject(Notification.class);
                });
    }

    /**
     * Retrieves an unused notification number from firebase. Its just an off platform number incrementer
     * so that we can avoid collisions in the notification IDs.
     *
     * @return a Task that resolves to the next unused notification number
     */
    public Task<Long> getUniqueNotificationID() {

        DocumentReference uniqueRef = extrasCollection.document("uniqueIdentifierData");

        return db.runTransaction(transaction -> {
            // Read the current document snapshot
            DocumentSnapshot snapshot = transaction.get(uniqueRef);

            // Get the current notification ID, or start at 0 if not present
            Long currentEventID = snapshot.getLong("curNotification");
            if (currentEventID == null) {
                throw new RuntimeException("Error calling getUniqueNotificationID: Current notification ID is null");
            }

            // Increment by 1
            Long newNotifID = currentEventID + 1;

            // Update Firestore with the new value
            transaction.update(uniqueRef, "curNotification", newNotifID);

            // Return the new event ID
            return newNotifID;
        });
    }


    /**
     * Added by Arunavo Dutta
     * Retrieves all notification documents from the Firestore "notifications" collection.
     * <p>
     * This method asynchronously fetches all documents in the collection. To ensure robustness
     * against malformed data in Firestore, it manually parses each document instead of using
     * automatic deserialization. This approach prevents the entire operation from failing if a
     * single document is missing a field, such as the primitive boolean {@code dismissed},
     * by providing a safe default (false).
     * <p>
     * If a document is missing its essential {@code notificationID}, it is considered invalid
     * and will be skipped. Any other parsing errors for a single document will be logged,
     * and the process will continue with the next document.
     *
     * @return A {@code Task<List<Notification>>} that, upon successful completion, contains a list
     *         of all valid {@code Notification} objects from the database. The list will be empty
     *         if the collection contains no documents or if no documents could be successfully parsed.
     *         If the initial fetch from Firestore fails, the task will complete with an exception.
     * @author Arunavo Dutta
     */
    public Task<List<Notification>> getAllNotifications() {
        return notificationCollection
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        Log.e("Database", "Error getting notifications", task.getException());
                        throw task.getException();
                    }

                    ArrayList<Notification> notificationList = new ArrayList<>();

                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        try {
                            // Get all fields from Firebase.
                            Long id = doc.getLong("notificationID");
                            if (id == null) {
                                // A notification without an ID is invalid, skip it.
                                Log.e("Database", "Skipping notification with null ID: " + doc.getId());
                                continue;
                            }
                            String sender = doc.getString("sendingUserHardwareID");
                            String receiver = doc.getString("receivingUserHardwareID");
                            String header = doc.getString("notificationHeader");
                            String body = doc.getString("notificationBody");

                            // Use the main constructor to build the object
                            Notification notification = new Notification(id, sender, receiver, header, body);

                            // This prevents a crash if the field is missing or null.
                            boolean dismissed = doc.contains("dismissed") ? doc.getBoolean("dismissed") : false;
                            notification.setDismissed(dismissed);

                            // Add the successfully parsed notification to the list
                            notificationList.add(notification);

                        } catch (Exception e) {
                            // If one document is malformed, log it and continue.
                            Log.e("Database", "Failed to parse notification: " + doc.getId(), e);
                        }
                    }

                    return notificationList;
                });
    }

    /**
     * Added by Arunavo Dutta
     * Retrieves all event documents from the Firestore "events" collection.
     * <p>
     * This method asynchronously fetches all documents from the "events" collection.
     * The result is a {@code Task} that, upon completion, provides a {@link com.google.firebase.firestore.QuerySnapshot}.
     * The snapshot contains all event documents, which can then be iterated over and
     * converted into {@link Event} objects. This approach is safer for handling potential
     * data inconsistencies, as it allows for manual parsing of each document.
     *
     * @return A {@code Task<QuerySnapshot>} that resolves with the query result. The task
     *         will fail with an exception if the data cannot be fetched.
     * @see #getAllEventsList() for a simpler but less safe alternative.
     */
    public Task<com.google.firebase.firestore.QuerySnapshot> getAllEvents() {
        return eventsCollection.get();
    }


    /**
     * Added by Arunavo Dutta
     * Manually parses a list of HashMaps from Firestore into a proper ArrayList of Entrants.
     * <p>
     * This helper method is crucial for safely handling lists of complex objects retrieved from
     * Firestore. When Firestore returns an array of objects (like the `waitListEntrants` in an
     * event), it deserializes it into a {@code List<HashMap<String, Object>>} rather than the
     * desired {@code ArrayList<Entrant>}. Attempting to cast this directly will result in a
     * {@code ClassCastException}.
     * <p>
     * This method iterates through the raw list, treating each item as a HashMap. It then
     * extracts the values for each field, constructs a new {@link Entrant} object, and adds it
     * to a new, properly typed list. It includes robust checks to handle missing or null fields
     * (e.g., providing default values for booleans) and catches exceptions to ensure that one
     * malformed entrant object doesn't cause the entire parsing process to fail.
     *
     * @param rawList The raw list object retrieved from Firestore, expected to be a {@code List}
     *                of {@code HashMap} objects, where each map represents an entrant.
     * @return A new, properly typed {@code ArrayList<Entrant>}. If the input is not a list or is
     *         null, an empty list is returned. Malformed items within the list are skipped.
     */
    private ArrayList<Entrant> parseEntrantList(Object rawList) {
        ArrayList<Entrant> entrantList = new ArrayList<>();

        // Check if the list is null or not the expected type
        if (!(rawList instanceof List)) {
            return entrantList; // Return empty list
        }

        List<?> rawEntrantList = (List<?>) rawList;
        for (Object item : rawEntrantList) {
            // Check if the item in the list is a HashMap
            if (item instanceof HashMap) {
                try {
                    HashMap<String, Object> map = (HashMap<String, Object>) item;

                    // Get all fields from the map
                    String hardwareID = (String) map.get("hardwareID");
                    String firstName = (String) map.get("firstName");
                    String lastName = (String) map.get("lastName");
                    String email = (String) map.get("email");
                    String phone = (String) map.get("phoneNumber");

                    // Default useGeolocation to false if missing
                    boolean useGeo = map.containsKey("useGeolocation") ? (Boolean) map.get("useGeolocation") : false;

                    // Default wantsNotifications to true if missing
                    boolean wantsNotifs = map.containsKey("wantsNotifications") ? (Boolean) map.get("wantsNotifications") : true;

                    // Create the Entrant object
                    Entrant entrant = new Entrant(hardwareID, firstName, lastName, email, phone, useGeo);
                    entrant.setWantsNotifications(wantsNotifs);

                    entrantList.add(entrant);

                } catch (Exception e) {
                    // Log and skip any malformed entrant
                    Log.e("Database", "Failed to parse one entrant from list", e);
                }
            }
        }
        return entrantList;
    }



    /**
     * Added by Arunavo Dutta
     * Retrieves all event documents from the Firestore "events" collection and converts them
     * into a list of {@link Event} objects.
     * <p>
     * This method has been updated to manually parse each event document from Firestore
     * rather than relying on automatic deserialization with {@code .toObject(Event.class)}.
     * This robust approach provides several key benefits:
     * <ul>
     * <li><b>Date Handling:</b> It correctly handles date fields stored as strings in Firestore
     * (e.g., "2025-10-20") by using the {@link Utils#createWholeDayDate(String)} helper
     * method to convert them into proper {@link Date} objects as required by the {@link Event} model.</li>
     * <li><b>Casting Safety:</b> It resolves a critical {@code ClassCastException} that occurs when
     * Firestore returns a list of entrants as an {@code ArrayList<HashMap>} instead of an
     * {@code ArrayList<Entrant>}. By using the {@link #parseEntrantList(Object)} helper, it ensures type safety.</li>
     * <li><b>Error Isolation:</b> If a single event document is malformed or missing a field, this
     * method will log the error and skip that document, allowing the rest of the valid
     * events to be loaded successfully. This prevents one bad entry from crashing the entire fetch operation.</li>
     * </ul>
     *
     * @return A {@code Task<List<Event>>} that, upon successful completion, contains a list
     * of all valid {@link Event} objects from the database. The list will be empty if the
     * @author Arunavo Dutta
     */
    public Task<List<Event>> getAllEventsList() {
        return eventsCollection.get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        Log.e("Database", "Error getting events list", task.getException());
                        throw task.getException();
                    }

                    ArrayList<Event> eventList = new ArrayList<>();

                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        try {

                            Long uniqueEventID = doc.getLong("uniqueEventID");
                            String eventName = doc.getString("eventName");
                            String eventDescription = doc.getString("eventDescription");
                            String location = doc.getString("location");
                            String eventOrganizerHardwareID = doc.getString("eventOrganizerHardwareID");
                            String posterURL = doc.getString("posterURL");

                            Date startTime = doc.getDate("startTime");
                            Date registrationStartTime= doc.getDate("registrationStartTime");
                            Date registrationEndTime = doc.getDate("registrationEndTime");

                            Event event = new Event(
                                    uniqueEventID,
                                    eventName,
                                    eventDescription,
                                    startTime,
                                    location,
                                    registrationStartTime,
                                    registrationEndTime,
                                    eventOrganizerHardwareID,
                                    posterURL
                            );

                            if (doc.contains("waitlistLimit")) {
                                Long limitLong = doc.getLong("waitlistLimit");
                                if (limitLong != null) {
                                    event.setWaitlistLimit(limitLong.intValue());
                                }
                            }

                            ArrayList<WaitlistEntry> waitList = parseWaitlistEntryList(doc.get("waitListEntrants"));
                            ArrayList<Entrant> acceptedList = parseEntrantList(doc.get("acceptedEntrants"));
                            ArrayList<Entrant> declinedList = parseEntrantList(doc.get("declinedEntrants"));

                            event.setWaitListEntrants(waitList);
                            event.setAcceptedEntrants(acceptedList);
                            event.setDeclinedEntrants(declinedList);

                            eventList.add(event);

                        } catch (Exception e) {

                            Log.e("Database", "Failed to parse event: " + doc.getId(), e);
                        }
                    }
                    return eventList;
                });
    }

    /**
     * Retrieves all event documents from the Firestore "events" collection that were created
     * by a specific organizer, identified by their hardware ID.
     * <p>
     * This method queries the events collection for events where the {@code eventOrganizerHardwareID}
     * field matches the provided organizer hardware ID. It manually parses each event document
     * following the same robust pattern as {@link #getAllEventsList()} to handle:
     * <ul>
     * <li><b>Date Handling:</b> Converts date strings (yyyy-MM-dd) to Date objects using {@link Utils#createWholeDayDate(String)}</li>
     * <li><b>Casting Safety:</b> Uses {@link #parseEntrantList(Object)} to safely convert entrant lists from HashMaps</li>
     * <li><b>Error Isolation:</b> Skips malformed documents and continues processing valid ones</li>
     * </ul>
     *
     * @param organizerHardwareID The hardware ID of the organizer whose events should be retrieved
     * @return A {@code Task<List<Event>>} that, upon successful completion, contains a list
     *         of all valid {@code Event} objects created by the specified organizer. The list will
     *         be empty if the organizer has no events or if no events could be successfully parsed.
     *         The task will fail if the initial query fails.
     */
    public Task<List<Event>> getEventsByOrganizer(String organizerHardwareID) {
        return eventsCollection
                .whereEqualTo("eventOrganizerHardwareID", organizerHardwareID)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        Log.e("Database", "Error getting organizer events", task.getException());
                        throw task.getException();
                    }

                    ArrayList<Event> eventList = new ArrayList<>();

                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        try {
                            Long uniqueEventID = doc.getLong("uniqueEventID");
                            String eventName = doc.getString("eventName");
                            String eventDescription = doc.getString("eventDescription");
                            String location = doc.getString("location");
                            String eventOrganizerHardwareID = doc.getString("eventOrganizerHardwareID");
                            String posterURL = doc.getString("posterURL");

                            Date startTime = doc.getDate("startTime");
                            Date registrationStartTime= doc.getDate("registrationStartTime");
                            Date registrationEndTime = doc.getDate("registrationEndTime");

                            Event event = new Event(
                                    uniqueEventID,
                                    eventName,
                                    eventDescription,
                                    startTime,
                                    location,
                                    registrationStartTime,
                                    registrationEndTime,
                                    eventOrganizerHardwareID,
                                    posterURL
                            );

                            if (doc.contains("waitlistLimit")) {
                                Long limitLong = doc.getLong("waitlistLimit");
                                if (limitLong != null) {
                                    event.setWaitlistLimit(limitLong.intValue());
                                }
                            }

                            ArrayList<WaitlistEntry> waitList = parseWaitlistEntryList(doc.get("waitListEntrants"));
                            ArrayList<Entrant> acceptedList = parseEntrantList(doc.get("acceptedEntrants"));
                            ArrayList<Entrant> declinedList = parseEntrantList(doc.get("declinedEntrants"));

                            event.setWaitListEntrants(waitList);
                            event.setAcceptedEntrants(acceptedList);
                            event.setDeclinedEntrants(declinedList);

                            eventList.add(event);

                        } catch (Exception e) {
                            Log.e("Database", "Failed to parse event: " + doc.getId(), e);
                        }
                    }
                    return eventList;
                });
    }


    /**
     * Added by Arunavo Dutta
     * Retrieves all user documents from the Firestore "users" collection and deserializes them
     * into their specific subclasses.
     * <p>
     * This method fetches all documents from the "users" collection. It then inspects the
     * {@code userType} field of each document to determine the correct user subclass
     * (e.g., {@link Entrant}, {@link Organizer}, or {@link Administrator}) and deserializes
     * the document into an object of that specific type. This ensures that the returned list
     * contains fully-typed user objects.
     * <p>
     * This method manually parses each user to prevent the app from crashing if a single user document
     * in Firebase is malformed, has null fields, or is missing a field that the automatic
     * {@code .toObject()} converter would expect. This robust approach ensures that one bad profile
     * does not prevent the entire list from loading.
     *
     * @return A {@code Task<List<User>>} that, upon successful completion, contains a list of
     *         all user objects from the database, each cast to its appropriate subclass. If the
     *         initial data fetch fails, the task will complete with an exception. Malformed
     *         individual documents will be logged and skipped.
     * @author Arunavo Dutta
     */
    public Task<List<User>> getAllUsers() {
        return usersCollection
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        Log.e("Database", "Error getting users list", task.getException());
                        throw task.getException();
                    }

                    ArrayList<User> userList = new ArrayList<>();

                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        try {
                            // Get the type string from Firebase.
                            String userTypeStr = doc.getString("userType");
                            if (userTypeStr == null) {
                                Log.e("Database", "Skipping user with null userType: " + doc.getId());
                                continue;
                            }

                            // Convert string to Enum
                            UserType type = UserType.valueOf(userTypeStr);

                            // Get base fields common to all users
                            String hardwareID = doc.getString("hardwareID");
                            String firstName = doc.getString("firstName");
                            String lastName = doc.getString("lastName");

                            // Build the correct object based on its type
                            switch (type) {
                                case ENTRANT:
                                    // Get all Entrant-specific fields
                                    String email = doc.getString("email");
                                    String phone = doc.getString("phoneNumber");

                                    // Default useGeolocation to false if missing
                                    boolean useGeo = doc.contains("useGeolocation") ? doc.getBoolean("useGeolocation") : false;
                                    boolean wantsNotifs = doc.contains("wantsNotifications") ? doc.getBoolean("wantsNotifications") : true;

                                    // Create the object using its constructor
                                    Entrant entrant = new Entrant(hardwareID, firstName, lastName, email, phone, useGeo);
                                    entrant.setWantsNotifications(wantsNotifs);

                                    userList.add(entrant);
                                    break;

                                case ORGANIZER:
                                    // Create the object
                                    Organizer organizer = new Organizer(hardwareID, firstName, lastName);

                                    userList.add(organizer);
                                    break;

                                case ADMINISTRATOR:
                                    // Create the object
                                    Administrator admin = new Administrator(hardwareID, firstName, lastName);
                                    userList.add(admin);
                                    break;
                            }

                        } catch (Exception e) {
                            // If one document is malformed, log it and continue.
                            Log.e("Database", "Failed to parse user: " + doc.getId(), e);
                        }
                    }
                    return userList;
                });
    }




    /**
     * Added by Arunavo Dutta
     * Adds an entrant to the waitlist for a specific event using a Firestore transaction.
     * <p>
     * This method atomically adds the provided entrant to the {@code waitListEntrants} array
     * in the event's Firestore document. By using a transaction, it performs several critical
     * server-side checks before committing the change, ensuring data integrity and enforcing business logic.
     * This prevents race conditions where, for example, multiple users could join a waitlist that appears
     * to have space but is already full.
     * <p>
     * The transaction performs the following checks:
     * <ul>
     *   <li>Verifies that the event exists.</li>
     *   <li>Ensures the current time is within the event's registration window (after start and before end).</li>
     *   <li>Checks if the waitlist has reached its capacity (if a {@code waitlistLimit} is set).</li>
     *   <li>Ensures the entrant is not already on the waitlist to prevent duplicates.</li>
     * </ul>
     * If all checks pass, it creates a new {@link WaitlistEntry} with the current timestamp and
     * adds it to the list. The entire modified list is then written back to Firestore.
     * <p>
     * This method robustly parses the event document from Firestore, safely converting the
     * waitlist from a list of HashMaps into a list of {@code WaitlistEntry} objects.
     *
     * @param eventId The unique ID of the event to which the entrant will be added.
     * @param entrant The {@link Entrant} object to add to the waitlist.
     * @return A {@code Task<Void>} that completes when the transaction is successfully committed.
     *         The task will fail with a {@link RuntimeException} if the event is not found,
     *         the registration window is closed, the waitlist is full, or if any other transaction error occurs.
     * @author Arunavo Dutta
     */
    public Task<Void> addEntrantToWaitlist(String eventId, Entrant entrant) {
        // Get reference to the event document
        DocumentReference eventRef = eventsCollection.document(String.valueOf(eventId));

        // Run a transaction to perform server-side checks and add the entrant
        return db.runTransaction(transaction -> {
            // Read the event document within the transaction
            DocumentSnapshot snapshot = transaction.get(eventRef);

            if (snapshot == null || !snapshot.exists()) {
                throw new RuntimeException("Event not found");
            }

            // Get the date strings
            Date registrationStartTime = snapshot.getDate("registrationStartTime");
            Date registrationEndTime = snapshot.getDate("registrationEndTime");

            // Get waitlist limit
            Integer limit = null;
            if (snapshot.contains("waitlistLimit")) {
                Long limitLong = snapshot.getLong("waitlistLimit");
                if (limitLong != null) {
                    limit = limitLong.intValue();
                }
            }

            // Get current waitlist from the snapshot
            ArrayList<WaitlistEntry> currentWaitlist = parseWaitlistEntryList(snapshot.get("waitListEntrants"));

            // Get current waitlist size
            int waitlistSize = currentWaitlist.size();

            // Perform server-side checks
            if (limit != null && waitlistSize >= limit) {
                throw new RuntimeException("Waitlist is full");
            }

            Date now = new Date(); // Server's current time
            if (registrationEndTime != null && now.after(registrationEndTime)) {
                throw new RuntimeException("Registration window has ended");
            }
            if (registrationStartTime != null && now.before(registrationStartTime)) {
                throw new RuntimeException("Registration window has not yet started");
            }

            // Check if entrant already exists on waitlist
            boolean alreadyExists = false;
            for (WaitlistEntry existingEntry : currentWaitlist) {
                if (existingEntry.getEntrant() != null && existingEntry.getEntrant().equals(entrant)) {
                    alreadyExists = true;
                    break;
                }
            }

            if (!alreadyExists) {
                // FIX: Create a new WaitlistEntry with the current date and add it to the list manually
                WaitlistEntry entry = new WaitlistEntry(entrant, new Date()); // Pass the current time directly
                currentWaitlist.add(entry);

                // FIX: Update the entire field with the modified list instead of using arrayUnion
                transaction.update(eventRef, "waitListEntrants", currentWaitlist);
            }

            return null; // Return null on success
        });
    }


    /**
     * Added by Arunavo Dutta
     * Removes an entrant from the waitlist of a specific event using a secure transaction.
     * <p>
     * This method atomically removes the provided entrant from the {@code waitListEntrants} array
     * in the specified event's Firestore document. Since the waitlist stores {@link WaitlistEntry}
     * objects (which include timestamps), this method manually parses the waitlist, finds the
     * entry containing the specified entrant, removes it, and writes the modified list back.
     * It is typically used when an entrant chooses to leave an event they were waitlisted for.
     * </p>
     * <p>
     * To ensure data integrity and prevent race conditions, the operation is wrapped in a
     * Firestore transaction. This transaction enforces critical server-side business logic:
     * <ul>
     *   <li>It verifies that the event exists before attempting any modification.</li>
     *   <li>It checks that the current server time is within the event's active registration window,
     *   preventing users from leaving the waitlist after the registration period has closed.</li>
     *   <li>It manually parses the {@link WaitlistEntry} list using {@link #parseWaitlistEntryList(Object)}.</li>
     *   <li>It finds the entry by comparing the nested {@link Entrant} objects (not the WaitlistEntry itself).</li>
     * </ul>
     * If any of these checks fail, the transaction is aborted, and the task will fail with an exception,
     * ensuring the database remains in a consistent state.
     * </p>
     *
     * @param eventId The unique identifier of the event from which the entrant will be removed.
     * @param entrant The {@link Entrant} object to be removed from the event's waitlist.
     * @return A {@code Task<Void>} that completes when the transaction is successfully committed.
     *         The task will fail with an exception if the event is not found, the registration
     *         window is closed, or any other database error occurs.
     * @see WaitlistEntry
     * @see #parseWaitlistEntryList(Object)
     * @author Arunavo Dutta
     */ // Used by "Leave" button
    public Task<Void> removeEntrantFromWaitlist(String eventId, Entrant entrant) {
        // Get reference to the event document
        DocumentReference eventRef = eventsCollection.document(String.valueOf(eventId));

        // Run a transaction to perform server-side checks and remove the entrant
        return db.runTransaction(transaction -> {
            // Read the current state of the event
            DocumentSnapshot snapshot = transaction.get(eventRef);

            if (snapshot == null || !snapshot.exists()) {
                throw new RuntimeException("Event not found");
            }

            // Get the date strings
            Date registrationStartTime = snapshot.getDate("registrationStartTime");
            Date registrationEndTime = snapshot.getDate("registrationEndTime");

            // Perform server-side checks
            Date now = new Date(); // Server's current time
            if (registrationEndTime != null && now.after(registrationEndTime)) {
                throw new RuntimeException("Registration window has ended");
            }
            if (registrationStartTime != null && now.before(registrationStartTime)) {
                throw new RuntimeException("Registration window has not yet started");
            }

            // All checks passed, remove matching WaitlistEntry (by entrant) and write back the list
            ArrayList<WaitlistEntry> currentWaitlist = parseWaitlistEntryList(snapshot.get("waitListEntrants"));
            int indexToRemove = -1;
            for (int i = 0; i < currentWaitlist.size(); i++) {
                Entrant e = currentWaitlist.get(i).getEntrant();
                if (e != null && e.equals(entrant)) {
                    indexToRemove = i;
                    break;
                }
            }
            if (indexToRemove != -1) {
                currentWaitlist.remove(indexToRemove);
                transaction.update(eventRef, "waitListEntrants", currentWaitlist);
            }
            return null; // Return null on success
        });
    }

    public Task<Void> moveEntrantToAccepted(String eventId, Entrant entrant) {
        DocumentReference eventRef = eventsCollection.document(String.valueOf(eventId));

        return db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(eventRef);
            if (!snapshot.exists()) {
                try {
                    throw new Exception("Event not found!");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            // Get the current waitlist using our new parser
            ArrayList<WaitlistEntry> currentWaitlist = parseWaitlistEntryList(snapshot.get("waitListEntrants"));

            // Find and remove the entrant
            Entrant entrantToMove = null;
            for (int i = 0; i < currentWaitlist.size(); i++) {
                if (currentWaitlist.get(i).getEntrant().equals(entrant)) {
                    // Get the entrant from the entry and remove the entry from the list
                    entrantToMove = currentWaitlist.remove(i).getEntrant();
                    break;
                }
            }

            if (entrantToMove != null) {
                // Write the modified waitlist back to the DB
                transaction.update(eventRef, "waitListEntrants", currentWaitlist);
                // Add the plain Entrant object to the accepted list
                transaction.update(eventRef, "acceptedEntrants", FieldValue.arrayUnion(entrantToMove));
            } else {
                // Entrant wasn't on the waitlist, maybe they were already moved
                Log.w("Database", "Entrant not found on waitlist, could not move.");
            }

            return null; // Transaction success
        });
    }

    /**
     * Manually parses a list of HashMaps from Firestore into a proper ArrayList of WaitlistEntry.
     * @return the list of waitlist entries parsed into java objects
     */
    public ArrayList<WaitlistEntry> parseWaitlistEntryList(Object rawList) {
        ArrayList<WaitlistEntry> entryList = new ArrayList<>();
        if (!(rawList instanceof List)) {
            return entryList;
        }

        List<?> rawEntryList = (List<?>) rawList;
        for (Object item : rawEntryList) {
            if (item instanceof HashMap) {
                try {
                    HashMap<String, Object> map = (HashMap<String, Object>) item;

                    // Firestore nests the Entrant object as another HashMap
                    HashMap<String, Object> entrantMap = (HashMap<String, Object>) map.get("entrant");

                    // Manually build the Entrant
                    String hardwareID = (String) entrantMap.get("hardwareID");
                    String firstName = (String) entrantMap.get("firstName");
                    String lastName = (String) entrantMap.get("lastName");
                    String email = (String) entrantMap.get("email");
                    String phone = (String) entrantMap.get("phoneNumber");
                    boolean useGeo = entrantMap.containsKey("useGeolocation") ? (Boolean) entrantMap.get("useGeolocation") : false;
                    boolean wantsNotifs = entrantMap.containsKey("wantsNotifications") ? (Boolean) entrantMap.get("wantsNotifications") : true;

                    Entrant entrant = new Entrant(hardwareID, firstName, lastName, email, phone, useGeo);
                    entrant.setWantsNotifications(wantsNotifs);

                    // Get the timeJoined
                    com.google.firebase.Timestamp timestamp = (com.google.firebase.Timestamp) map.get("timeJoined");
                    Date timeJoined = (timestamp != null) ? timestamp.toDate() : null;

                    // the final WaitlistEntry object
                    WaitlistEntry entry = new WaitlistEntry(entrant);
                    entry.setTimeJoined(timeJoined); // Manually set the time
                    entryList.add(entry);

                } catch (Exception e) {
                    Log.e("Database", "Failed to parse one waitlist entry", e);
                }
            }
        }
        return entryList;
    }
}
