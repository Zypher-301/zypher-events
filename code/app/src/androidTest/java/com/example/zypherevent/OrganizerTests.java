package com.example.zypherevent;

import com.example.zypherevent.userTypes.Entrant;
import com.example.zypherevent.userTypes.Organizer;
import com.example.zypherevent.userTypes.User;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.DocumentSnapshot;

import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Instrumented tests for Organizer User Stories.
 * Covers:
 * - US 02.01.01: Create events
 * - US 02.02.01: View/Manage Waitlists
 * - US 02.03.01: Limit Waitlist Capacity
 * - US 02.05.02: Sample Entrants (Lottery Logic)
 * - US 02.02.03: Geolocation Requirements
 * - US 02.06.04: Handling Cancellations
 */
@RunWith(AndroidJUnit4.class)
public class OrganizerTests {

    // Test collections
    private static final String TEST_USERS_COLLECTION = "test_users";
    private static final String TEST_EVENTS_COLLECTION = "test_events";
    private static final String TEST_NOTIFICATIONS_COLLECTION = "test_notifications";
    private static final String TEST_EXTRAS_COLLECTION = "test_extras";

    private static final Long TEST_ID_START = 700000L;
    private static Database testDatabase;
    private static FirebaseFirestore firestoreDb;

    // Cleanup lists
    private List<User> usersToClean = new ArrayList<>();
    private List<Event> eventsToClean = new ArrayList<>();
    private List<Notification> notificationsToClean = new ArrayList<>();

    @BeforeClass
    public static void setUpClass() throws ExecutionException, InterruptedException {
        testDatabase = new Database(TEST_USERS_COLLECTION, TEST_EVENTS_COLLECTION, TEST_NOTIFICATIONS_COLLECTION,
                TEST_EXTRAS_COLLECTION);
        firestoreDb = FirebaseFirestore.getInstance();
        resetUniqueCounters();
    }

    @AfterClass
    public static void cleanUpClass() throws ExecutionException, InterruptedException {
        DocumentReference uniqueRef = firestoreDb.collection(TEST_EXTRAS_COLLECTION).document("uniqueIdentifierData");
        Tasks.await(uniqueRef.delete());
        clearCollection(TEST_USERS_COLLECTION);
        clearCollection(TEST_EVENTS_COLLECTION);
        clearCollection(TEST_NOTIFICATIONS_COLLECTION);
    }

