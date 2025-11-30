package com.example.zypherevent.ui.entrant.events;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.zypherevent.Database;
import com.example.zypherevent.EntrantActivity;
import com.example.zypherevent.Event;
import com.example.zypherevent.R;
import com.example.zypherevent.userTypes.Entrant;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Elliot Chrystal
 * @author Tom Yang (added functionality to display entrant's joined event history)
 *
 * A fragment that displays a comprehensive list of joined events to an Entrant.
 * It provides the functionality for users to join or leave the waitlist for an event.
 *
 * @version 1.0
 */
public class EntrantJoinedEventsFragment extends Fragment implements EntrantEventAdapter.OnItemClickListener {
    private static final String TAG = "EntrantJoinedEvents";
    private RecyclerView recyclerView;
    private EntrantEventAdapter adapter;
    private Entrant currentUser;
    private List<Event> eventList = new ArrayList<>();

    public EntrantJoinedEventsFragment() {
        // public no-arg constructor required
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entrant_joined_events, container, false);
    }

    /**
     * Called immediately after {@link #onCreateView} has returned, but before any saved state
     * has been restored in to the view.
     * <p>
     * This method initializes the fragment's view components, sets up the RecyclerView with
     * the adapter, and loads the initial event data.
     *
     * @param view The View returned by {@link #onCreateView}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get the current user from activity
        EntrantActivity activity = (EntrantActivity) requireActivity();
        currentUser = activity.getEntrantUser();

        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.recycler_view);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Create adapter
        adapter = new EntrantEventAdapter(eventList, currentUser, this);
        recyclerView.setAdapter(adapter);

        // Load initial data
        loadEvents();
    }

    /**
     * Loads the joined events from the current user's registered event history.
     * <p>
     * This method retrieves the list of events from the user's registered event history,
     * updates the local event list, and notifies the adapter to refresh the UI.
     */
    private void loadEvents() {
        Log.d(TAG, "Loading joined events for user: " + currentUser.getHardwareID());

        // Get registered events from user
        ArrayList<Long> registeredEventIDs = currentUser.getRegisteredEventHistory();

        Log.d(TAG, "Registered event IDs: " + registeredEventIDs);

        Database db = new Database();

        db.getEventsByIds(registeredEventIDs).addOnCompleteListener(t -> {
            if (t.isSuccessful()) {
                List<Event> registeredEvents = t.getResult();

                // Clear and update the event list
                eventList.clear();

                if (registeredEvents != null && !registeredEvents.isEmpty()) {
                    eventList.addAll(registeredEvents);
                    Log.d(TAG, "Successfully loaded " + eventList.size() + " joined events.");
                } else {
                    Log.d(TAG, "No joined events found.");
                }

                // Notify adapter of data change
                adapter.notifyDataSetChanged();
            }
        });
    }

    /**
     * Handles the "Join Waitlist" action for the given event.
     *
     * @param event the event for which the user is attempting to join the waitlist
     */
    @Override
    public void onJoinClick(Event event) {
        Log.d(TAG, "Joining waitlist for: " + event.getEventName());

        if (event.getRequiresGeolocation() && !currentUser.getUseGeolocation()) {
            Toast.makeText(getContext(),
                    "Geolocation is required for this event. Please enable it in your settings.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Event.WaitlistOperationResult result =
                event.joinWaitlist(currentUser.getHardwareID());

        switch (result) {
            case ALREADY_INVITED:
                Toast.makeText(getContext(),
                        "You are already invited to this event.", Toast.LENGTH_SHORT).show();
                return;
            case ALREADY_ACCEPTED:
                Toast.makeText(getContext(),
                        "You have already accepted this event.", Toast.LENGTH_SHORT).show();
                return;
            case ALREADY_DECLINED:
                Toast.makeText(getContext(),
                        "You have already declined this event.", Toast.LENGTH_SHORT).show();
                return;
            case ALREADY_ON_WAITLIST:
                Toast.makeText(getContext(),
                        "You are already on the waitlist.", Toast.LENGTH_SHORT).show();
                return;
            case REGISTRATION_NOT_STARTED:
                Toast.makeText(getContext(),
                        "Registration has not started yet.", Toast.LENGTH_SHORT).show();
                return;
            case REGISTRATION_CLOSED:
                Toast.makeText(getContext(),
                        "Registration has closed.", Toast.LENGTH_SHORT).show();
                return;
            case WAITLIST_FULL:
                Toast.makeText(getContext(),
                        "Waitlist is full.", Toast.LENGTH_SHORT).show();
                return;
            case SUCCESS:
                break;
        }

        Database db = new Database();
        db.addEntrantToWaitlist(String.valueOf(event.getUniqueEventID()), currentUser)
                .addOnSuccessListener(aVoid -> {
                    currentUser.addEventToRegisteredEventHistory(event.getUniqueEventID());
                    db.setUserData(currentUser.getHardwareID(), currentUser)
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(getContext(),
                                        "Joined waitlist!", Toast.LENGTH_SHORT).show();
                                loadEvents();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error saving user data after joining: ", e);
                                Toast.makeText(getContext(),
                                        "Error saving to profile: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error joining waitlist", e);
                    Toast.makeText(getContext(),
                            e.getMessage() != null ? e.getMessage() : "Failed to join waitlist",
                            Toast.LENGTH_SHORT).show();
                });
    }


    /**
     * Handles the "Leave Waitlist" action for the given event.
     *
     * @param event the event for which the user is attempting to leave the waitlist
     */
    @Override
    public void onLeaveClick(Event event) {
        Log.d(TAG, "Leaving waitlist for: " + event.getEventName());

        Event.WaitlistOperationResult result =
                event.leaveWaitlist(currentUser.getHardwareID());

        if (result == Event.WaitlistOperationResult.NOT_ON_WAITLIST) {
            Toast.makeText(getContext(),
                    "You are not on the waitlist for this event.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Local user history update
        currentUser.removeEventFromRegisteredEventHistory(event.getUniqueEventID());
        adapter.notifyDataSetChanged();

        Database db = new Database();
        db.removeEntrantFromWaitlist(String.valueOf(event.getUniqueEventID()), currentUser)
                .addOnSuccessListener(aVoid -> {
                    db.setUserData(currentUser.getHardwareID(), currentUser)
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(getContext(),
                                        "Left waitlist.", Toast.LENGTH_SHORT).show();
                                loadEvents();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error saving user data after leaving: ", e);
                                Toast.makeText(getContext(),
                                        "Error updating profile: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error leaving waitlist", e);
                    Toast.makeText(getContext(),
                            "Error leaving waitlist: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Starts the EntrantEventDetailsFragment when an event is clicked.
     */
    @Override
    public void onItemClick(Event event) {
        if (event == null) return;

        Bundle args = new Bundle();
        args.putSerializable(EntrantEventDetailsFragment.ARG_EVENT, event);
        args.putString(EntrantEventDetailsFragment.ARG_ENTRANT_HARDWARE_ID, currentUser.getHardwareID());

        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.nav_entrant_event_details, args);
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * Refreshes the event list when returning to this fragment.
     */
    @Override
    public void onResume() {
        super.onResume();
        loadEvents();
    }
}