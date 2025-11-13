package com.example.zypherevent.ui.organizer.events;

import android.os.Bundle;
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
import com.example.zypherevent.Notification;
import com.example.zypherevent.R;
import com.example.zypherevent.WaitlistEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Britney Kunchidi
 * @author Tom Yang (connected firebase to waiting list and implemented sending notifications to accepted and declined entrants)
 * Simple Activity to run the lottery for an event.
 * Uses popup_organizer_lottery.xml as its layout.
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

    public OrganizerLotteryFragment() {
        // Required empty public constructor
    }

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
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading event", e);
                });
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
        List<WaitlistEntry> selectedEntrants = new ArrayList<>(waitlistEntries.subList(0, n));
        List<WaitlistEntry> deniedEntrants = new ArrayList<>(waitlistEntries.subList(n, waitlistEntries.size()));

        //Update the event in database
        updateEventWIthLotteryResults(selectedEntrants, deniedEntrants);
    }

    /**
     * Updates the event in the Database and sends notification
     *
     * @param selected List of WaitlistEntry objects that were selected
     * @param denied List of WaitlistEntry objects that were not selected
     */
    private void updateEventWIthLotteryResults(List<WaitlistEntry> selected, List<WaitlistEntry> denied) {
        for (WaitlistEntry entry : selected) {
            currentEvent.addEntrantToAcceptedList(entry.getEntrant());
            currentEvent.removeEntrantFromWaitList(entry);
        }

        // Keep a record of the denied entrants for future lottery
        Log.d(TAG, denied.size() + " entrants remain on waitlist for future lottery");

        // Update event in Database
        db.setEventData(eventId, currentEvent)
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "Successfully updated event with lottery results");

                    // Send notification to selected entrants
                    sendInvitationNotification(selected);

                    // Send notification to denied entrants
                    sendWaitlistNotification(denied);

                    Toast.makeText(getContext(), "Lottery complete! Invited " + selected.size() + " entrants.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating event", e);
                });
    }

    /**
     * Sends notification to the selected entrants
     *
     * @param selected List of WaitlistEntry objects that were selected
     */
    private void sendInvitationNotification(List<WaitlistEntry> selected) {
        for (WaitlistEntry entry : selected) {
            String entrantID = entry.getEntrant().getHardwareID();

            db.getUniqueEventID().addOnSuccessListener(notificationID -> {
                Notification notification = new Notification(notificationID, organizerID, entrantID,
                        "You've Been Selected!",
                        "Congratulations! You have been selected for " + currentEvent.getEventName() + ". Please accept or decline your invitation.");

                db.setNotificationData(notificationID, notification)
                        .addOnSuccessListener(v ->
                            Log.d(TAG, "Invitation sent to: " + entrantID))
                        .addOnFailureListener(e ->
                                Log.e(TAG, "Failed to send notification to: " + entrantID, e));
            });
        }
    }

    /**
     * Sends notification to the non-selected entrants
     *
     * @param denied List of WaitlistEntry objects that were not selected
     */
    private void sendWaitlistNotification(List<WaitlistEntry> denied) {
        for (WaitlistEntry entry : denied) {
            String entrantID = entry.getEntrant().getHardwareID();

            db.getUniqueEventID().addOnSuccessListener(notificationID -> {
                Notification notification = new Notification(notificationID, organizerID, entrantID,
                        "Event Update",
                        "You were not selected in "
                                + currentEvent.getEventName() +
                                "at this lottery run, but you will remain on the waitlist for future selections.");

                db.setNotificationData(notificationID, notification)
                        .addOnSuccessListener(v ->
                                Log.d(TAG, "Invitation sent to: " + entrantID))
                        .addOnFailureListener(e ->
                                Log.e(TAG, "Failed to send notification to: " + entrantID, e));
            });
        }
    }
}
