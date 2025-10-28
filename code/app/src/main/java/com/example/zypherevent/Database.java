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
 */
public class Database {

    /** Reference to the Firebase Firestore instance. */
    private FirebaseFirestore db;

    /** Reference to the Firestore collection containing user data. */
    private CollectionReference usersCollection;

    /** Reference to the Firestore collection containing event data. */
    private CollectionReference eventsCollection;

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
        extrasCollection = db.collection("extras");
    }

    /**
     * Constructs a new Database instance and initializes references with custom names
     * to the Firestore database and its key collections with custom names.
     * This allows for testing using different collections that do not corrupt
     * production data.
     */
    public Database (String usersCollectionName, String eventsCollectionName, String extrasCollectionName) {
        this.db = FirebaseFirestore.getInstance();
        usersCollection = db.collection(usersCollectionName);
        eventsCollection = db.collection(eventsCollectionName);
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
     * @return a {@link Task} representing the asynchronous database operation
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
     * @return a {@link Task} representing the asynchronous database operation
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

}
