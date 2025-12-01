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

    /**
     * US 02.01.04: As an organizer, I want to set a registration period.
     */
    @Test
    public void testRegistrationPeriod() throws ExecutionException, InterruptedException, ParseException {
        Organizer organizer = createOrganizer("org-reg-period");
        Long eventId = Tasks.await(testDatabase.getUniqueEventID());

        // Create dates relative to now
        Date now = new Date();
        Date pastStart = new Date(now.getTime() - 86400000L); // 1 day ago
        Date futureEnd = new Date(now.getTime() + 86400000L); // 1 day future
        Date futureStart = new Date(now.getTime() + 86400000L); // 1 day future
        Date pastEnd = new Date(now.getTime() - 86400000L); // 1 day ago

        // Open Registration (past start, future end)
        Event openEvent = new Event(eventId, "Open Event", "Desc", null, "Loc", pastStart, futureEnd,
                organizer.getHardwareID(), false);
        Tasks.await(testDatabase.setEventData(eventId, openEvent));
        eventsToClean.add(openEvent);

        Event fetchedOpen = Tasks.await(testDatabase.getEvent(eventId));
        assertTrue("Registration should be open", fetchedOpen.isRegistrationOpen());
        assertEquals("Status should be empty (open)", "", fetchedOpen.getRegistrationStatus());

        // Not Started (future start)
        Long eventId2 = Tasks.await(testDatabase.getUniqueEventID());
        Event futureEvent = new Event(eventId2, "Future Event", "Desc", null, "Loc", futureStart, null,
                organizer.getHardwareID(), false);
        Tasks.await(testDatabase.setEventData(eventId2, futureEvent));
        eventsToClean.add(futureEvent);

        Event fetchedFuture = Tasks.await(testDatabase.getEvent(eventId2));
        assertFalse("Registration should not be open", fetchedFuture.isRegistrationOpen());
        assertEquals("Status should be 'Registration opens soon'", "Registration opens soon",
                fetchedFuture.getRegistrationStatus());

        // Closed (past end)
        Long eventId3 = Tasks.await(testDatabase.getUniqueEventID());
        Event closedEvent = new Event(eventId3, "Closed Event", "Desc", null, "Loc", null, pastEnd,
                organizer.getHardwareID(), false);
        Tasks.await(testDatabase.setEventData(eventId3, closedEvent));
        eventsToClean.add(closedEvent);

        Event fetchedClosed = Tasks.await(testDatabase.getEvent(eventId3));
        assertFalse("Registration should not be open", fetchedClosed.isRegistrationOpen());
        assertEquals("Status should be 'Registration closed'", "Registration closed",
                fetchedClosed.getRegistrationStatus());
    }

    /**
     * US 02.04.01 & 02.04.02: As an organizer I want to upload/update an event
     * poster.
     */
    @Test
    public void testPosterManagement() throws ExecutionException, InterruptedException, ParseException {
        Organizer organizer = createOrganizer("org-poster");
        Long eventId = Tasks.await(testDatabase.getUniqueEventID());

        // Create event with no poster
        Event event = new Event(eventId, "Poster Event", "Desc", null, "Loc", null, null, organizer.getHardwareID(),
                false);
        Tasks.await(testDatabase.setEventData(eventId, event));
        eventsToClean.add(event);

        // Verify initial state
        Event fetched1 = Tasks.await(testDatabase.getEvent(eventId));
        assertNull("Poster URL should be null initially", fetched1.getPosterURL());

        // Update poster URL (Upload)
        String posterUrl = "https://example.com/poster.jpg";
        fetched1.setPosterURL(posterUrl);
        Tasks.await(testDatabase.setEventData(eventId, fetched1));

        // Verify update
        Event fetched2 = Tasks.await(testDatabase.getEvent(eventId));
        assertEquals("Poster URL should match", posterUrl, fetched2.getPosterURL());

        // Update poster URL again (Update)
        String newPosterUrl = "https://example.com/new_poster.jpg";
        fetched2.setPosterURL(newPosterUrl);
        Tasks.await(testDatabase.setEventData(eventId, fetched2));

        // Verify second update
        Event fetched3 = Tasks.await(testDatabase.getEvent(eventId));
        assertEquals("New Poster URL should match", newPosterUrl, fetched3.getPosterURL());
    }

    /**
     * US 02.06.01, 02.06.02, 02.06.03: View invited, cancelled, enrolled entrants.
     */
    @Test
    public void testViewEntrantLists() throws ExecutionException, InterruptedException, ParseException {
        Organizer organizer = createOrganizer("org-lists");
        Long eventId = Tasks.await(testDatabase.getUniqueEventID());

        Event event = new Event(eventId, "List View Event", "Desc", null, "Loc", null, null, organizer.getHardwareID(),
                false);
        Tasks.await(testDatabase.setEventData(eventId, event));
        eventsToClean.add(event);

        Entrant invited = createEntrant("entrant-invited");
        Entrant cancelled = createEntrant("entrant-cancelled");
        Entrant enrolled = createEntrant("entrant-enrolled");

        // Setup lists
        event.addEntrantToInvitedList(invited.getHardwareID());
        event.addEntrantToCancelledList(cancelled.getHardwareID());
        event.addEntrantToAcceptedList(enrolled.getHardwareID());
        Tasks.await(testDatabase.setEventData(eventId, event));

        // Fetch and Verify
        Event fetched = Tasks.await(testDatabase.getEvent(eventId));

        // US 02.06.01
        assertTrue("Should contain invited entrant", fetched.getInvitedEntrants().contains(invited.getHardwareID()));
        // US 02.06.02
        assertTrue("Should contain cancelled entrant",
                fetched.getCancelledEntrants().contains(cancelled.getHardwareID()));
        // US 02.06.03
        assertTrue("Should contain enrolled (accepted) entrant",
                fetched.getAcceptedEntrants().contains(enrolled.getHardwareID()));
    }

    /**
     * US 02.07.01, 02.07.02, 02.07.03: Send notifications to entrants.
     * Tests that notifications are correctly saved to the database.
     */
    @Test
    public void testSendNotifications() throws ExecutionException, InterruptedException {
        Organizer organizer = createOrganizer("org-notif");
        Entrant recipient = createEntrant("entrant-recipient");
        Long eventId = Tasks.await(testDatabase.getUniqueEventID());

        // Create a notification
        Long notifId = Tasks.await(testDatabase.getUniqueNotificationID());
        Notification notification = new Notification(notifId, organizer.getHardwareID(), recipient.getHardwareID(),
                "Test Title", "Test Body", eventId, false);

        Tasks.await(testDatabase.setNotificationData(notifId, notification));
        notificationsToClean.add(notification);

        // Verify it exists in DB
        Notification fetched = Tasks.await(testDatabase.getNotification(notifId));
        assertNotNull("Notification should exist", fetched);
        assertEquals("Title should match", "Test Title", fetched.getNotificationHeader());
        assertEquals("Body should match", "Test Body", fetched.getNotificationBody());
        assertEquals("Recipient should match", recipient.getHardwareID(), fetched.getReceivingUserHardwareID());
    }

    /**
     * US 02.06.05: Export to CSV.
     * Simulates the CSV generation logic.
     */
    @Test
    public void testCSVExportLogic() throws ExecutionException, InterruptedException {
        Organizer organizer = createOrganizer("org-csv");
        Long eventId = Tasks.await(testDatabase.getUniqueEventID());

        Event event = new Event(eventId, "CSV Event", "Desc", null, "Loc", null, null, organizer.getHardwareID(),
                false);

        Entrant e1 = createEntrant("csv-1");
        e1.setFirstName("John");
        e1.setLastName("Doe");
        Tasks.await(testDatabase.setUserData(e1.getHardwareID(), e1));

        Entrant e2 = createEntrant("csv-2");
        e2.setFirstName("Jane");
        e2.setLastName("Smith");
        Tasks.await(testDatabase.setUserData(e2.getHardwareID(), e2));

        event.addEntrantToAcceptedList(e1.getHardwareID());
        event.addEntrantToAcceptedList(e2.getHardwareID());
        Tasks.await(testDatabase.setEventData(eventId, event));
        eventsToClean.add(event);

        // Simulate CSV Generation
        List<String> names = new ArrayList<>();
        for (String id : event.getAcceptedEntrants()) {
            User u = Tasks.await(testDatabase.getUser(id));
            names.add(u.getFirstName() + " " + u.getLastName());
        }
        String csvOutput = String.join(", ", names);

        // Verify format
        assertTrue("Should contain John Doe", csvOutput.contains("John Doe"));
        assertTrue("Should contain Jane Smith", csvOutput.contains("Jane Smith"));
        assertTrue("Should be comma separated", csvOutput.contains(", "));
    }
}