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
 * - 01.04.xx: Notifications (Win/Loss, Opt-out)
 * - 01.05.xx: Invitation Management (Accept/Decline, Another Chance, Waitlist Count, Criteria)
 * - 01.06.xx: QR Code Event Details and Sign-up
 * - 01.07.xx: Device Identification
 */
@RunWith(AndroidJUnit4.class)
public class EntrantTests {

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
        for (User u : usersToClean)
            Tasks.await(testDatabase.removeUserData(u.getHardwareID()));
        for (Event e : eventsToClean)
            Tasks.await(testDatabase.removeEventData(e.getUniqueEventID()));
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

    private Entrant createEntrant(String id) throws ExecutionException, InterruptedException {
        Entrant e = new Entrant(id, "Test", "Entrant", "test@email.com");
        Tasks.await(testDatabase.setUserData(id, e));
        usersToClean.add(e);
        return e;
    }

    private Event createEvent(String name, String orgId)
            throws ExecutionException, InterruptedException, ParseException {
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

    // ==========================================
    // USER STORIES (US 01.04.xx)
    // ==========================================

    /**
     * US 01.04.01: As an entrant I want to receive notification when I am chosen
     * to participate from the waiting list (when I "win" the lottery).
     * * Logic:
     * 1. Create an Entrant and an Event.
     * 2. Simulate the system/organizer sending a "You Won" notification.
     * 3. Fetch all notifications for this entrant.
     * 4. Verify the specific "Won" notification is present.
     */
    @Test
    public void testReceiveWinNotification() throws ExecutionException, InterruptedException, ParseException {
        // Setup
        Entrant entrant = createEntrant("entrant-winner");
        Event event = createEvent("Lottery Event", "org-sender");
        Long notifId = Tasks.await(testDatabase.getUniqueNotificationID());

        // Execute: Simulate System sending a "Win" Notification
        Notification winNotif = new Notification(
                notifId,
                "org-sender", // Sender
                entrant.getHardwareID(), // Receiver (Our Entrant)
                "You Won!", // Header
                "You have been selected to participate.", // Body
                event.getUniqueEventID(), // Linked Event
                true // isInvitation
        );
        Tasks.await(testDatabase.setNotificationData(notifId, winNotif));

        // Add to cleanup
        final Notification cleanNotif = winNotif;

        // Assert: Entrant fetches their notifications
        List<Notification> allNotifs = Tasks.await(testDatabase.getAllNotifications());

        // Filter for our entrant only
        List<Notification> myNotifs = allNotifs.stream()
                .filter(n -> n.getReceivingUserHardwareID().equals(entrant.getHardwareID()))
                .collect(Collectors.toList());

        assertFalse("Should have notifications", myNotifs.isEmpty());
        assertTrue("Should contain the Win notification",
                myNotifs.stream().anyMatch(n -> n.getUniqueNotificationID().equals(notifId)));

        // Cleanup
        Tasks.await(testDatabase.removeNotificationData(notifId));
    }

    /**
     * US 01.04.02: As an entrant I want to receive notification of when I am not
     * chosen
     * on the app (when I "lose" the lottery).
     * * Logic:
     * 1. Create an Entrant.
     * 2. Simulate the system sending a "You Lost" notification.
     * 3. Verify the entrant can retrieve it from the database.
     */
    @Test
    public void testReceiveLossNotification() throws ExecutionException, InterruptedException {
        // Setup
        Entrant entrant = createEntrant("entrant-loser");
        Long notifId = Tasks.await(testDatabase.getUniqueNotificationID());

        // Execute: Simulate System sending a "Loss" Notification
        Notification lossNotif = new Notification(
                notifId,
                "system-admin",
                entrant.getHardwareID(),
                "Lottery Results",
                "Unfortunately, you were not selected this time.");
        Tasks.await(testDatabase.setNotificationData(notifId, lossNotif));

        // Assert
        List<Notification> allNotifs = Tasks.await(testDatabase.getAllNotifications());

        boolean found = allNotifs.stream()
                .anyMatch(n -> n.getUniqueNotificationID().equals(notifId) &&
                        n.getReceivingUserHardwareID().equals(entrant.getHardwareID()));

        assertTrue("Entrant should find the Loss notification", found);

        // Cleanup
        Tasks.await(testDatabase.removeNotificationData(notifId));
    }

    /**
     * US 01.04.03: As an entrant I want to opt out of receiving notifications
     * from organizers and admins.
     * * Logic:
     * 1. Create an Entrant (default notifications enabled).
     * 2. Toggle the preference to FALSE.
     * 3. Save to database.
     * 4. Fetch the user again and verify the preference persisted.
     */
    @Test
    public void testOptOutNotifications() throws ExecutionException, InterruptedException {
        // Setup: Create entrant, assert default is TRUE
        Entrant entrant = createEntrant("entrant-opt-out");

        // Ensure it starts as true for this test
        entrant.setWantsNotifications(true);
        Tasks.await(testDatabase.setUserData(entrant.getHardwareID(), entrant));

        // Execute: User opts out
        entrant.setWantsNotifications(false);
        Tasks.await(testDatabase.setUserData(entrant.getHardwareID(), entrant));

        // Assert: Fetch fresh from DB
        User fetchedUser = Tasks.await(testDatabase.getUser(entrant.getHardwareID()));
        assertTrue("User should be Entrant", fetchedUser instanceof Entrant);
        assertFalse("WantsNotifications should be FALSE in DB", ((Entrant) fetchedUser).getWantsNotifications());
    }

    // ==========================================
    // USER STORIES (US 01.05.xx)
    // ==========================================

    /**
     * US 01.05.01: As an entrant I want another chance to be chosen from the
     * waiting list
     * if a selected user declines an invitation to sign up.
     * * * Test Logic:
     * This test verifies the data integrity required for "Another Chance".
     * If User A declines, they must be moved to the 'declined' list, and User B
     * must remain on the 'waitlist'. This ensures the Organizer's sampling
     * algorithm
     * has the correct data state to pick User B in a re-draw.
     */
    @Test
    public void testDeclineOpensSlot() throws ExecutionException, InterruptedException, ParseException {
        // Setup: Event with 1 Invited user and 1 Waitlisted user
        Event event = createEvent("Re-Draw Event", "org-1");
        Entrant userInvited = createEntrant("user-invite-decline");
        Entrant userWaitlist = createEntrant("user-waitlist-hopeful");

        // Manually set up the state
        event.addEntrantToInvitedList(userInvited.getHardwareID());
        event.addEntrantToWaitList(userWaitlist.getHardwareID());
        Tasks.await(testDatabase.setEventData(event.getUniqueEventID(), event));

        // Execute: User A Declines
        event.removeEntrantFromInvitedList(userInvited.getHardwareID());
        event.addEntrantToDeclinedList(userInvited.getHardwareID());
        Tasks.await(testDatabase.setEventData(event.getUniqueEventID(), event));

        // Assert: Verify State for Re-Draw
        Event updatedEvent = Tasks.await(testDatabase.getEvent(event.getUniqueEventID()));

        // 1. User A is definitely declined
        assertTrue("Invited user should be in declined list",
                updatedEvent.getDeclinedEntrants().contains(userInvited.getHardwareID()));
        assertFalse("Invited user should NOT be in invited list",
                updatedEvent.getInvitedEntrants().contains(userInvited.getHardwareID()));

        // 2. User B is still available on waitlist
        boolean userB_StillWaiting = updatedEvent.getWaitListEntrants().stream()
                .anyMatch(e -> e.getEntrantHardwareID().equals(userWaitlist.getHardwareID()));
        assertTrue("Waitlist user should still be available for the next draw", userB_StillWaiting);
    }

    /**
     * US 01.05.02: As an entrant I want to be able to accept the invitation
     * to register/sign up when chosen to participate in an event.
     * * Logic:
     * 1. Place Entrant in 'Invited' list.
     * 2. Simulate 'Accept' action (Remove from Invited -> Add to Accepted).
     * 3. Verify persistence.
     */
    @Test
    public void testAcceptInvitation() throws ExecutionException, InterruptedException, ParseException {
        // Setup
        Event event = createEvent("Accept Event", "org-1");
        Entrant entrant = createEntrant("entrant-accept");

        // Set initial state: Invited
        event.addEntrantToInvitedList(entrant.getHardwareID());
        Tasks.await(testDatabase.setEventData(event.getUniqueEventID(), event));

        // Execute: Accept Invitation
        event.removeEntrantFromInvitedList(entrant.getHardwareID());
        event.addEntrantToAcceptedList(entrant.getHardwareID());
        Tasks.await(testDatabase.setEventData(event.getUniqueEventID(), event));

        // Assert
        Event updatedEvent = Tasks.await(testDatabase.getEvent(event.getUniqueEventID()));
        assertTrue("Entrant should be in accepted list",
                updatedEvent.getAcceptedEntrants().contains(entrant.getHardwareID()));
        assertFalse("Entrant should NOT be in invited list",
                updatedEvent.getInvitedEntrants().contains(entrant.getHardwareID()));
    }

    /**
     * US 01.05.03: As an entrant I want to be able to decline an invitation
     * when chosen to participate in an event.
     * * Logic:
     * 1. Place Entrant in 'Invited' list.
     * 2. Simulate 'Decline' action (Remove from Invited -> Add to Declined).
     * 3. Verify persistence.
     */
    @Test
    public void testDeclineInvitation() throws ExecutionException, InterruptedException, ParseException {
        // Setup
        Event event = createEvent("Decline Event", "org-1");
        Entrant entrant = createEntrant("entrant-decline");

        // Set initial state: Invited
        event.addEntrantToInvitedList(entrant.getHardwareID());
        Tasks.await(testDatabase.setEventData(event.getUniqueEventID(), event));

        // Execute: Decline Invitation
        event.removeEntrantFromInvitedList(entrant.getHardwareID());
        event.addEntrantToDeclinedList(entrant.getHardwareID());
        Tasks.await(testDatabase.setEventData(event.getUniqueEventID(), event));

        // Assert
        Event updatedEvent = Tasks.await(testDatabase.getEvent(event.getUniqueEventID()));
        assertTrue("Entrant should be in declined list",
                updatedEvent.getDeclinedEntrants().contains(entrant.getHardwareID()));
        assertFalse("Entrant should NOT be in invited list",
                updatedEvent.getInvitedEntrants().contains(entrant.getHardwareID()));
    }

    /**
     * US 01.05.04: As an entrant, I want to know how many total entrants are on the
     * waiting list.
     * * Logic:
     * 1. Add 3 entrants to the waitlist.
     * 2. Retrieve the event.
     * 3. Verify the size of the waitlist list is 3.
     */
    @Test
    public void testSeeWaitlistCount() throws ExecutionException, InterruptedException, ParseException {
        Event event = createEvent("Popular Event", "org-1");
        Entrant e1 = createEntrant("e1");
        Entrant e2 = createEntrant("e2");
        Entrant e3 = createEntrant("e3");

        // Add 3 entrants
        Tasks.await(testDatabase.addEntrantToWaitlist(String.valueOf(event.getUniqueEventID()), e1));
        Tasks.await(testDatabase.addEntrantToWaitlist(String.valueOf(event.getUniqueEventID()), e2));
        Tasks.await(testDatabase.addEntrantToWaitlist(String.valueOf(event.getUniqueEventID()), e3));

        // Assert
        Event updatedEvent = Tasks.await(testDatabase.getEvent(event.getUniqueEventID()));
        assertNotNull(updatedEvent.getWaitListEntrants());
        assertEquals("Waitlist count should be 3", 3, updatedEvent.getWaitListEntrants().size());
    }

    /**
     * US 01.05.05: As an entrant, I want to be informed about the criteria or
     * guidelines
     * for the lottery selection process.
     * * Logic:
     * 1. Create an event with specific lottery criteria text.
     * 2. Save it.
     * 3. Retrieve it and verify the text is accessible.
     */
    @Test
    public void testViewLotteryCriteria() throws ExecutionException, InterruptedException, ParseException {
        // Setup
        Event event = createEvent("Strict Event", "org-1");
        String criteriaText = "Must have prior hiking experience and valid safety gear.";

        event.setLotteryCriteria(criteriaText);
        Tasks.await(testDatabase.setEventData(event.getUniqueEventID(), event));

        // Assert
        Event fetchedEvent = Tasks.await(testDatabase.getEvent(event.getUniqueEventID()));
        assertNotNull("Criteria should not be null", fetchedEvent.getLotteryCriteria());
        assertEquals("Criteria text should match", criteriaText, fetchedEvent.getLotteryCriteria());
    }

    // ==========================================
    // USER STORIES (US 01.06.xx)
    // ==========================================

    /**
     * US 01.06.01: As an entrant I want to view event details within the app
     * by scanning the promotional QR code.
     * * Logic:
     * 1. Create a target event in the database.
     * 2. Construct a simulated QR code string ("EVENT:{id}").
     * 3. Use the App's utility to extract the ID (simulating the scan).
     * 4. Fetch the event and verify the details match.
     */
    @Test
    public void testViewEventDetailsFromQR() throws ExecutionException, InterruptedException, ParseException {
        // Setup: Create the event that the QR code points to
        Event originalEvent = createEvent("QR Promo Event", "org-qr");
        originalEvent.setEventDescription("Exclusive gala.");
        originalEvent.setPosterURL("www.poster.com/img.png");
        Tasks.await(testDatabase.setEventData(originalEvent.getUniqueEventID(), originalEvent));

        // Execute: Simulate scanning a QR code string
        String simulatedQRContent = "EVENT:" + originalEvent.getUniqueEventID();

        // Use the actual app logic to parse it
        Long scannedId = Utils.extractEventId(simulatedQRContent);
        assertNotNull("Utils should extract a valid ID", scannedId);

        // Fetch the "Details" using that ID
        Event fetchedEvent = Tasks.await(testDatabase.getEvent(scannedId));

        // Assert: Verify we got the correct details
        assertNotNull("Should find event from extracted ID", fetchedEvent);
        assertEquals("Name matches", "QR Promo Event", fetchedEvent.getEventName());
        assertEquals("Description matches", "Exclusive gala.", fetchedEvent.getEventDescription());
        assertEquals("Poster URL matches", "www.poster.com/img.png", fetchedEvent.getPosterURL());
    }

    /**
     * US 01.06.02: As an entrant I want to be able to sign up for an event
     * from the event details.
     * * Logic:
     * 1. Create Entrant and Event.
     * 2. Fetch the event object (mimicking the "View Details" page load).
     * 3. Call the join waitlist logic on this fetched object.
     * 4. Verify the entrant is successfully added to the database waitlist.
     */
    @Test
    public void testSignUpFromEventDetails() throws ExecutionException, InterruptedException, ParseException {
        // Setup
        Entrant entrant = createEntrant("entrant-qr-signer");
        Event event = createEvent("QR Signup Event", "org-qr");

        // 1. Simulate viewing details
        Event detailsPageEvent = Tasks.await(testDatabase.getEvent(event.getUniqueEventID()));
        assertNotNull(detailsPageEvent);

        // 2. Execute: User clicks "Join Waitlist" on the details page
        Tasks.await(testDatabase.addEntrantToWaitlist(String.valueOf(detailsPageEvent.getUniqueEventID()), entrant));

        // 3. Update User history
        entrant.addEventToRegisteredEventHistory(detailsPageEvent.getUniqueEventID());
        Tasks.await(testDatabase.setUserData(entrant.getHardwareID(), entrant));

        // Assert: Verify entrant is on waitlist
        Event updatedEvent = Tasks.await(testDatabase.getEvent(event.getUniqueEventID()));

        boolean onWaitlist = updatedEvent.getWaitListEntrants().stream()
                .anyMatch(e -> e.getEntrantHardwareID().equals(entrant.getHardwareID()));

        assertTrue("Entrant should be on waitlist after signing up from details", onWaitlist);
    }

    // ==========================================
    // USER STORIES (US 01.07.xx)
    // ==========================================

    /**
     * US 01.07.01: As an entrant, I want to be identified by my device,
     * so that I don't have to use a username and password.
     * * Logic:
     * 1. Simulate a unique device ID (hardware ID).
     * 2. Create a user linked to this ID.
     * 3. Verify that we can retrieve the specific user profile solely using this ID
     * (Authentication by ID).
     * 4. Verify that a different/unknown ID returns null (New user scenario).
     */
    @Test
    public void testDeviceIdentification() throws ExecutionException, InterruptedException {
        String existingDeviceId = "device-id-existing";
        String newDeviceId = "device-id-new";

        // 1. Setup: User with specific device ID exists
        Entrant existingUser = new Entrant(existingDeviceId, "Device", "User", "device@test.com");
        Tasks.await(testDatabase.setUserData(existingDeviceId, existingUser));
        usersToClean.add(existingUser);

        // 2. Execute: "Log in" by fetching user with the Device ID
        User fetchedUser = Tasks.await(testDatabase.getUser(existingDeviceId));

        // 3. Assert: Existing user is identified/found without password
        assertNotNull("Should identify existing user by ID", fetchedUser);
        assertEquals("Should match the correct user", existingDeviceId, fetchedUser.getHardwareID());
        assertTrue("Should be an Entrant", fetchedUser instanceof Entrant);
        User unknownUser = Tasks.await(testDatabase.getUser(newDeviceId));

        // 5. Assert: Unknown ID is treated as new (null result)
        assertNull("Unknown device ID should return null (prompt creation)", unknownUser);
    }

    /**
     * US 01.05.02: Verify behavior when multiple invitation notifications exist for
     * the same event.
     * Logic:
     * 1. Create Event and Entrant.
     * 2. Add Entrant to Invited list.
     * 3. Create TWO notifications for this invitation.
     * 4. Simulate accepting the invitation (updating Event).
     * 5. Verify that the Entrant is in the Accepted list.
     * 6. (Implicit) In the UI, both notifications check this same Event state, so
     * both will show "Accepted".
     */
    @Test
    public void testDuplicateInvitationNotifications() throws ExecutionException, InterruptedException, ParseException {
        // Setup
        Event event = createEvent("Double Invite Event", "org-1");
        Entrant entrant = createEntrant("entrant-double-invite");

        // Invite entrant
        event.addEntrantToInvitedList(entrant.getHardwareID());
        Tasks.await(testDatabase.setEventData(event.getUniqueEventID(), event));

        // Create Notification 1
        Long notifId1 = Tasks.await(testDatabase.getUniqueNotificationID());
        Notification n1 = new Notification(notifId1, "org-1", entrant.getHardwareID(), "Invite 1", "Body 1",
                event.getUniqueEventID(), true);
        Tasks.await(testDatabase.setNotificationData(notifId1, n1));

        // Create Notification 2
        Long notifId2 = Tasks.await(testDatabase.getUniqueNotificationID());
        Notification n2 = new Notification(notifId2, "org-1", entrant.getHardwareID(), "Invite 2", "Body 2",
                event.getUniqueEventID(), true);
        Tasks.await(testDatabase.setNotificationData(notifId2, n2));

        // Execute: Simulate User accepting via Notification 1
        // This logic mirrors handleAcceptInvitation in the Fragment
        Event fetchedEvent = Tasks.await(testDatabase.getEvent(event.getUniqueEventID()));
        fetchedEvent.removeEntrantFromInvitedList(entrant.getHardwareID());
        fetchedEvent.addEntrantToAcceptedList(entrant.getHardwareID());
        Tasks.await(testDatabase.setEventData(fetchedEvent.getUniqueEventID(), fetchedEvent));

        // Assert: Verify Event State
        Event finalEvent = Tasks.await(testDatabase.getEvent(event.getUniqueEventID()));
        assertTrue("Entrant should be accepted", finalEvent.getAcceptedEntrants().contains(entrant.getHardwareID()));
        assertFalse("Entrant should not be invited", finalEvent.getInvitedEntrants().contains(entrant.getHardwareID()));

        // The UI Adapter uses this exact 'finalEvent' object to determine status for
        // BOTH n1 and n2.
        // Therefore, both will display "Status: Accepted".
    }
}