    @Before
    public void setUp() throws ExecutionException, InterruptedException {
        usersToClean.clear();
        eventsToClean.clear();
        notificationsToClean.clear();
        clearCollection(TEST_USERS_COLLECTION);
        clearCollection(TEST_EVENTS_COLLECTION);
        clearCollection(TEST_NOTIFICATIONS_COLLECTION);
        resetUniqueCounters();
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        for (User u : usersToClean)
            Tasks.await(testDatabase.removeUserData(u.getHardwareID()));
        for (Event e : eventsToClean)
            Tasks.await(testDatabase.removeEventData(e.getUniqueEventID()));
        for (Notification n : notificationsToClean)
            Tasks.await(testDatabase.removeNotificationData(n.getUniqueNotificationID()));
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
            for (DocumentSnapshot doc : qs.getDocuments())
                batch.delete(doc.getReference());
            Tasks.await(batch.commit());
        }
    }

    private Organizer createOrganizer(String id) throws ExecutionException, InterruptedException {
        Organizer o = new Organizer(id, "Test", "Organizer");
        Tasks.await(testDatabase.setUserData(id, o));
        usersToClean.add(o);
        return o;
    }

    private Entrant createEntrant(String id) throws ExecutionException, InterruptedException {
        Entrant e = new Entrant(id, "Test", "Entrant", "test@email.com");
        Tasks.await(testDatabase.setUserData(id, e));
        usersToClean.add(e);
        return e;
    }

    // ==========================================
    // ORGANIZER USER STORY TESTS
    // ==========================================

    /**
     * US 02.01.01: As an organizer I want to create a new event.
     */
    @Test
    public void testCreateEvent() throws ExecutionException, InterruptedException, ParseException {
        Organizer organizer = createOrganizer("org-creator");
        Long eventId = Tasks.await(testDatabase.getUniqueEventID());

        Date start = Utils.createWholeDayDate("2025-12-01");
        Date regStart = Utils.createWholeDayDate("2025-01-01");
        Date regEnd = Utils.createWholeDayDate("2025-11-30");

        Event newEvent = new Event(eventId, "Created Event", "Description", start, "Edmonton",
                regStart, regEnd, organizer.getHardwareID(), "http://poster.url", false);

        Tasks.await(testDatabase.setEventData(eventId, newEvent));
        eventsToClean.add(newEvent);

        // Assert
        Event fetchedEvent = Tasks.await(testDatabase.getEvent(eventId));
        assertNotNull("Event should exist", fetchedEvent);
        assertEquals("Organizer ID should match", organizer.getHardwareID(), fetchedEvent.getEventOrganizerHardwareID());
        assertEquals("Event Name should match", "Created Event", fetchedEvent.getEventName());
    }

    /**
     * US 02.02.01: As an organizer I want to view the list of entrants who joined my event waiting list.
     */
    @Test
    public void testViewWaitlist() throws ExecutionException, InterruptedException, ParseException {
        Organizer organizer = createOrganizer("org-view-list");
        Long eventId = Tasks.await(testDatabase.getUniqueEventID());

        Event event = new Event(eventId, "Waitlist View Event", "Desc", null, "Loc", null, null, organizer.getHardwareID(), false);
        Tasks.await(testDatabase.setEventData(eventId, event));
        eventsToClean.add(event);

        Entrant e1 = createEntrant("entrant-1");
        Entrant e2 = createEntrant("entrant-2");

        // Add to waitlist
        Tasks.await(testDatabase.addEntrantToWaitlist(String.valueOf(eventId), e1));
        Tasks.await(testDatabase.addEntrantToWaitlist(String.valueOf(eventId), e2));

        // Assert
        Event updatedEvent = Tasks.await(testDatabase.getEvent(eventId));
        ArrayList<WaitlistEntry> waitlist = updatedEvent.getWaitListEntrants();

        assertEquals("Waitlist should have 2 entrants", 2, waitlist.size());

        boolean foundE1 = waitlist.stream().anyMatch(entry -> entry.getEntrantHardwareID().equals(e1.getHardwareID()));
        boolean foundE2 = waitlist.stream().anyMatch(entry -> entry.getEntrantHardwareID().equals(e2.getHardwareID()));

        assertTrue("Entrant 1 should be in waitlist", foundE1);
        assertTrue("Entrant 2 should be in waitlist", foundE2);
    }

    /**
     * US 02.03.01: As an organizer I want to OPTIONALLY limit the number of entrants who can join my waiting list.
     */
    @Test
    public void testLimitWaitlist() throws ExecutionException, InterruptedException, ParseException {
        Organizer organizer = createOrganizer("org-limiter");
        Long eventId = Tasks.await(testDatabase.getUniqueEventID());

        // Event with limit of 1
        Event event = new Event(eventId, "Limited Event", "Desc", null, "Loc", null, null, organizer.getHardwareID(), false);
        event.setWaitlistLimit(1);
        Tasks.await(testDatabase.setEventData(eventId, event));
        eventsToClean.add(event);

        Entrant e1 = createEntrant("entrant-success");
        Entrant e2 = createEntrant("entrant-fail");

        // First add succeeds
        Tasks.await(testDatabase.addEntrantToWaitlist(String.valueOf(eventId), e1));

        // Second add should throw RuntimeException
        try {
            Tasks.await(testDatabase.addEntrantToWaitlist(String.valueOf(eventId), e2));
            fail("Should have thrown exception for full waitlist");
        } catch (Exception e) {
            assertTrue("Exception message should indicate full", e.getMessage().contains("Waitlist is full"));
        }

        // Verify only 1 person is in list
        Event updatedEvent = Tasks.await(testDatabase.getEvent(eventId));
        assertEquals("Waitlist size should stay at 1", 1, updatedEvent.getWaitListEntrants().size());
    }

    /**
     * US 02.05.02: As an organizer I want to set the system to sample a specified number of attendees (Lottery).
     */
    @Test
    public void testSampleEntrants() throws ExecutionException, InterruptedException, ParseException {
        Organizer organizer = createOrganizer("org-lottery");
        Long eventId = Tasks.await(testDatabase.getUniqueEventID());

        Event event = new Event(eventId, "Lottery Event", "Desc", null, "Loc", null, null, organizer.getHardwareID(), false);
        Tasks.await(testDatabase.setEventData(eventId, event));
        eventsToClean.add(event);

        Entrant winner1 = createEntrant("winner-1");
        Entrant winner2 = createEntrant("winner-2");
        Entrant loser = createEntrant("loser");

        // Add all to waitlist
        Tasks.await(testDatabase.addEntrantToWaitlist(String.valueOf(eventId), winner1));
        Tasks.await(testDatabase.addEntrantToWaitlist(String.valueOf(eventId), winner2));
        Tasks.await(testDatabase.addEntrantToWaitlist(String.valueOf(eventId), loser));

        // Simulate Lottery Draw (moving 2 to invited)
        Tasks.await(testDatabase.moveEntrantToInvited(String.valueOf(eventId), winner1));
        Tasks.await(testDatabase.moveEntrantToInvited(String.valueOf(eventId), winner2));

        // Assert
        Event updatedEvent = Tasks.await(testDatabase.getEvent(eventId));

        // Check Invited List
        ArrayList<String> invited = updatedEvent.getInvitedEntrants();
        assertEquals("Should have 2 invited", 2, invited.size());
        assertTrue("Winner 1 invited", invited.contains(winner1.getHardwareID()));
        assertTrue("Winner 2 invited", invited.contains(winner2.getHardwareID()));

        // Check Waitlist
        ArrayList<WaitlistEntry> waitlist = updatedEvent.getWaitListEntrants();
        assertEquals("Should have 1 left in waitlist", 1, waitlist.size());
        assertEquals("Loser remains", loser.getHardwareID(), waitlist.get(0).getEntrantHardwareID());
    }

    /**
     * US 02.02.03: As an organizer I want to enable or disable the geolocation requirement for my event.
     */
    @Test
    public void testToggleGeolocation() throws ExecutionException, InterruptedException, ParseException {
        Organizer organizer = createOrganizer("org-geo");
        Long eventId = Tasks.await(testDatabase.getUniqueEventID());

        // Default: false
        Event event = new Event(eventId, "Geo Event", "Desc", null, "Loc", null, null, organizer.getHardwareID(), false);
        Tasks.await(testDatabase.setEventData(eventId, event));
        eventsToClean.add(event);

        // Update to true
        event.setRequiresGeolocation(true);
        Tasks.await(testDatabase.setEventData(eventId, event));

        // Assert
        Event fetchedEvent = Tasks.await(testDatabase.getEvent(eventId));
        assertTrue("Geolocation should be enabled", fetchedEvent.getRequiresGeolocation());
    }

    /**
     * US 02.06.04 & US 02.05.03: Cancel entrants and draw replacement.
     */
    @Test
    public void testCancelAndReplace() throws ExecutionException, InterruptedException, ParseException {
        Organizer organizer = createOrganizer("org-replace");
        Long eventId = Tasks.await(testDatabase.getUniqueEventID());

        Event event = new Event(eventId, "Replace Event", "Desc", null, "Loc", null, null, organizer.getHardwareID(), false);
        Tasks.await(testDatabase.setEventData(eventId, event));
        eventsToClean.add(event);

        Entrant userA = createEntrant("user-A-cancelled");
        Entrant userB = createEntrant("user-B-replacement");

        // Setup: User A is invited, User B is on waitlist
        event.addEntrantToInvitedList(userA.getHardwareID());
        Tasks.await(testDatabase.setEventData(eventId, event));
        Tasks.await(testDatabase.addEntrantToWaitlist(String.valueOf(eventId), userB));

        // Execute 1: Cancel User A
        Event currentEvent = Tasks.await(testDatabase.getEvent(eventId));
        currentEvent.removeEntrantFromInvitedList(userA.getHardwareID());
        currentEvent.addEntrantToDeclinedList(userA.getHardwareID()); // Or cancelled list if specific
        Tasks.await(testDatabase.setEventData(eventId, currentEvent));

        // Execute 2: Draw replacement (User B)
        Tasks.await(testDatabase.moveEntrantToInvited(String.valueOf(eventId), userB));

        // Assert
        Event finalEvent = Tasks.await(testDatabase.getEvent(eventId));

        assertFalse("User A not invited", finalEvent.getInvitedEntrants().contains(userA.getHardwareID()));
        assertTrue("User A declined/cancelled", finalEvent.getDeclinedEntrants().contains(userA.getHardwareID()));

        assertTrue("User B is now invited", finalEvent.getInvitedEntrants().contains(userB.getHardwareID()));
        boolean userBOnWaitlist = finalEvent.getWaitListEntrants().stream()
                .anyMatch(e -> e.getEntrantHardwareID().equals(userB.getHardwareID()));
        assertFalse("User B removed from waitlist", userBOnWaitlist);
    }
}