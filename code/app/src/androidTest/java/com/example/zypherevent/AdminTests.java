package com.example.zypherevent;


import com.example.zypherevent.userTypes.Administrator;
import com.example.zypherevent.userTypes.Entrant;
import com.example.zypherevent.userTypes.Organizer;
import com.example.zypherevent.userTypes.User;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import org.junit.*;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Instrumented tests for the business logic implemented in the Administrator UI fragments.
 * <p>
 * This test class verifies the core functionalities available to an Administrator by directly
 * interacting with a Firebase Firestore test database. It ensures the logic used in
 * {@code AdminEventsFragment}, {@code AdminProfileFragment}, and {@code AdminNotificationLogFragment}
 * behaves as expected.
 * <p>
 * To prevent interference with production data, these tests operate on a separate set of
 * Firestore collections (e.g., "test_users", "test_events"). All test data created during
 * the execution of this suite is automatically cleaned up afterward.
 * <p>
 * This class covers the following user stories:
 * <ul>
 *     <li><b>US 03.04.01 (Admin Browse Events):</b> Verifies that all events can be successfully retrieved from the database.</li>
 *     <li><b>US 03.05.01 (Admin Browse Profiles):</b> Verifies that all user profiles can be retrieved and correctly deserialized into their specific types (Entrant, Organizer, Administrator).</li>
 *     <li><b>US 03.08.01 (Admin Browse Notifications):</b> Verifies that all notification logs can be successfully retrieved.</li>
 *     <li><b>US 03.02.01 & 03.07.01 (Admin Remove Profiles):</b> Specifically tests the cascading delete logic, ensuring that when an Organizer's profile is removed, all events created by that Organizer are also removed.</li>
 * </ul>
 */
@RunWith(AndroidJUnit4.class)
public class AdminTests {

    // Use separate test collections to avoid changing production data
    private static final String TEST_USERS_COLLECTION = "test_users";
    private static final String TEST_EVENTS_COLLECTION = "test_events";
    private static final String TEST_NOTIFICATIONS_COLLECTION = "test_notifications";
    private static final String TEST_EXTRAS_COLLECTION = "test_extras";

    // Start unique IDs at a high number
    private static final Long TEST_ID_START_VALUE = 910000L;

    private static Database testDatabase;
    private static FirebaseFirestore firestoreDb;
    private List<User> usersToClean = new ArrayList<>();
    private List<Event> eventsToClean = new ArrayList<>();
    private List<Notification> notificationsToClean = new ArrayList<>();

    /**
     * Runs once before all tests in the class.
     *
     * This method initializes the connection to a dedicated test database to ensure that
     * production data is not affected by the tests. It also resets the unique ID counters
     * for events and notifications to a known starting value ({@code TEST_ID_START_VALUE}).
     * This provides a predictable state for tests that rely on unique ID generation,
     * ensuring test reliability and reproducibility.
     *
     * @throws ExecutionException if a Firebase task fails to execute.
     * @throws InterruptedException if a Firebase task is interrupted.
     */
    @BeforeClass
    public static void setUpClass() throws ExecutionException, InterruptedException {
        testDatabase = new Database(TEST_USERS_COLLECTION, TEST_EVENTS_COLLECTION, TEST_NOTIFICATIONS_COLLECTION, TEST_EXTRAS_COLLECTION);
        firestoreDb = FirebaseFirestore.getInstance();

        // Create/reset counters doc up-front
        resetUniqueCounters();

        // Optional: also purge once at class start to guarantee a clean run
        clearCollection(TEST_USERS_COLLECTION);
        clearCollection(TEST_EVENTS_COLLECTION);
        clearCollection(TEST_NOTIFICATIONS_COLLECTION);
    }

    /**
     * Runs once after all tests are complete.
     * Cleans up the test data by deleting the document that holds the unique ID counters
     * from the 'test_extras' collection in Firestore. This ensures that a subsequent test run
     * will start with a fresh, predictable set of unique IDs.
     *
     * @throws ExecutionException if a Firebase task fails.
     * @throws InterruptedException if a Firebase task is interrupted.
     */
    @AfterClass
    public static void cleanUpClass() throws ExecutionException, InterruptedException {
        DocumentReference uniqueRef = firestoreDb
                .collection(TEST_EXTRAS_COLLECTION)
                .document("uniqueIdentifierData");
        Tasks.await(uniqueRef.delete());

        // really make sure its gone lol
        clearCollection(TEST_USERS_COLLECTION);
        clearCollection(TEST_EVENTS_COLLECTION);
        clearCollection(TEST_NOTIFICATIONS_COLLECTION);
    }

