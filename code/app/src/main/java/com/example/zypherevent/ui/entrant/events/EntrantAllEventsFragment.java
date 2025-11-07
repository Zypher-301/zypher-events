package com.example.zypherevent.ui.entrant.events;


import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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
 * BUG: DOUBLE CLICKING NEEDED FOR LEAVING THE WAITLIST
 * A fragment that displays a comprehensive list of all available events to an Entrant.
 * It provides the functionality for users to join or leave the waitlist for an event.
 * This class is responsible for fetching event data from the database, displaying it in a
 * RecyclerView, and handling user interactions such as joining, leaving, and refreshing the event list.
 * <p>
 * This class implements the following user stories:
 * <ul>
 * <li>US 01.01.01: As an Entrant, I want to join the waitlist for an event.</li>
 * <li>US 01.01.02: As an Entrant, I want to leave the waitlist for an event.</li>
 * </ul>
 *
 * @author Elliot Chrystal
 * @author Arunavo Dutta
 * @version 3.0
 */

public class EntrantAllEventsFragment extends Fragment implements EntrantEventAdapter.OnItemClickListener {

    private static final String TAG = "EntrantAllEvents";

    private Database db;
    private RecyclerView recyclerView;
    private EntrantEventAdapter adapter;
    private List<Event> eventList = new ArrayList<>();
    private Button refreshButton;
    private Entrant currentUser;

    public EntrantAllEventsFragment() { }

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is optional, and non-graphical fragments can return null. This will be called between
     * {@link #onCreate(Bundle)} and {@link #onViewCreated(View, Bundle)}.
     * <p>A default View can be returned by calling Fragment in your
     * constructor. If you return a View from here, you will later be called in
     * {@link #onDestroyView} when the view is being released.
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return Return the View for the fragment's UI, or null.
     */
    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entrant_fragment_list_page, container, false);
    }

