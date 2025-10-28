package com.example.zypherevent;

import android.util.Log;

import com.example.zypherevent.userTypes.Entrant;
import com.example.zypherevent.userTypes.Organizer;
import com.example.zypherevent.userTypes.User;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.*;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Integration test for the Database class.
 * <p>
 * This test class connects to the live Firebase Firestore to tests
 * database function implementation
 * <p>
 * It uses a separate set of test collections to avoid interfering with
 * production data and cleans up all test data after execution.
 * <p>
 * Each test is isolated via @Before and @After methods.
 */
@RunWith(AndroidJUnit4.class)
public class DatabaseTests {

    // Use separate test collections to avoid changing production data
    private static final String TEST_USERS_COLLECTION = "test_users";
    private static final String TEST_EVENTS_COLLECTION = "test_events";
    private static final String TEST_EXTRAS_COLLECTION = "test_extras";

    // Start unique IDs at a high, non-conflicting number
    private static final Long TEST_ID_START_VALUE = 900000L;

    private static Database testDatabase;

    // These objects are re-created before each test in setUp()
    private Entrant testEntrant;
    private Organizer testOrganizer;
    private Event testEvent;

    /**
     * Runs ONCE before all tests.
     * Initializes the database connection and sets the unique event ID
     * counter to a known, predictable value for testing.
     */
    @BeforeClass
    public static void setUpClass() throws ExecutionException, InterruptedException {
        testDatabase = new Database(TEST_USERS_COLLECTION, TEST_EVENTS_COLLECTION, TEST_EXTRAS_COLLECTION);

        // Set the unique ID counter to a known value for testing
        DocumentReference uniqueRef = FirebaseFirestore.getInstance()
                .collection(TEST_EXTRAS_COLLECTION)
                .document("uniqueIdentifierData");

        Map<String, Object> data = new HashMap<>();
        data.put("curEvent", TEST_ID_START_VALUE);
        Tasks.await(uniqueRef.set(data));
    }

    /**
     * Runs ONCE after all tests are complete.
     * Cleans up the test data.
     */
    @AfterClass
    public static void cleanUpClass() throws ExecutionException, InterruptedException {
        DocumentReference uniqueRef = FirebaseFirestore.getInstance()
                .collection(TEST_EXTRAS_COLLECTION)
                .document("uniqueIdentifierData");
        Tasks.await(uniqueRef.delete());
        Log.d("DatabaseTest", "Test extras collection cleaned up ONCE.");
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
                "555-1234");

        testOrganizer = new Organizer(
                "test-organizer-id",
                "Test",
                "Organizer");