    /**
     * Runs before each test method.
     * Clears the tracking lists for users, events, and notifications to ensure
     * that the cleanup process in {@code tearDown()} only affects the data
     * created by the upcoming test.
     */
    @Before
    public void setUp() throws ExecutionException, InterruptedException {
        // Clear tracking lists
        usersToClean.clear();
        eventsToClean.clear();
        notificationsToClean.clear();

        // Ensure clean slate for this test
        clearCollection(TEST_USERS_COLLECTION);
        clearCollection(TEST_EVENTS_COLLECTION);
        clearCollection(TEST_NOTIFICATIONS_COLLECTION);

        // Reset counters so IDs are predictable per test
        resetUniqueCounters();
    }

    /**
     * Runs after each @Test method.
     * Reliably cleans up any data created during the test by iterating through
     * tracking lists (usersToClean, eventsToClean, notificationsToClean) and
     * deleting each entry from the Firestore test collections. This ensures
     * that each test starts with a clean slate and no test data is left behind.
     *
     * @throws ExecutionException if a Firebase task fails.
     * @throws InterruptedException if a Firebase task is interrupted.
     */
    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        // Clean up all created test data
        for (User user : usersToClean) {
            Tasks.await(testDatabase.removeUserData(user.getHardwareID()));
        }
        for (Event event : eventsToClean) {
            Tasks.await(testDatabase.removeEventData(event.getUniqueEventID()));
        }
        for (Notification notification : notificationsToClean) {
            Tasks.await(testDatabase.removeNotificationData(notification.getUniqueNotificationID()));
        }
    }

    private static void clearCollection(String collection)
            throws ExecutionException, InterruptedException {
        QuerySnapshot qs = Tasks.await(firestoreDb.collection(collection).get());
        if (qs.isEmpty()) return;
        WriteBatch batch = firestoreDb.batch();
        for (DocumentSnapshot doc : qs.getDocuments()) {
            batch.delete(doc.getReference());
        }
        Tasks.await(batch.commit());
    }

    private static void resetUniqueCounters()
            throws ExecutionException, InterruptedException {
        DocumentReference uniqueRef = firestoreDb
                .collection(TEST_EXTRAS_COLLECTION)
                .document("uniqueIdentifierData");

        Map<String, Object> data = new HashMap<>();
        data.put("curEvent", TEST_ID_START_VALUE);
        data.put("curNotification", TEST_ID_START_VALUE);
        Tasks.await(uniqueRef.set(data));
    }

    // --- ADMIN BROWSE TESTS ---

    /**
     * Tests the administrator's ability to browse all events (US 03.04.01).
     * <p>
     * This test simulates the scenario where an administrator browses the list of all events
     * in the system. It verifies that the {@code getAllEventsList} method correctly fetches
     * every event document from the Firestore database and deserializes them into a list
     * of {@code Event} objects.
     * </p>
     * <p>
     * Test Steps:
     * <ol>
     *     <li><b>Setup:</b> Creates and saves three distinct {@code Event} objects to the test database.</li>
     *     <li><b>Execute:</b> Calls {@code testDatabase.getAllEventsList()}, which is the method used by the admin UI to retrieve all events.</li>
     *     <li><b>Assert:</b>
     *         <ul>
     *             <li>Checks that the returned list is not null.</li>
     *             <li>Verifies that the size of the list matches the number of created events (3).</li>
     *             <li>Confirms that each of the created events is present in the fetched list.</li>
     *         </ul>
     *     </li>
     * </ol>
     *
     * @throws ExecutionException if the database tasks fail.
     * @throws InterruptedException if the database tasks are interrupted.
     */
    @Test
    public void testAdminBrowseEvents() throws ExecutionException, InterruptedException, ParseException {
        // Setup: Create 3 events
        Event event1 = createTestEvent("Event 1", "org-id-1");
        Event event2 = createTestEvent("Event 2", "org-id-1");
        Event event3 = createTestEvent("Event 3", "org-id-2");

        // Execute: Call the method used by AdminEventsFragment
        List<Event> fetchedEvents = Tasks.await(testDatabase.getAllEventsList());

        // Assert: Check if all events were fetched
        assertNotNull("Fetched event list should not be null", fetchedEvents);
        assertEquals("Fetched event list should contain 3 events", 3, fetchedEvents.size());
        assertTrue("List should contain event 1", fetchedEvents.contains(event1));
        assertTrue("List should contain event 2", fetchedEvents.contains(event2));
        assertTrue("List should contain event 3", fetchedEvents.contains(event3));
    }

    /**
     * Tests US 03.05.01: As an administrator, I want to be able to browse profiles.
     * This test verifies that the logic for loading all users from the database functions correctly.
     * It specifically checks that the {@code getAllUsers} method correctly deserializes user documents
     * from Firestore into their appropriate subclasses ({@link Entrant}, {@link Organizer}, {@link Administrator})
     * based on the 'userType' field in the database.
     *
     * @throws ExecutionException   if a task fails.
     * @throws InterruptedException if a task is interrupted.
     */
    @Test
    public void testAdminBrowseProfiles() throws ExecutionException, InterruptedException {
        // Setup: Create one of each user type
        User entrant = createTestEntrant("test-entrant-browse");
        User organizer = createTestOrganizer("test-organizer-browse");
        User admin = createTestAdmin("test-admin-browse");

        // Execute: Call the method used by AdminProfileFragment
        List<User> fetchedUsers = Tasks.await(testDatabase.getAllUsers());

        // Assert: Check list size and types
        assertNotNull("Fetched user list should not be null", fetchedUsers);
        assertEquals("Fetched user list should contain 3 users", 3, fetchedUsers.size());


        int entrantCount = 0;
        int organizerCount = 0;
        int adminCount = 0;

        for (User user : fetchedUsers) {
            if (user instanceof Entrant) {
                entrantCount++;
                assertEquals("Entrant ID does not match", "test-entrant-browse", user.getHardwareID());
            } else if (user instanceof Organizer) {
                organizerCount++;
                assertEquals("Organizer ID does not match", "test-organizer-browse", user.getHardwareID());
            } else if (user instanceof Administrator) {
                adminCount++;
                assertEquals("Admin ID does not match", "test-admin-browse", user.getHardwareID());
            }
        }

        assertEquals("List should contain 1 Entrant", 1, entrantCount);
        assertEquals("List should contain 1 Organizer", 1, organizerCount);
        assertEquals("List should contain 1 Admin", 1, adminCount);
    }

    /**
     * Tests US 03.08.01: As an administrator, I want to review logs of all notifications.
     * <p>
     * This test verifies that the {@link Database#getAllNotifications()} method, which is used by the
     * AdminNotificationLogFragment, correctly fetches all notification documents from the
     * Firestore database.
     * </p>
     * <p>
     * It performs the following steps:
     * <ol>
     *     <li>Creates three distinct {@link Notification} objects and saves them to the test database.</li>
     *     <li>Calls the {@code getAllNotifications()} method to retrieve the list of all notifications.</li>
     *     <li>Asserts that the fetched list is not null, contains exactly three items, and includes
     *         each of the notifications that were created.</li>
     * </ol>
     *
     * @throws ExecutionException   if a Firebase task fails with an exception.
     * @throws InterruptedException if a Firebase task is interrupted.
     */
    @Test
    public void testAdminBrowseNotificationLogs() throws ExecutionException, InterruptedException {
        // Setup: Create 3 notifications
        Notification notif1 = createTestNotification("org-1", "ent-1", "Header 1");
        Notification notif2 = createTestNotification("org-1", "ent-2", "Header 2");
        Notification notif3 = createTestNotification("org-2", "ent-1", "Header 3");

        // Execute: Call the method used by AdminNotificationLogFragment
        List<Notification> fetchedNotifications = Tasks.await(testDatabase.getAllNotifications());

        // Assert: Check if all notifications were fetched
        assertNotNull("Fetched notification list should not be null", fetchedNotifications);
        assertEquals("Fetched notification list should contain 3 notifications", 3, fetchedNotifications.size());
        assertTrue("List should contain notification 1", fetchedNotifications.contains(notif1));
        assertTrue("List should contain notification 2", fetchedNotifications.contains(notif2));
        assertTrue("List should contain notification 3", fetchedNotifications.contains(notif3));
    }

    /**
     * Tests US 03.06.01: As an administrator, I want to browse images.
     * * Logic:
     * 1. Create two events: one with a poster URL, one without.
     * 2. Fetch all events.
     * 3. Filter the list to only those with poster URLs (simulating AdminImagesFragment logic).
     * 4. Assert that only the event with the poster is identified as having an image.
     */
    @Test
    public void testAdminBrowseImages() throws ExecutionException, InterruptedException, ParseException {
        // Setup: Create event WITH poster
        Event eventWithPoster = createTestEvent("Poster Event", "org-poster");
        eventWithPoster.setPosterURL("https://example.com/poster.jpg");
        Tasks.await(testDatabase.setEventData(eventWithPoster.getUniqueEventID(), eventWithPoster));

        // Setup: Create event WITHOUT poster
        Event eventNoPoster = createTestEvent("No Poster Event", "org-no-poster");
        // Default createTestEvent leaves posterURL null or we explicitly set it null
        eventNoPoster.setPosterURL(null);
        Tasks.await(testDatabase.setEventData(eventNoPoster.getUniqueEventID(), eventNoPoster));

        // Execute: Fetch all events
        List<Event> allEvents = Tasks.await(testDatabase.getAllEventsList());

        // Simulate AdminImagesFragment filtering
        List<Event> eventsWithImages = new ArrayList<>();
        for (Event e : allEvents) {
            if (e.getPosterURL() != null && !e.getPosterURL().isEmpty()) {
                eventsWithImages.add(e);
            }
        }

        // Assert
        assertTrue("Should contain the event with poster", eventsWithImages.contains(eventWithPoster));
        assertFalse("Should NOT contain event without poster", eventsWithImages.contains(eventNoPoster));
    }

    // --- ADMIN DELETE TESTS ---

    /**
     * Tests US 03.01.01: As an administrator, I want to be able to remove events.
     * * Logic:
     * 1. Create a test event.
     * 2. Call the database removal method.
     * 3. Assert that the event can no longer be retrieved.
     */
    @Test
    public void testAdminDeleteEvent() throws ExecutionException, InterruptedException, ParseException {
        // Setup: Create an event
        Event event = createTestEvent("Event To Delete", "org-id-delete");
        Long eventId = event.getUniqueEventID();

        // Verify it exists
        Event fetchedBefore = Tasks.await(testDatabase.getEvent(eventId));
        assertNotNull("Event should exist before deletion", fetchedBefore);

        // Execute: Delete the event (Logic from AdminEventsFragment)
        Tasks.await(testDatabase.removeEventData(eventId));

        // Assert: Check if it is gone
        Event fetchedAfter = Tasks.await(testDatabase.getEvent(eventId));
        assertNull("Event should be null after deletion", fetchedAfter);

        // Remove from cleanup list since it's already deleted
        eventsToClean.remove(event);
    }

    /**
     * Tests US 03.02.01: As an administrator, I want to be able to remove profiles.
     * * Logic:
     * 1. Create a standard Entrant profile.
     * 2. Call the database removal method.
     * 3. Assert the user object returns null.
     */
    @Test
    public void testAdminDeleteEntrantProfile() throws ExecutionException, InterruptedException {
        // Setup: Create an entrant
        Entrant entrant = createTestEntrant("entrant-to-delete");
        String hardwareId = entrant.getHardwareID();

        // Verify existence
        User userBefore = Tasks.await(testDatabase.getUser(hardwareId));
        assertNotNull("User should exist before deletion", userBefore);

        // Execute: Remove user (Logic from AdminProfileFragment)
        Tasks.await(testDatabase.removeUserData(hardwareId));

        // Assert: User is gone
        User userAfter = Tasks.await(testDatabase.getUser(hardwareId));
        assertNull("User should be null after deletion", userAfter);

        // Remove from cleanup list
        usersToClean.remove(entrant);
    }

    /**
     * Tests US 03.02.01 (Admin remove profile) and US 03.07.01 (Organizer removed, events removed).
     *
     * This test verifies the cascading delete logic implemented in the app, which is
     * triggered when an administrator deletes an Organizer's profile. It ensures that when an
     * Organizer is deleted, all events created by that Organizer are also deleted, while
     * other unrelated users and events remain unaffected.
     *
     * The test simulates the multi-step process:
     * 1. Creates an Organizer, an unrelated Entrant, two events by the Organizer, and one
     *    unrelated event.
     * 2. Finds and deletes all events associated with the target Organizer.
     * 3. Deletes the Organizer's user profile.
     * 4. Asserts that the Organizer and their events are gone from the database.
     * 5. Asserts that the unrelated Entrant and event still exist.
     *
     * @throws ExecutionException if a Firebase task fails.
     * @throws InterruptedException if a Firebase task is interrupted.
     */
    @Test
    public void testAdminDeleteOrganizerAndCascadeEvents() throws ExecutionException, InterruptedException, ParseException {
        // Setup:
        // Create an Organizer
        Organizer organizer = createTestOrganizer("test-organizer-cascade-delete");
        String organizerId = organizer.getHardwareID();

        // Create an Entrant (to prove they are unaffected)
        Entrant entrant = createTestEntrant("test-entrant-unaffected");
        String entrantId = entrant.getHardwareID();
        // Create 2 events for the Organizer
        Event event1 = createTestEvent("Organizer's Event 1", organizerId);
        Event event2 = createTestEvent("Organizer's Event 2", organizerId);

        // Create 1 event for a *different* organizer (to prove it's unaffected)
        Event event3 = createTestEvent("Other Organizer's Event", "other-org-id");

        // Execute: Simulate the logic from AdminProfileFragment
        Log.d("AdminTests", "Simulating cascading delete for organizer: " + organizerId);

        // Find all events for the organizer
        QuerySnapshot eventSnapshot = Tasks.await(firestoreDb.collection(TEST_EVENTS_COLLECTION)
                .whereEqualTo("eventOrganizerHardwareID", organizerId)
                .get());

        List<DocumentSnapshot> eventDocuments = eventSnapshot.getDocuments();
        assertEquals("Should find 2 events for the organizer", 2, eventDocuments.size());

        // Create and commit a batch delete for those events
        WriteBatch batch = firestoreDb.batch();
        for (DocumentSnapshot doc : eventDocuments) {
            batch.delete(doc.getReference());
        }
        Tasks.await(batch.commit());
        Log.d("AdminTests", "Batch delete of 2 events committed.");

        // Delete the organizer's user profile
        Tasks.await(testDatabase.removeUserData(organizerId));
        Log.d("AdminTests", "Organizer profile deleted.");

        // Assert:
        // The Organizer is deleted
        User deletedOrganizer = Tasks.await(testDatabase.getUser(organizerId));
        assertNull("The Organizer should be deleted", deletedOrganizer);

        // The Organizer's events are deleted
        Event deletedEvent1 = Tasks.await(testDatabase.getEvent(event1.getUniqueEventID()));
        Event deletedEvent2 = Tasks.await(testDatabase.getEvent(event2.getUniqueEventID()));
        assertNull("Organizer's event 1 should be deleted", deletedEvent1);
        assertNull("Organizer's event 2 should be deleted", deletedEvent2);

        // Unrelated data is NOT deleted
        User unaffectedEntrant = Tasks.await(testDatabase.getUser(entrantId));
        Event unaffectedEvent = Tasks.await(testDatabase.getEvent(event3.getUniqueEventID()));
        assertNotNull("The Entrant should still exist", unaffectedEntrant);
        assertNotNull("The other organizer's event should still exist", unaffectedEvent);

        // Clear deleted items from cleanup list
        usersToClean.remove(organizer);
        eventsToClean.remove(event1);
        eventsToClean.remove(event2);
    }

    /**
     * Tests US 03.03.01: As an administrator, I want to be able to remove images.
     * * Logic:
     * 1. Create an event with a poster.
     * 2. "Remove" the image by setting the posterURL to null (Logic from AdminImagesFragment).
     * 3. Update the database.
     * 4. Assert the event still exists, but the posterURL is now null.
     */
    @Test
    public void testAdminRemoveImage() throws ExecutionException, InterruptedException, ParseException {
        // Setup: Create event with poster
        Event event = createTestEvent("Image Remove Test", "org-img-rem");
        event.setPosterURL("https://example.com/delete_me.jpg");
        Tasks.await(testDatabase.setEventData(event.getUniqueEventID(), event));

        // Execute: Remove image
        event.setPosterURL(null);
        Tasks.await(testDatabase.setEventData(event.getUniqueEventID(), event));

        // Assert
        Event updatedEvent = Tasks.await(testDatabase.getEvent(event.getUniqueEventID()));
        assertNotNull("Event should still exist", updatedEvent);
        assertNull("Poster URL should be null", updatedEvent.getPosterURL());
    }

    // --- HELPER METHODS FOR CREATING TEST DATA ---

    /**
     * Creates a new {@link Entrant} user, saves it to the test Firestore database,
     * and adds it to the cleanup list.
     *
     * @param hardwareId The unique hardware ID to assign to the test entrant.
     * @return The created {@link Entrant} object.
     * @throws ExecutionException   If the Firestore task fails.
     * @throws InterruptedException If the Firestore task is interrupted.
     */
    private Entrant createTestEntrant(String hardwareId) throws ExecutionException, InterruptedException {
        Entrant entrant = new Entrant(hardwareId, "Test", "Entrant", hardwareId + "@test.com");
        Tasks.await(testDatabase.setUserData(hardwareId, entrant));
        usersToClean.add(entrant);
        return entrant;
    }

    /**
     * Creates an {@link Organizer} object, saves it to the test Firestore database,
     * and adds it to a list for cleanup after the test.
     *
     * @param hardwareId The unique hardware ID to assign to the test organizer.
     * @return The created and saved Organizer object.
     * @throws ExecutionException If the Firestore task fails.
     * @throws InterruptedException If the Firestore task is interrupted.
     */
    private Organizer createTestOrganizer(String hardwareId) throws ExecutionException, InterruptedException {
        Organizer organizer = new Organizer(hardwareId, "Test", "Organizer");
        Tasks.await(testDatabase.setUserData(hardwareId, organizer));
        usersToClean.add(organizer);
        return organizer;
    }

    /**
     * Helper method to create a test Administrator, add it to the test database,
     * and schedule it for cleanup after the test.
     *
     * @param hardwareId The unique hardware ID to assign to the test admin.
     * @return The created Administrator object.
     * @throws ExecutionException   If the database task fails.
     * @throws InterruptedException If the database task is interrupted.
     */
    private Administrator createTestAdmin(String hardwareId) throws ExecutionException, InterruptedException {
        Administrator admin = new Administrator(hardwareId, "Test", "Admin");
        Tasks.await(testDatabase.setUserData(hardwareId, admin));
        usersToClean.add(admin);
        return admin;
    }

    /**
     * Helper method to create a new {@link Event}, assign it a unique ID,
     * save it to the test Firestore collection, and add it to the cleanup list.
     *
     * @param eventName   The name for the test event.
     * @param organizerId The hardware ID of the organizer creating the event.
     * @return The fully constructed and saved {@link Event} object.
     * @throws ExecutionException   If the database task fails.
     * @throws InterruptedException If the database task is interrupted.
     */
    private Event createTestEvent(String eventName, String organizerId) throws ExecutionException, InterruptedException, ParseException {
        Long eventId = Tasks.await(testDatabase.getUniqueEventID());
        Date start = Utils.createWholeDayDate("2025-10-27");
        Date registrationStart = Utils.createWholeDayDate("2025-10-01");
        Date registrationEnd = Utils.createWholeDayDate("2025-10-25");
        Event event = new Event(
                eventId, eventName, "Test Description", start,
                "Location", registrationStart, registrationEnd,
                organizerId,
                false
        );
        Tasks.await(testDatabase.setEventData(eventId, event));
        eventsToClean.add(event);
        return event;
    }

    /**
     * Helper method to create a new {@link Notification}, save it to the test database,
     * and add it to the cleanup list.
     *
     * @param senderId   The hardware ID of the user sending the notification.
     * @param receiverId The hardware ID of the user receiving the notification.
     * @param header     The header/title of the notification.
     * @return The created {@link Notification} object.
     * @throws ExecutionException   If the database operation fails.
     * @throws InterruptedException If the database operation is interrupted.
     */
    private Notification createTestNotification(String senderId, String receiverId, String header) throws ExecutionException, InterruptedException {
        Long notifId = Tasks.await(testDatabase.getUniqueNotificationID());
        Notification notification = new Notification(
                notifId, senderId, receiverId, header, "Test Body"
        );
        Tasks.await(testDatabase.setNotificationData(notifId, notification));
        notificationsToClean.add(notification);
        return notification;
    }
}