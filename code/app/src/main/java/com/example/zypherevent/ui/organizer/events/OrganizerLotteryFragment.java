package com.example.zypherevent.ui.organizer.events;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.zypherevent.Database;
import com.example.zypherevent.Event;
import com.example.zypherevent.R;
import com.example.zypherevent.WaitlistEntry;
import com.example.zypherevent.notifications.NotificationService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Britney Kunchidi
 * @author Tom Yang (connected firebase to waiting list and implemented sending notifications,
 * both in-app and Android level to accepted and declined entrants)
 * @version 2.0
 * Simple Activity to run the lottery for an event.
 * Uses popup_organizer_lottery.xml as its layout.
 * Uses NotificationService for sending Android level notifications
 *
 */
public class OrganizerLotteryFragment extends Fragment {
    private static final String TAG = "OrganizerLottery";

    private RecyclerView rvWaitlist;
    private EditText etSampleSize;
    private Button btnRunLottery;

    private Database db;
    private Event currentEvent;
    private Long eventId;
    private String organizerID;
    private ArrayList<WaitlistEntry> waitlistEntries = new ArrayList<>();
    private WaitlistEntrantAdapter waitlistAdapter;

    // NotificationService binding
    private NotificationService notificationService;
    private boolean serviceBound = false;

    public OrganizerLotteryFragment() {
        // Required empty public constructor
    }