    /**
     * Added by Arunavo Dutta
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * has returned, but before any saved state has been restored in to the view.
     * This gives subclasses a chance to initialize themselves once
     * they know their view hierarchy has been completely created.
     * <p>
     * This method initializes the RecyclerView, its adapter, and the refresh button.
     * It also retrieves the current Entrant user from the activity and sets up a listener
     * for the refresh button to reload the list of events. The initial load of events
     * is also triggered here.
     *
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = new Database();
        if (getActivity() instanceof EntrantActivity) {
            currentUser = ((EntrantActivity) getActivity()).getEntrantUser();
        }
        if (currentUser == null) {
            Log.e(TAG, "Current Entrant user is NULL.");
            return;
        }

        recyclerView = view.findViewById(R.id.recycler_view);
        refreshButton = view.findViewById(R.id.refresh_button);
        adapter = new EntrantEventAdapter(eventList, currentUser, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        refreshButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Refreshing list...", Toast.LENGTH_SHORT).show();
            loadEvents();
        });
        loadEvents();
    }

    /**
     * Added by Arunavo Dutta
     * Asynchronously loads all events from the Firestore database.
     * <p>
     * This method fetches the complete list of available events from the database.
     * It first clears the local {@code eventList}, then populates it with the newly fetched data.
     * Finally, it notifies the {@link EntrantEventAdapter} that the data set has changed,
     * prompting the {@link RecyclerView} to refresh and display the updated list of events.
     * If the database query fails, an error is logged.
     */
    private void loadEvents() {
        Log.d(TAG, "Attempting to query 'events' collection...");
        db.getAllEventsList().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<Event> fetchedEvents = task.getResult();
                if (fetchedEvents == null) { return; }
                Log.d(TAG, "Firebase query successful. Found " + fetchedEvents.size() + " events.");
                eventList.clear();
                eventList.addAll(fetchedEvents);
                adapter.notifyDataSetChanged();
            } else {
                Log.e(TAG, "Error running query: ", task.getException());
            }
        });
    }

    /**
     * Added by Arunoavo Dutta
     * Handles the click event for an item in the RecyclerView.
     * This is triggered when a user taps on an event card.
     * Currently, it displays a Toast message with the name of the clicked event.
     * <p>
     * This method can be expanded in the future to navigate to a detailed view of the event.
     *
     * @param event The {@link Event} object corresponding to the clicked item.
     */
    // Can be used to show event details in future
    @Override
    public void onItemClick(Event event) {
        Toast.makeText(getContext(), "Clicked on: " + event.getEventName(), Toast.LENGTH_SHORT).show();
    }

    /**
     * Added by Arunavo Dutta
     * Handles the click event for joining an event's waitlist.
     * <p>
     * This method orchestrates the process of an entrant joining an event. It performs a sequence
     * of asynchronous database operations:
     * <ol>
     * <li>Adds the current user to the specified event's waitlist in the database.</li>
     * <li>Upon success, creates a simplified "clean" version of the event object to avoid
     * nested data issues in Firestore when saving to the user's profile.</li>
     * <li>Adds this clean event to the user's local list of registered events.</li>
     * <li>Saves the updated user object (with the new event history) back to the database.</li>
     * <li>Finally, refreshes the event list to update the UI, typically changing the
     * "Join" button to a "Leave" button.</li>
     * </ol>
     * Error handling is implemented at each step to log failures and provide feedback to the user
     * via a {@link Toast}.
     *
     * @param event The {@link Event} object that the user has chosen to join.
     */
    @Override
    public void onJoinClick(Event event) {
        Log.d(TAG, "Joining waitlist for: " + event.getEventName());

        db.addEntrantToWaitlist(String.valueOf(event.getUniqueEventID()), currentUser)
                .addOnSuccessListener(aVoid -> {

                    Event eventForHistory = new Event(
                            event.getUniqueEventID(),
                            event.getEventName(),
                            event.getEventDescription(),
                            event.getStartTime(),
                            event.getLocation(),
                            event.getRegistrationStartTime(),
                            event.getRegistrationEndTime(),
                            event.getEventOrganizerHardwareID(),
                            event.getPosterURL()
                    );

                    currentUser.addEventToRegisteredEventHistory(eventForHistory);

                    db.setUserData(currentUser.getHardwareID(), currentUser)
                            .addOnSuccessListener(aVoid1 -> {
                                Log.d(TAG, "User profile updated with new event.");
                                Toast.makeText(getContext(), "Joined waitlist!", Toast.LENGTH_SHORT).show();
                                loadEvents(); // Refresh
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error saving user data after joining: ", e);
                                Toast.makeText(getContext(), "Error saving to profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error joining waitlist", e);
                    String message = e.getMessage();
                    if (message != null && !message.isEmpty()) {
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Failed to join waitlist", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Added by Arunavo Dutta
     * BUG: DOUBLE CLICKING NEEDED FOR LEAVING THE WAITLIST
     * Handles the "Leave Waitlist" button click for an event.
     * <p>
     * This method orchestrates the process for an entrant to leave the waitlist of a specific event.
     * It performs the following sequential, asynchronous operations:
     * <ol>
     * <li>Calls the database to remove the current user from the specified event's waitlist.</li>
     * <li>Upon successful removal from the event's waitlist, it removes the corresponding event from the user's
     * local list of registered events.</li>
     * <li>Saves the updated user object back to the database to persist the change in their event history.</li>
     * <li>Refreshes the list of all events to update the UI, which will now show the option to "Join" the waitlist again for that event.</li>
     * </ol>
     * Each step includes error handling. If a database operation fails, an error is logged, and a
     * {@link Toast} message is shown to the user to inform them of the failure. A success message is shown upon completion.
     *
     * @param event The {@link Event} object from which the user is leaving the waitlist.
     */
    // BUG: DOUBLE CLICKING NEEDED FOR LEAVING THE WAITLIST
    @Override
    public void onLeaveClick(Event event) {
        Log.d(TAG, "Leaving waitlist for: " + event.getEventName());

        db.removeEntrantFromWaitlist(String.valueOf(event.getUniqueEventID()), currentUser)
                .addOnSuccessListener(aVoid -> {

                    Event eventToRemove = new Event();
                    eventToRemove.setUniqueEventID(event.getUniqueEventID());

                    currentUser.removeEventFromRegisteredEventHistory(eventToRemove);

                    db.setUserData(currentUser.getHardwareID(), currentUser)
                            .addOnSuccessListener(aVoid1 -> {
                                Log.d(TAG, "User profile updated, event removed.");
                                Toast.makeText(getContext(), "Left waitlist.", Toast.LENGTH_SHORT).show();
                                loadEvents(); // Refresh
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error saving user data after leaving: ", e);
                                Toast.makeText(getContext(), "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error leaving waitlist", e);
                    Toast.makeText(getContext(), "Error leaving waitlist: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
