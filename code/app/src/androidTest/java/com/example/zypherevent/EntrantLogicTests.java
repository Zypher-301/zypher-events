package com.example.zypherevent;

import com.example.zypherevent.userTypes.Entrant;
import com.example.zypherevent.userTypes.User;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;

import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Instrumented tests for ALL Entrant User Stories
 * Covers:
 * - 01.01.xx: Joining/Leaving waitlists, Browsing, Filtering
 * - 01.02.xx: Profile Creation, Updating, History, Deletion
 */
@RunWith(AndroidJUnit4.class)
public class EntrantLogicTests {

    // Isolated test collections
    private static final String TEST_USERS_COLLECTION = "test_users";
    private static final String TEST_EVENTS_COLLECTION = "test_events";
    private static final String TEST_NOTIFICATIONS_COLLECTION = "test_notifications";
    private static final String TEST_EXTRAS_COLLECTION = "test_extras";

    private static final Long TEST_ID_START = 800000L;
    private static Database testDatabase;
    private static FirebaseFirestore firestoreDb;

    // Cleanup lists
    private List<User> usersToClean = new ArrayList<>();
    private List<Event> eventsToClean = new ArrayList<>();

    @BeforeClass
    public static void setUpClass() throws ExecutionException, InterruptedException {
        testDatabase = new Database(TEST_USERS_COLLECTION, TEST_EVENTS_COLLECTION, TEST_NOTIFICATIONS_COLLECTION, TEST_EXTRAS_COLLECTION);
        firestoreDb = FirebaseFirestore.getInstance();
        resetUniqueCounters();
    }

    @AfterClass
    public static void cleanUpClass() throws ExecutionException, InterruptedException {
        DocumentReference uniqueRef = firestoreDb.collection(TEST_EXTRAS_COLLECTION).document("uniqueIdentifierData");
        Tasks.await(uniqueRef.delete());
        clearCollection(TEST_USERS_COLLECTION);
        clearCollection(TEST_EVENTS_COLLECTION);
    }

    @Before
    public void setUp() throws ExecutionException, InterruptedException {
        usersToClean.clear();
        eventsToClean.clear();
        clearCollection(TEST_USERS_COLLECTION);
        clearCollection(TEST_EVENTS_COLLECTION);
        resetUniqueCounters();
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        for (User u : usersToClean) Tasks.await(testDatabase.removeUserData(u.getHardwareID()));
        for (Event e : eventsToClean) Tasks.await(testDatabase.removeEventData(e.getUniqueEventID()));
    }

    // --- HELPER METHODS ---

    private static void resetUniqueCounters() throws ExecutionException, InterruptedException {
        Map<String, Object> data = new HashMap<>();
        data.put("curEvent", TEST_ID_START);
        data.put("curNotification", TEST_ID_START);
        Tasks.await(firestoreDb.collection(TEST_EXTRAS_COLLECTION).document("uniqueIdentifierData").set(data));
    }

    private static void clearCollection(String collection) throws ExecutionException, InterruptedException {
        QuerySnapshot qs = Tasks.await(firestoreDb.collection(collection).get());
        if (!qs.isEmpty()) {
            WriteBatch batch = firestoreDb.batch();
            for (DocumentSnapshot doc : qs.getDocuments()) batch.delete(doc.getReference());
            Tasks.await(batch.commit());
        }
    }

    private Entrant createEntrant(String id) throws ExecutionException, InterruptedException {
        Entrant e = new Entrant(id, "Test", "Entrant", "test@email.com");
        Tasks.await(testDatabase.setUserData(id, e));
        usersToClean.add(e);
        return e;
    }

    private Event createEvent(String name, String orgId) throws ExecutionException, InterruptedException, ParseException {
        Long id = Tasks.await(testDatabase.getUniqueEventID());
        Event e = new Event(id, name, "Desc", Utils.createWholeDayDate("2025-12-01"), "Location",
                Utils.createWholeDayDate("2024-01-01"), Utils.createWholeDayDate("2025-11-30"),
                orgId, false);
        Tasks.await(testDatabase.setEventData(id, e));
        eventsToClean.add(e);
        return e;
    }

    // ==========================================
    // USER STORIES (US 01.01.xx)
    // ==========================================

    /**
     * US 01.01.01: Join Waitlist
     */
    @Test
    public void testJoinWaitlist() throws ExecutionException, InterruptedException, ParseException {
        Entrant entrant = createEntrant("entrant-join");
        Event event = createEvent("Join Event", "org-1");

        // 1. Join Waitlist
        Tasks.await(testDatabase.addEntrantToWaitlist(String.valueOf(event.getUniqueEventID()), entrant));

        // 2. Update User History
        entrant.addEventToRegisteredEventHistory(event.getUniqueEventID());
        Tasks.await(testDatabase.setUserData(entrant.getHardwareID(), entrant));

        // Assert
        Event updatedEvent = Tasks.await(testDatabase.getEvent(event.getUniqueEventID()));
        boolean onWaitlist = false;
        for (WaitlistEntry entry : updatedEvent.getWaitListEntrants()) {
            if (entry.getEntrantHardwareID().equals(entrant.getHardwareID())) {
                onWaitlist = true;
                break;
            }
        }
        assertTrue("Entrant should be on the waitlist", onWaitlist);

        User updatedUser = Tasks.await(testDatabase.getUser(entrant.getHardwareID()));
        assertTrue(((Entrant) updatedUser).getRegisteredEventHistory().contains(event.getUniqueEventID()));
    }