    /**
     * ServiceConnection callback for binding to the NotificationService.
     * When connected, starts listening for notifications if the user has notifications enabled.
     */
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            NotificationService.LocalBinder binder = (NotificationService.LocalBinder) service;
            notificationService = binder.getService();
            serviceBound = true;
            Log.d(TAG, "NotificationService connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            notificationService = null;
            Log.d(TAG, "NotificationService disconnected");
        }
    };

    /**
     * Sets the event ID for this lottery fragment
     *
     * @param eventId the unique ID for the event
     */
    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    /**
     * Sets the organizer hardware ID for this lottery fragment
     *
     * @param organizerID the hardware ID of the organizer
     */
    public void setOrganizerID(String organizerID) {
        this.organizerID = organizerID;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new Database();

        // Bind to NotificationService
        Intent intent = new Intent(getContext(), NotificationService.class);
        getContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.popup_organizer_lottery, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (eventId == null) {
            Toast.makeText(getContext(), "Error: No event ID provided", Toast.LENGTH_SHORT).show();
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
            return;
        }

        // Find views from popup_organizer_lottery.xml
        rvWaitlist = view.findViewById(R.id.entrant_waitlist);
        etSampleSize = view.findViewById(R.id.etSampleSize);
        btnRunLottery = view.findViewById(R.id.run_lottery);

        // Set up recycler view for waitlist entrants
        waitlistAdapter = new WaitlistEntrantAdapter(waitlistEntries);
        rvWaitlist.setAdapter(waitlistAdapter);
        rvWaitlist.setLayoutManager(new LinearLayoutManager(getContext()));

        loadEventData();

        btnRunLottery.setOnClickListener(v -> runLottery());
    }

    /**
     * Load the event and its waitlist from Database
     */
    private void loadEventData() {
        db.getEvent(eventId)
                .addOnSuccessListener(event -> {
                    currentEvent = event;
                    if (currentEvent != null && currentEvent.getWaitListEntrants() != null) {
                        waitlistEntries.clear();
                        waitlistEntries.addAll(currentEvent.getWaitListEntrants());
                        waitlistAdapter.notifyDataSetChanged();
                        Log.d(TAG, "Loaded " + waitlistEntries.size() + " entrants from waitlist");
                    }
                })
                .addOnFailureListener(e ->
                    Log.e(TAG, "Error loading event", e));
    }

    /**
     * Reads the number to sample, randomly selects that many entrants from the waitlist,
     * and shows the result in a Toast.
     * (Added Update database with selected entrants)
     * This satisfies:
     * - random selection
     * - equal chance of being chosen (we shuffle the whole list once)
     */
    private void runLottery() {
        String input = etSampleSize.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a number to sample", Toast.LENGTH_SHORT).show();
            return;
        }

        int sampleSize;

        try {
            sampleSize = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid number", Toast.LENGTH_SHORT).show();
            return;
        }

        if (sampleSize <= 0) {
            Toast.makeText(getContext(), "Sample size must be greater than 0", Toast.LENGTH_SHORT).show();
            return;
        }

        if (waitlistEntries.isEmpty()) {
            Toast.makeText(getContext(), "No entrants in waitlist", Toast.LENGTH_SHORT).show();
            return;
        }

        // Don’t ask for more than we have
        int n = Math.min(sampleSize, waitlistEntries.size());

        // Randomly shuffle the waitlist so everyone has an equal chance
        Collections.shuffle(waitlistEntries);

        // Pick the first n entrants after shuffle
        List<WaitlistEntry> entrantsToInvite = new ArrayList<>(waitlistEntries.subList(0, n));
        List<WaitlistEntry> entrantsWithoutInvite = new ArrayList<>(waitlistEntries.subList(n, waitlistEntries.size()));

        //Update the event in database
        updateEventWithLotteryResults(entrantsToInvite, entrantsWithoutInvite);
    }

    /**
     * Updates the event in the Database and sends notifications using NotificationService.
     *
     * Selected entrants:
     *  - moved from waitlist → invited list
     * Non-selected entrants:
     *  - remain on the waitlist for future lotteries
     *
     * @param entrantsToInvite List of WaitlistEntry objects that were selected by the lottery
     * @param entrantsWithoutInvite List of WaitlistEntry objects that were not selected by lottery
     *
     */
    private void updateEventWithLotteryResults(List<WaitlistEntry> entrantsToInvite, List<WaitlistEntry> entrantsWithoutInvite) {
        // Selected entrants get invitations
        for (WaitlistEntry entry : entrantsToInvite) {
            String entrantId = entry.getEntrantHardwareID();

            // Add to invited list
            currentEvent.addEntrantToInvitedList(entrantId);

            // Remove from waitlist so they aren't drawn again while invite is pending
            currentEvent.removeEntrantFromWaitList(entry);
        }

        // Denied entrants: we *do not* remove them from the waitlist.
        // They stay for future lottery runs.
        Log.d(TAG, entrantsWithoutInvite.size() + " entrants remain on waitlist for future lottery");

        // Persist updated event
        db.setEventData(eventId, currentEvent)
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "Successfully updated event with lottery results");

                    // Notify selected entrants that they've been invited
                    sendInvitationNotification(entrantsToInvite);

                    // Notify non-selected entrants that they're still on the waitlist
                    sendWaitlistNotification(entrantsWithoutInvite);

                    Toast.makeText(
                            getContext(),
                            "Lottery complete! Invited " + entrantsToInvite.size() + " entrants.",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(e ->
                    Log.e(TAG, "Error updating event", e));
    }

    /**
     * Sends invitation notifications to the selected entrants using NotificationService
     *
     * @param selected List of WaitlistEntry objects that were selected
     */
    private void sendInvitationNotification(List<WaitlistEntry> selected) {
        if (!serviceBound || notificationService == null) {
            Log.e(TAG,"NotificationService not bound, cannot send notifications");
            return;
        }

        String title = "You've Been Selected!";
        String message = "Congratulations! You have been selected for " + currentEvent.getEventName() + ". Please accept to claim your spot before the deadline.";

        // Get all hardware IDs from selected entrants
        List<String> entrantIDs = new ArrayList<>();
        for (WaitlistEntry entry : selected) {
            entrantIDs.add(entry.getEntrantHardwareID());
        }

        notificationService.sendBulkNotifications(organizerID, entrantIDs, title, message);
        Log.d(TAG, "Invitation notification sent to: " + entrantIDs.size() + " entrants");
    }

    /**
     * Sends notification to the non-selected entrants using NotificationService
     *
     * @param notSelected List of WaitlistEntry objects that were not selected
     */
    private void sendWaitlistNotification(List<WaitlistEntry> notSelected) {
        if (!serviceBound || notificationService == null) {
            Log.e(TAG,"NotificationService not bound, cannot send notifications");
            return;
        }

        String title = "Event Update";
        String message = "You were not selected in " + currentEvent.getEventName() +
                "at this lottery run, but you will remain on the waitlist for future selections.";

        // Get all hardware IDs from waitlist entrants
        List<String> entrantIDs = new ArrayList<>();
        for (WaitlistEntry entry : notSelected) {
            entrantIDs.add(entry.getEntrantHardwareID());
        }

        notificationService.sendBulkNotifications(organizerID, entrantIDs, title, message);
        Log.d(TAG, "Waitlist notifications sent to " + entrantIDs.size() + " entrants");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (serviceBound && getContext() != null) {
            getContext().unbindService(serviceConnection);
            serviceBound = false;
        }
    }
}