        testEvent = null;
    }

    /**
     * Runs AFTER EACH @Test method.
     * Reliably cleans up any data created during the test,
     * ensuring tests are isolated and can be run repeatedly.
     */
    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        // Clean up any test users
        if (testEntrant != null) {
            Tasks.await(testDatabase.removeUserData(testEntrant.getHardwareID()));
        }
        if (testOrganizer != null) {
            Tasks.await(testDatabase.removeUserData(testOrganizer.getHardwareID()));
        }

        // Clean up any test events
        if (testEvent != null) {
            Tasks.await(testDatabase.removeEventData(testEvent.getUniqueEventID()));
        }
        Log.d("DatabaseTest", "--- Test complete, data cleaned up ---");
    }

    //  USER TESTS

    /**
     * Tests ADD (Create) and READ (Get) for a User.
     * Verifies that the user retrieved from the database is
     * identical to the user that was saved, using .equals().
     */
    @Test
    public void testAddAndGetUser() throws ExecutionException, InterruptedException {
        // 1. ADD (Create)
        Tasks.await(testDatabase.setUserData(testEntrant.getHardwareID(), testEntrant));

        // 2. READ (Get)
        User fetchedUser = Tasks.await(testDatabase.getUser(testEntrant.getHardwareID()));

        // 3. Assert
        assertNotNull("Fetched user should not be null", fetchedUser);
        assertTrue("Fetched user should be an instance of Entrant", fetchedUser instanceof Entrant);
        assertEquals("Fetched user should be identical to the original", testEntrant, (Entrant) fetchedUser);
    }

    /**
     * Tests UPDATE for a User.
     * Verifies that changes to a user object are persisted
     * when the object is saved again.
     */
    @Test
    public void testUpdateUser() throws ExecutionException, InterruptedException {
        // Create the original user
        Tasks.await(testDatabase.setUserData(testEntrant.getHardwareID(), testEntrant));

        // Update user
        testEntrant.setFirstName("UpdatedName");
        testEntrant.setEmail("updated@email.com");
        Tasks.await(testDatabase.setUserData(testEntrant.getHardwareID(), testEntrant));

        // Read the usr
        User fetchedUser = Tasks.await(testDatabase.getUser(testEntrant.getHardwareID()));

        // Assert the update happened correctly
        assertNotNull("Fetched user should not be null", fetchedUser);
        assertEquals("First name should be updated", "UpdatedName", fetchedUser.getFirstName());
        assertEquals("Email should be updated", "updated@email.com", ((Entrant) fetchedUser).getEmail());
        assertEquals("Fetched user should match updated local object", testEntrant, (Entrant) fetchedUser);
    }

    /**
     * Tests Delete for a User.
     * Verifies that a user is null after being deleted.
     */
    @Test
    public void testRemoveUser() throws ExecutionException, InterruptedException {
        // Create
        Tasks.await(testDatabase.setUserData(testEntrant.getHardwareID(), testEntrant));

        // Delete
        Tasks.await(testDatabase.removeUserData(testEntrant.getHardwareID()));

        // Get user
        User fetchedUser = Tasks.await(testDatabase.getUser(testEntrant.getHardwareID()));

        // Assert
        assertNull("User should be null after being removed", fetchedUser);
    }

    //  EVENT TESTS

    @Test
    public void testAddAndGetEvent() throws ExecutionException, InterruptedException {
        // 1. Arrange - Get a unique ID and create the event
        Long newEventID = Tasks.await(testDatabase.getUniqueEventID());
        testEvent = new Event(
                newEventID,
                "Test Event", "Event for testing", "2025-10-27T10:00:00",
                "Test Location", "2025-10-01T10:00:00", "2025-10-25T10:00:00",
                testOrganizer.getHardwareID()
        );

        // create
        Tasks.await(testDatabase.setEventData(testEvent.getUniqueEventID(), testEvent));

        // get
        Event fetchedEvent = Tasks.await(testDatabase.getEvent(testEvent.getUniqueEventID()));

        // Test fields individually instead of object-to-object
        assertNotNull("Fetched event should not be null", fetchedEvent);
        assertEquals("Event IDs should match", testEvent.getUniqueEventID(), fetchedEvent.getUniqueEventID());
        assertEquals("Event names should match", testEvent.getEventName(), fetchedEvent.getEventName());
        assertEquals("Event locations should match", testEvent.getLocation(), fetchedEvent.getLocation());
        assertEquals("Organizer IDs should match", testEvent.getEventOrganizerHardwareID(), fetchedEvent.getEventOrganizerHardwareID());

        // Check that lists are initialized correctly (not null)
        assertNotNull("Waitlist should not be null", fetchedEvent.getWaitListEntrants());
        assertNotNull("Accepted list should not be null", fetchedEvent.getAcceptedEntrants());
        assertNotNull("Declined list should not be null", fetchedEvent.getDeclinedEntrants());
    }

    @Test
    public void testUpdateEvent() throws ExecutionException, InterruptedException {
        // create
        Long newEventID = Tasks.await(testDatabase.getUniqueEventID());
        testEvent = new Event(
                newEventID,
                "Original Event Name", "Original Description", "...",
                "Original Location", "...", "...",
                testOrganizer.getHardwareID()
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
    }

    /**
     * Tests REMOVE (Delete) for an Event.
     * Verifies that an event is null after being deleted.
     */
    @Test
    public void testRemoveEvent() throws ExecutionException, InterruptedException {
        Long newEventID = Tasks.await(testDatabase.getUniqueEventID());
        testEvent = new Event(
                newEventID, "Event To Delete", "...", "...", "...", "...", "...",
                testOrganizer.getHardwareID()
        );
        Tasks.await(testDatabase.setEventData(testEvent.getUniqueEventID(), testEvent));

        Tasks.await(testDatabase.removeEventData(testEvent.getUniqueEventID()));

        Event fetchedEvent = Tasks.await(testDatabase.getEvent(testEvent.getUniqueEventID()));

        assertNull("Event should be null after being removed", fetchedEvent);
    }

    //  EXTRA DATABASE TESTS

    /**
     * Tests that the unique ID counter is sequential.
     * This test is "stateless" and only checks that subsequent calls
     * return incrementing numbers.
     */
    @Test
    public void testGetUniqueEventID() throws ExecutionException, InterruptedException {
        // Get two IDs back-to-back
        Long id1 = Tasks.await(testDatabase.getUniqueEventID());
        Long id2 = Tasks.await(testDatabase.getUniqueEventID());

        // We only care that the second ID is one greater than the first.
        // We do not care what the starting value is.
        assertNotNull("First ID should not be null", id1);
        assertNotNull("Second ID should not be null", id2);
        assertEquals("IDs should be sequential", (long) id1 + 1, (long) id2);
    }

    /**
     * Tests that fetching non-existent documents reliably returns null.
     */
    @Test
    public void testGetNonExistentDocuments() throws ExecutionException, InterruptedException {
        User user = Tasks.await(testDatabase.getUser("id-that-does-not-exist"));
        Event evt = Tasks.await(testDatabase.getEvent(-99L));

        // should be null
        assertNull("Non-existent user should be null", user);
        assertNull("Non-existent event should be null", evt);
    }
}
