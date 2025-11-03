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
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.Serializable;
import java.util.ArrayList;
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

                    return doc.toObject(Event.class);
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
     * This method fetches all documents in the collection and converts them into a list
     * of {@link Notification} objects. If the fetch operation fails, the task will
     * complete with an exception.
     *
     * @return A {@code Task<List<Notification>>} that, upon successful completion, contains a list
     *         of all {@code Notification} objects from the database. The list will be empty
     *         if the collection is empty.
     */
    public Task<List<Notification>> getAllNotifications() {
        return notificationCollection
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        Log.e("Database", "Error getting notifications", task.getException());
                        throw task.getException();
                    }
                    // Automatically convert all documents to Notification objects
                    return task.getResult().toObjects(Notification.class);
                });
    }

    /**
     * Added by Arunavo Dutta
     * Retrieves all event documents from the Firestore "events" collection.
     * This method returns a Task that, upon completion, provides a {@code QuerySnapshot}
     * containing all documents in the "events" collection. To get a list of {@code Event}
     * objects, you can iterate through the snapshot's documents and convert each one
     * to an {@code Event} object.
     *
     * @return A {@code Task<QuerySnapshot>} that resolves with the query result. The task
     *         will fail if the data cannot be fetched.
     */
    public Task<com.google.firebase.firestore.QuerySnapshot> getAllEvents() {
        return eventsCollection.get();
    }


    /**
     * Added by Arunavo Dutta
     * Retrieves all event documents from the Firestore "events" collection.
     * <p>
     * FOR ENTRANT: Returns a simple, automatically converted List<Event>.
     * This is less safe (will fail if *any* document has bad data) but
     * is much simpler to use in the fragment.
     *
     * @return A {@code Task<List<Event>>} that resolves with a list of all Event objects.
     */
    public Task<List<Event>> getAllEventsList() {
        return eventsCollection.get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        Log.e("Database", "Error getting events list", task.getException());
                        throw task.getException();
                    }
                    // Automatically convert all documents to Event objects
                    return task.getResult().toObjects(Event.class);
                });
    }


    /**
     * Added by Arunavo Dutta
     * Retrieves all user documents from the Firestore "users" collection.
     * This method fetches all documents and correctly deserializes each one into its
     * specific subclass (e.g., {@link Entrant}, {@link Organizer}, {@link Administrator})
     * based on the {@code userType} field stored in Firestore.
     *
     * @return A {@code Task<List<User>>} that resolves with a list containing all user objects.
     *         The task will fail if the data cannot be fetched or parsed.
     */
    public Task<List<User>> getAllUsers() {
        return usersCollection
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    // Complex because we must deserialize into subtypes
                    ArrayList<User> userList = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        User baseUser = doc.toObject(User.class);
                        if (baseUser == null) continue;

                        // Re-deserialize into the correct subclass
                        if (baseUser.getUserType() == UserType.ENTRANT) {
                            userList.add(doc.toObject(Entrant.class));
                        } else if (baseUser.getUserType() == UserType.ORGANIZER) {
                            userList.add(doc.toObject(Organizer.class));
                        } else if (baseUser.getUserType() == UserType.ADMINISTRATOR) {
                            userList.add(doc.toObject(Administrator.class));
                        }
                    }
                    return userList;
                });
    }


    /**
     * Added by Arunavo Dutta
     * Adds an entrant to the waitlist of a specific event.
     * <p>
     * This method updates the "waitListEntrants" array field in the corresponding
     * event document in Firestore by adding the provided entrant object. It uses
     * an atomic `arrayUnion` operation to prevent duplicate entries.
     *
     * @param eventId The unique identifier of the event to which the entrant will be added.
     * @param entrant The entrant object to be added to the event's waitlist.
     * @return A {@code Task<Void>} representing the asynchronous database operation. The task
     *         will complete successfully if the update is committed, or fail with an
     *         exception if the operation is unsuccessful.
     */
    // Used by "Join" button
    public Task<Void> addEntrantToWaitlist(String eventId, Entrant entrant) {
        DocumentReference eventRef = eventsCollection.document(String.valueOf(eventId));
        return eventRef.update("waitListEntrants", com.google.firebase.firestore.FieldValue.arrayUnion(entrant));
    }

    /**
     * Added by Arunavo Dutta
     * Removes an entrant from the waitlist of a specific event.
     * This is typically used when an entrant decides to leave an event they were waitlisted for.
     *
     * @param eventId The ID of the event from which to remove the entrant.
     * @param entrant The {@link Entrant} object to be removed from the event's waitlist.
     * @return A {@code Task<Void>} representing the asynchronous Firestore operation.
     */
    // Used by "Leave" button
    public Task<Void> removeEntrantFromWaitlist(String eventId, Entrant entrant) {
        DocumentReference eventRef = eventsCollection.document(String.valueOf(eventId));
        return eventRef.update("waitListEntrants", com.google.firebase.firestore.FieldValue.arrayRemove(entrant));
    }


}
