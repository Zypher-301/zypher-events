package com.example.zypherevent;

import com.example.zypherevent.userTypes.Administrator;
import com.example.zypherevent.userTypes.Entrant;
import com.example.zypherevent.userTypes.Organizer;
import com.example.zypherevent.userTypes.User;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.*;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import android.text.format.DateUtils;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Tests for the Database class and operations.
 * This test class connects to Firebase Firestore to test
 * database function implementation.
 * It uses a separate set of test collections to avoid interfering with
 * production data and cleans up all test data after execution.
 * Each test is isolated using @Before and @After methods.
 */
@RunWith(AndroidJUnit4.class)
public class DatabaseTests {

    // Use separate test collections to avoid changing production data
    private static final String TEST_USERS_COLLECTION = "test_users";
    private static final String TEST_EVENTS_COLLECTION = "test_events";
    private static final String TEST_NOTIFICATIONS_COLLECTION = "test_notifications";
    private static final String TEST_EXTRAS_COLLECTION = "test_extras";

    // Start unique IDs at a high, non-conflicting number
    private static final Long TEST_ID_START_VALUE = 900000L;

    private static Database testDatabase;

    // cleared before each test in setUp()
    // helps to avoid try/finally blocks in every test
    private Entrant testEntrant;
    private Organizer testOrganizer;
    private Administrator testAdmin;
    private Event testEvent;

    private Notification testNotification;

    /**
     * Runs once before all tests.
     * Initializes the database connection and sets the unique event ID & unique notification ID
     * counter to a known, predictable value for testing.
     */
    @BeforeClass
    public static void setUpClass() throws ExecutionException, InterruptedException {
        testDatabase = new Database(TEST_USERS_COLLECTION, TEST_EVENTS_COLLECTION, TEST_NOTIFICATIONS_COLLECTION, TEST_EXTRAS_COLLECTION);

        // Set the unique ID counter to a known value for testing
        DocumentReference uniqueRef = FirebaseFirestore.getInstance()
                .collection(TEST_EXTRAS_COLLECTION)
                .document("uniqueIdentifierData");

        Map<String, Object> data = new HashMap<>();
        data.put("curEvent", TEST_ID_START_VALUE);
        data.put("curNotification", TEST_ID_START_VALUE);
        Tasks.await(uniqueRef.set(data));
    }

    /**
     * Runs once after all tests are complete.
     * Cleans up the test data.
     */
    @AfterClass
    public static void cleanUpClass() throws ExecutionException, InterruptedException {
        DocumentReference uniqueRef = FirebaseFirestore.getInstance()
                .collection(TEST_EXTRAS_COLLECTION)
                .document("uniqueIdentifierData");
        Tasks.await(uniqueRef.delete());
    }

    /**
     * Runs before each @Test method.
     * Creates fresh, clean objects for each test to use.
     */
    @Before
    public void setUp() {
        // Create fresh test objects for each test
        testEntrant = new Entrant(
                "test-entrant-id",
                "Test",
                "Entrant",
                "test@entrant.com",
                "555-1234",
                false);

        // This object has a null email and phone number (implicitly)
        testOrganizer = new Organizer(
                "test-organizer-id",
                "Test",
                "Organizer");

        testAdmin = new Administrator(
                "test-admin-id",
                "Test",
                "Admin");

        testEvent = null; // events are created in their own tests
        testNotification = null; // notifications are created in their own tests
    }