    /**
     * US 01.01.02: Leave Waitlist
     */
    @Test
    public void testLeaveWaitlist() throws ExecutionException, InterruptedException, ParseException {
        Entrant entrant = createEntrant("entrant-leave");
        Event event = createEvent("Leave Event", "org-1");

        // Setup: Join first
        Tasks.await(testDatabase.addEntrantToWaitlist(String.valueOf(event.getUniqueEventID()), entrant));
        entrant.addEventToRegisteredEventHistory(event.getUniqueEventID());
        Tasks.await(testDatabase.setUserData(entrant.getHardwareID(), entrant));

        // Execute: Leave
        Tasks.await(testDatabase.removeEntrantFromWaitlist(String.valueOf(event.getUniqueEventID()), entrant));
        entrant.removeEventFromRegisteredEventHistory(event.getUniqueEventID());
        Tasks.await(testDatabase.setUserData(entrant.getHardwareID(), entrant));

        // Assert
        Event updatedEvent = Tasks.await(testDatabase.getEvent(event.getUniqueEventID()));
        boolean onWaitlist = false;
        for (WaitlistEntry entry : updatedEvent.getWaitListEntrants()) {
            if (entry.getEntrantHardwareID().equals(entrant.getHardwareID())) {
                onWaitlist = true;
                break;
            }
        }
        assertFalse("Entrant should NOT be on the waitlist", onWaitlist);

        User updatedUser = Tasks.await(testDatabase.getUser(entrant.getHardwareID()));
        assertFalse(((Entrant) updatedUser).getRegisteredEventHistory().contains(event.getUniqueEventID()));
    }

    /**
     * US 01.01.03: Browse Events
     */
    @Test
    public void testSeeListOfEvents() throws ExecutionException, InterruptedException, ParseException {
        createEvent("Browse Event 1", "org-1");
        createEvent("Browse Event 2", "org-1");
        createEvent("Browse Event 3", "org-2");

        List<Event> allEvents = Tasks.await(testDatabase.getAllEventsList());

        assertNotNull(allEvents);
        assertEquals(3, allEvents.size());
    }

    /**
     * US 01.01.04: Filter Events
     */
    @Test
    public void testFilterEventsByName() throws ExecutionException, InterruptedException, ParseException {
        Event soccer = createEvent("Soccer Match", "org-1");
        createEvent("Chess Tournament", "org-1");

        List<Event> allEvents = Tasks.await(testDatabase.getAllEventsList());

        String query = "Soccer";
        List<Event> filtered = allEvents.stream()
                .filter(e -> e.getEventName().contains(query))
                .collect(Collectors.toList());

        assertEquals(1, filtered.size());
        assertEquals(soccer.getUniqueEventID(), filtered.get(0).getUniqueEventID());
    }

    // ==========================================
    // USER STORIES (US 01.02.xx)
    // ==========================================

    /**
     * US 01.02.01: Provide Personal Info (Create)
     */
    @Test
    public void testProvidePersonalInformation() throws ExecutionException, InterruptedException {
        String hardwareId = "test-device-01";
        Entrant newEntrant = new Entrant(hardwareId, "John", "Doe", "john@example.com", "123-456-7890", false);

        Tasks.await(testDatabase.setUserData(hardwareId, newEntrant));
        usersToClean.add(newEntrant);

        Entrant fetchedEntrant = (Entrant) Tasks.await(testDatabase.getUser(hardwareId));
        assertEquals("John", fetchedEntrant.getFirstName());
        assertEquals("john@example.com", fetchedEntrant.getEmail());
    }

    /**
     * US 01.02.02: Update Info
     */
    @Test
    public void testUpdatePersonalInformation() throws ExecutionException, InterruptedException {
        String hardwareId = "test-device-02";
        Entrant entrant = new Entrant(hardwareId, "Jane", "Doe", "jane@old.com");
        Tasks.await(testDatabase.setUserData(hardwareId, entrant));
        usersToClean.add(entrant);

        // Update
        entrant.setFirstName("Janet");
        entrant.setEmail("janet@new.com");
        Tasks.await(testDatabase.setUserData(hardwareId, entrant));

        Entrant updatedEntrant = (Entrant) Tasks.await(testDatabase.getUser(hardwareId));
        assertEquals("Janet", updatedEntrant.getFirstName());
        assertEquals("janet@new.com", updatedEntrant.getEmail());
    }

    /**
     * US 01.02.03: Event History
     */
    @Test
    public void testEventHistory() throws ExecutionException, InterruptedException, ParseException {
        Entrant entrant = new Entrant("test-history-user", "History", "Buff", "hist@test.com");
        Tasks.await(testDatabase.setUserData(entrant.getHardwareID(), entrant));
        usersToClean.add(entrant);

        Event event1 = createEvent("History Event 1", "org-1");
        Event event2 = createEvent("History Event 2", "org-1");

        entrant.addEventToRegisteredEventHistory(event1.getUniqueEventID());
        entrant.addEventToRegisteredEventHistory(event2.getUniqueEventID());
        Tasks.await(testDatabase.setUserData(entrant.getHardwareID(), entrant));

        Entrant fetchedEntrant = (Entrant) Tasks.await(testDatabase.getUser(entrant.getHardwareID()));
        assertTrue(fetchedEntrant.getRegisteredEventHistory().contains(event1.getUniqueEventID()));
        assertTrue(fetchedEntrant.getRegisteredEventHistory().contains(event2.getUniqueEventID()));
    }

    /**
     * US 01.02.04: Delete Profile
     */
    @Test
    public void testDeleteProfile() throws ExecutionException, InterruptedException {
        String hardwareId = "delete-me-user";
        Entrant entrant = new Entrant(hardwareId, "Delete", "Me", "delete@test.com");
        Tasks.await(testDatabase.setUserData(hardwareId, entrant));

        // Execute Delete
        Tasks.await(testDatabase.removeUserData(hardwareId));

        User deletedUser = Tasks.await(testDatabase.getUser(hardwareId));
        assertNull(deletedUser);
    }
}