    /**
     * Runs after each @Test method.
     * Reliably cleans up any data created during the test.
     */
    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        // Clean up any test data
        if (testEntrant != null) {
            Tasks.await(testDatabase.removeUserData(testEntrant.getHardwareID()));
        }
        if (testOrganizer != null) {
            Tasks.await(testDatabase.removeUserData(testOrganizer.getHardwareID()));
        }
        if (testAdmin != null) {
            Tasks.await(testDatabase.removeUserData(testAdmin.getHardwareID()));
        }
        if (testEvent != null) {
            Tasks.await(testDatabase.removeEventData(testEvent.getUniqueEventID()));
        }
        if (testNotification != null) {
            Tasks.await(testDatabase.removeNotificationData(testNotification.getUniqueNotificationID()));
        }
    }

    //  USER TESTS

    /**
     * Tests setUserData and getUser for an Entrant
     */
    @Test
    public void testAddAndGetEntrant() throws ExecutionException, InterruptedException {
        Tasks.await(testDatabase.setUserData(testEntrant.getHardwareID(), testEntrant));
        User fetchedUser = Tasks.await(testDatabase.getUser(testEntrant.getHardwareID()));

        assertNotNull("Fetched user should not be null", fetchedUser);
        assertTrue("Fetched user should be an instance of Entrant", fetchedUser instanceof Entrant);
        assertEquals("Fetched user should be identical to the original", testEntrant, fetchedUser);
    }

    /**
     * Tests setUserData and getUser for an Organizer with null fields
     */
    @Test
    public void testAddAndGetOrganizer() throws ExecutionException, InterruptedException {
        Tasks.await(testDatabase.setUserData(testOrganizer.getHardwareID(), testOrganizer));
        User fetchedUser = Tasks.await(testDatabase.getUser(testOrganizer.getHardwareID()));

        assertNotNull("Fetched user should not be null", fetchedUser);
        assertTrue("Fetched user should be an instance of Organizer", fetchedUser instanceof Organizer);
        assertEquals("Fetched user should be identical to the original", testOrganizer, fetchedUser);
    }

    /**
     * Tests setUserData and getUser for an Administrator.
     */
    @Test
    public void testAddAndGetAdmin() throws ExecutionException, InterruptedException {
        Tasks.await(testDatabase.setUserData(testAdmin.getHardwareID(), testAdmin));
        User fetchedUser = Tasks.await(testDatabase.getUser(testAdmin.getHardwareID()));

        assertNotNull("Fetched user should not be null", fetchedUser);
        assertTrue("Fetched user should be an instance of Administrator", fetchedUser instanceof Administrator);
        assertEquals("Fetched user should be identical to the original", testAdmin, fetchedUser);
    }

    /**
     * Tests updating a user and using setUserData..
     * Verifies that changes to a user object are persisted.
     */
    @Test
    public void testUpdateUser() throws ExecutionException, InterruptedException {
        Tasks.await(testDatabase.setUserData(testEntrant.getHardwareID(), testEntrant));

        // Update user
        testEntrant.setFirstName("UpdatedName");
        testEntrant.setEmail("updated@email.com");
        Tasks.await(testDatabase.setUserData(testEntrant.getHardwareID(), testEntrant));

        User fetchedUser = Tasks.await(testDatabase.getUser(testEntrant.getHardwareID()));

        // Assert the update happened correctly
        assertNotNull("Fetched user should not be null", fetchedUser);
        assertEquals("First name should be updated", "UpdatedName", fetchedUser.getFirstName());
        assertEquals("Email should be updated", "updated@email.com", ((Entrant) fetchedUser).getEmail());
        assertEquals("Fetched user should match updated local object", testEntrant, fetchedUser);
    }

    /**
     * Tests removeUserData for a User.
     * Verifies that a user is null after being deleted.
     */
    @Test
    public void testRemoveUser() throws ExecutionException, InterruptedException {
        Tasks.await(testDatabase.setUserData(testEntrant.getHardwareID(), testEntrant));
        Tasks.await(testDatabase.removeUserData(testEntrant.getHardwareID()));
        User fetchedUser = Tasks.await(testDatabase.getUser(testEntrant.getHardwareID()));
        assertNull("User should be null after being removed", fetchedUser);
    }

    //  EVENT TESTS

    /**
     * Tests setEventData and getEvent. Also verifies that the implementation
     * of equals() for Events are correct.
     */
    @Test
    public void testAddAndGetEvent() throws ExecutionException, InterruptedException, ParseException {
        Long newEventID = Tasks.await(testDatabase.getUniqueEventID());
        // Create an event with all fields non-null
        testEvent = new Event(
                newEventID,
                "Test Event",
                "Event for testing",
                Utils.createWholeDayDate("2025-10-27"),
                "Test Location",
                Utils.createWholeDayDate("2025-10-01"),
                Utils.createWholeDayDate("2025-10-25"),
                testOrganizer.getHardwareID(),
                "http://example.com/poster.png"
        );

        Tasks.await(testDatabase.setEventData(testEvent.getUniqueEventID(), testEvent));
        Event fetchedEvent = Tasks.await(testDatabase.getEvent(testEvent.getUniqueEventID()));

        // Test fields individually
        assertNotNull("Fetched event should not be null", fetchedEvent);
        assertEquals("Event IDs should match", testEvent.getUniqueEventID(), fetchedEvent.getUniqueEventID());
        assertEquals("Event names should match", testEvent.getEventName(), fetchedEvent.getEventName());
        assertEquals("Event locations should match", testEvent.getLocation(), fetchedEvent.getLocation());
        assertEquals("Organizer IDs should match", testEvent.getEventOrganizerHardwareID(), fetchedEvent.getEventOrganizerHardwareID());
        assertEquals("Poster URLs should match", testEvent.getPosterURL(), fetchedEvent.getPosterURL());

        // Test object's equal method
        assertEquals("Event objects should be equal", testEvent, fetchedEvent);

        // Check that lists were correctly initialized (not null for null safety)
        assertNotNull("Waitlist should not be null", fetchedEvent.getWaitListEntrants());
        assertNotNull("Accepted list should not be null", fetchedEvent.getAcceptedEntrants());
        assertNotNull("Declined list should not be null", fetchedEvent.getDeclinedEntrants());
    }

    /**
     * Tests setEventData for an Event. Also tests the equals method for Events.
     */
    @Test
    public void testUpdateEvent() throws ExecutionException, InterruptedException, ParseException {
        // create
        Long newEventID = Tasks.await(testDatabase.getUniqueEventID());
        Date start = Utils.createWholeDayDate("2025-10-27");
        Date registrationStart = Utils.createWholeDayDate("2025-10-01");
        Date registrationEnd = Utils.createWholeDayDate("2025-10-25");
        testEvent = new Event(
                newEventID,
                "Original Event Name", "Original Description", start,
                "Original Location", registrationStart, registrationEnd,
                testOrganizer.getHardwareID(),
                "http://example.com/poster.png"
        );
        Tasks.await(testDatabase.setEventData(testEvent.getUniqueEventID(), testEvent));

        // update
        testEvent.setEventName("Updated Event Name");
        testEvent.setLocation("New Location");
        Tasks.await(testDatabase.setEventData(testEvent.getUniqueEventID(), testEvent));

        // get
        Event fetchedEvent = Tasks.await(testDatabase.getEvent(testEvent.getUniqueEventID()));

        // Test fields individually
        assertNotNull("Fetched event should not be null", fetchedEvent);
        assertEquals("Event name should be updated", "Updated Event Name", fetchedEvent.getEventName());
        assertEquals("Location should be updated", "New Location", fetchedEvent.getLocation());
        assertEquals("Fetched event's ID should match", testEvent.getUniqueEventID(), fetchedEvent.getUniqueEventID());
        assertEquals("Events objects should be equal", testEvent, fetchedEvent);
    }

    /**
     * Tests removeEventData for an Event.
     */
    @Test
    public void testRemoveEvent() throws ExecutionException, InterruptedException, ParseException {
        Long newEventID = Tasks.await(testDatabase.getUniqueEventID());
        Date start = Utils.createWholeDayDate("2025-10-27");
        Date registrationStart = Utils.createWholeDayDate("2025-10-01");
        Date registrationEnd = Utils.createWholeDayDate("2025-10-25");
        testEvent = new Event(
                newEventID, "Event To Delete", "...", start, "...", registrationStart, registrationEnd,
                testOrganizer.getHardwareID()
        );
        Tasks.await(testDatabase.setEventData(testEvent.getUniqueEventID(), testEvent));

        Tasks.await(testDatabase.removeEventData(testEvent.getUniqueEventID()));

        Event fetchedEvent = Tasks.await(testDatabase.getEvent(newEventID));
        assertNull("Event should be null after being removed", fetchedEvent);

        testEvent = null; // set to null so @After doesn't try to delete it again
    }

    // NOTIFICATION TESTS

    /**
     * Tests setNotificationData and getNotification. Also verifies that the implementation
     * of equals() for Events are correct.
     */
    @Test
    public void testAddAndGetNotification() throws ExecutionException, InterruptedException {
        Long newNotifID = Tasks.await(testDatabase.getUniqueNotificationID());

        // Create a notification with all fields non-null
        testNotification = new Notification(
                newNotifID,
                testOrganizer.getHardwareID(),
                testEntrant.getHardwareID(),
                "Test Notification",
                "Notification for testing");


        Tasks.await(testDatabase.setNotificationData(testNotification.getUniqueNotificationID(), testNotification));
        Notification fetchedNotification = Tasks.await(testDatabase.getNotification(testNotification.getUniqueNotificationID()));

        // Test fields individually
        assertNotNull("Fetched notification should not be null", fetchedNotification);
        assertEquals("Notification IDs should match", testNotification.getUniqueNotificationID(), fetchedNotification.getUniqueNotificationID());
        assertEquals("Notification senders should match", testNotification.getSendingUserHardwareID(), fetchedNotification.getSendingUserHardwareID());
        assertEquals("Notification receivers should match", testNotification.getReceivingUserHardwareID(), fetchedNotification.getReceivingUserHardwareID());
        assertEquals("Notification headers should match", testNotification.getNotificationHeader(), fetchedNotification.getNotificationHeader());
        assertEquals("Notification bodies should match", testNotification.getNotificationBody(), fetchedNotification.getNotificationBody());

        // Test object's equal method
        assertEquals("Event objects should be equal", testNotification, fetchedNotification);
    }

    /**
     * Tests setNotificationData for a Notification. Also tests the equals method for Notifications.
     */
    @Test
    public void testUpdateNotification() throws ExecutionException, InterruptedException {
        // create
        Long newNotifID = Tasks.await(testDatabase.getUniqueNotificationID());
        testNotification = new Notification(
                newNotifID,
                testOrganizer.getHardwareID(),
                testEntrant.getHardwareID(),
                "Original Notification Header",
                "Original Notification Body"
        );
        Tasks.await(testDatabase.setNotificationData(testNotification.getUniqueNotificationID(), testNotification));

        // update
        testNotification.setNotificationHeader("Updated Notification Header");
        testNotification.setNotificationBody("Updated Notification Body");
        Tasks.await(testDatabase.setNotificationData(testNotification.getUniqueNotificationID(), testNotification));

        // get
        Notification fetchedNotification = Tasks.await(testDatabase.getNotification(testNotification.getUniqueNotificationID()));

        // Test fields individually
        assertNotNull("Fetched notification should not be null", fetchedNotification);
        assertEquals("Notification header should be updated", "Updated Notification Header", fetchedNotification.getNotificationHeader());
        assertEquals("Notification body should be updated", "Updated Notification Body", fetchedNotification.getNotificationBody());
        assertEquals("Fetched notification's ID should match", testNotification.getUniqueNotificationID(), fetchedNotification.getUniqueNotificationID());
        assertEquals("Notification objects should be equal", testNotification, fetchedNotification);

    }

    /**
     * Tests removeNotificationData for a Notification.
     */
    @Test
    public void testRemoveNotification() throws ExecutionException, InterruptedException {
        Long newNotifID = Tasks.await(testDatabase.getUniqueNotificationID());
        testNotification = new Notification(
                newNotifID,
                testOrganizer.getHardwareID(),
                testEntrant.getHardwareID(),
                "Notification To Delete",
                "This is a test notification to delete"
        );
        Tasks.await(testDatabase.setNotificationData(testNotification.getUniqueNotificationID(), testNotification));

        Tasks.await(testDatabase.removeNotificationData(testNotification.getUniqueNotificationID()));

        Notification fetchedNotification = Tasks.await(testDatabase.getNotification(newNotifID));
        assertNull("Notification should be null after being removed", fetchedNotification);

        testNotification = null; // set to null so @After doesn't try to delete it again
    }

    //  EXTRA DATABASE TESTS

    /**
     * Tests that the getUniqueEventID counter is sequential.
     */
    @Test
    public void testGetUniqueEventID() throws ExecutionException, InterruptedException {
        Long id1 = Tasks.await(testDatabase.getUniqueEventID());
        Long id2 = Tasks.await(testDatabase.getUniqueEventID());

        assertNotNull("First event ID should not be null", id1);
        assertNotNull("Second event ID should not be null", id2);
        assertEquals("Event IDs should be sequential", id1 + 1, (long) id2);
    }

    /**
     * Tests that the getUniqueNotificationID counter is sequential.
     */
    @Test
    public void testGetUniqueNotificationID() throws ExecutionException, InterruptedException {
        Long id1 = Tasks.await(testDatabase.getUniqueNotificationID());
        Long id2 = Tasks.await(testDatabase.getUniqueNotificationID());

        assertNotNull("First notif ID should not be null", id1);
        assertNotNull("Second notif ID should not be null", id2);
        assertEquals("Notif IDs should be sequential", id1 + 1, (long) id2);
    }

    /**
     * Tests that fetching non-existent documents reliably returns null.
     */
    @Test
    public void testGetNonExistentDocuments() throws ExecutionException, InterruptedException {
        User user = Tasks.await(testDatabase.getUser("id-that-does-not-exist"));
        Event evt = Tasks.await(testDatabase.getEvent(-99L));

        assertNull("Non-existent user should be null", user);
        assertNull("Non-existent event should be null", evt);
    }

    // WAITLIST LIMIT TESTS

    /**
     * Tests that waitlist limit is enforced in database transactions.
     */
    @Test
    public void testWaitlistLimitEnforcementInDatabase() throws ExecutionException, InterruptedException, ParseException {
        Long newEventID = Tasks.await(testDatabase.getUniqueEventID());
        testEvent = new Event(
                newEventID,
                "Limited Event",
                "Test event with waitlist limit",
                Utils.createWholeDayDate("2025-12-01"),
                "Test Location",
                Utils.createWholeDayDate("2025-01-01"),
                Utils.createWholeDayDate("2025-12-31"),
                testOrganizer.getHardwareID()
        );
        testEvent.setWaitlistLimit(1);
        Tasks.await(testDatabase.setEventData(newEventID, testEvent));

        // Add first entrant - should succeed
        Tasks.await(testDatabase.addEntrantToWaitlist(String.valueOf(newEventID), testEntrant));

        // Try to add second entrant - should fail
        Entrant secondEntrant = new Entrant("test-entrant-2", "Jane", "Doe", "jane@test.com", "555-0002", false);
        try {
            Tasks.await(testDatabase.addEntrantToWaitlist(String.valueOf(newEventID), secondEntrant));
            fail("Should throw RuntimeException when waitlist is full");
        } catch (Exception e) {
            assertTrue("Exception should mention waitlist is full",
                    e.getMessage() != null && e.getMessage().contains("Waitlist is full"));
        }
    }

    /**
     * Tests that events with null waitlist limit allow unlimited entrants.
     */
    @Test
    public void testUnlimitedWaitlistInDatabase() throws ExecutionException, InterruptedException, ParseException {
        Long newEventID = Tasks.await(testDatabase.getUniqueEventID());
        testEvent = new Event(
                newEventID,
                "Unlimited Event",
                "Test event without waitlist limit",
                Utils.createWholeDayDate("2025-12-01"),
                "Test Location",
                Utils.createWholeDayDate("2025-01-01"),
                Utils.createWholeDayDate("2025-12-31"),
                testOrganizer.getHardwareID()
        );
        // No limit set (null)
        Tasks.await(testDatabase.setEventData(newEventID, testEvent));

        // Add multiple entrants - all should succeed
        Tasks.await(testDatabase.addEntrantToWaitlist(String.valueOf(newEventID), testEntrant));
        Entrant secondEntrant = new Entrant("test-entrant-2", "Jane", "Doe", "jane@test.com", "555-0002", false);
        Tasks.await(testDatabase.addEntrantToWaitlist(String.valueOf(newEventID), secondEntrant));

        Event fetchedEvent = Tasks.await(testDatabase.getEvent(newEventID));
        assertEquals("Should have 2 entrants", 2, fetchedEvent.getWaitListEntrants().size());
    }

    // REGISTRATION PERIOD TESTS

    /**
     * Tests that registration period is enforced before start time.
     */
    @Test
    public void testRegistrationNotStartedInDatabase() throws ExecutionException, InterruptedException, ParseException {
        Long newEventID = Tasks.await(testDatabase.getUniqueEventID());
        testEvent = new Event(
                newEventID,
                "Future Event",
                "Registration hasn't started",
                Utils.createWholeDayDate("2030-12-01"),
                "Test Location",
                Utils.createWholeDayDate("2030-11-01"),  // Future start
                Utils.createWholeDayDate("2030-11-30"),
                testOrganizer.getHardwareID()
        );
        Tasks.await(testDatabase.setEventData(newEventID, testEvent));

        try {
            Tasks.await(testDatabase.addEntrantToWaitlist(String.valueOf(newEventID), testEntrant));
            fail("Should throw RuntimeException when registration hasn't started");
        } catch (Exception e) {
            assertTrue("Exception should mention registration window",
                    e.getMessage() != null && e.getMessage().contains("Registration window has not yet started"));
        }
    }

    /**
     * Tests that registration period is enforced after end time.
     */
    @Test
    public void testRegistrationEndedInDatabase() throws ExecutionException, InterruptedException, ParseException {
        Long newEventID = Tasks.await(testDatabase.getUniqueEventID());
        testEvent = new Event(
                newEventID,
                "Past Event",
                "Registration has ended",
                Utils.createWholeDayDate("2020-12-01"),
                "Test Location",
                Utils.createWholeDayDate("2020-11-01"),
                Utils.createWholeDayDate("2020-11-30"),  // Past end
                testOrganizer.getHardwareID()
        );
        Tasks.await(testDatabase.setEventData(newEventID, testEvent));

        try {
            Tasks.await(testDatabase.addEntrantToWaitlist(String.valueOf(newEventID), testEntrant));
            fail("Should throw RuntimeException when registration has ended");
        } catch (Exception e) {
            assertTrue("Exception should mention registration window",
                    e.getMessage() != null && e.getMessage().contains("Registration window has ended"));
        }
    }
}
