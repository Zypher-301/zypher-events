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
 * @author Elliot Chrystal
 * @author Arunavo Dutta
 * @version 3.0
 * This fragment displays all events available to the Entrant
 * and implements US 01.01.01 (Join Waitlist) and US 01.01.02 (Leave Waitlist).
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
     * Asynchronously loads all events from the Firestore database.
     * It fetches the list of all available events, clears the current local list,
     * populates it with the newly fetched data, and then notifies the RecyclerView adapter
     * to refresh the UI and display the updated list. If the database query fails,
     * it logs an error message.
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
     * Handles the click event for an item in the RecyclerView. Currently, it displays a Toast
     * message with the name of the clicked event. This can be expanded to show event details.
     *
     * @param event The {@link Event} object that was clicked.
     */
    @Override
    public void onItemClick(Event event) {
        Toast.makeText(getContext(), "Clicked on: " + event.getEventName(), Toast.LENGTH_SHORT).show();
    }

    /**
     * Handles the click event when an entrant chooses to join an event's waitlist.
     * This method performs a series of asynchronous operations:
     * 1. It calls the database to add the current entrant to the specified event's waitlist.
     * 2. Upon successful addition to the waitlist, it creates a simplified 'clean' version of the
     *    event object. This is done to prevent nested object saving issues in Firestore.
     * 3. It adds this clean event object to the current user's local list of registered events.
     * 4. It then updates the user's data in the database with this new event history.
     * 5. Finally, upon successful update of the user's profile, it refreshes the list of events
     *    to reflect the change in status (e.g., showing "Leave Waitlist" instead of "Join").
     *
     * Error handling is implemented at each step to log failures and provide feedback to the user
     * via Toasts.
     *
     * @param event The {@link Event} object that the user has chosen to join.
     */
    @Override
    public void onJoinClick(Event event) {
        Log.d(TAG, "Joining waitlist for: " + event.getEventName());

        // 1. Add entrant to the event's 'waitListEntrants' array
        db.addEntrantToWaitlist(String.valueOf(event.getUniqueEventID()), currentUser)
                .addOnSuccessListener(aVoid -> {

                    // 2. Create a "clean" version of the event to save to the user's profile.
                    // This prevents the nested object save failure.
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

                    // 3. Add the clean event to the user's local history
                    currentUser.addEventToRegisteredEventHistory(eventForHistory);

                    // 4. Save the updated user object
                    db.setUserData(currentUser.getHardwareID(), currentUser)
                            .addOnSuccessListener(aVoid1 -> {
                                Log.d(TAG, "User profile updated with new event.");
                                Toast.makeText(getContext(), "Joined waitlist!", Toast.LENGTH_SHORT).show();
                                loadEvents(); // Refresh
                            })
                            .addOnFailureListener(e -> {
                                // This is where the error was happening silently
                                Log.e(TAG, "Error saving user data after joining: ", e);
                                Toast.makeText(getContext(), "Error: Failed to save to your profile.", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error joining waitlist", e);
                    Toast.makeText(getContext(), "Error: Could not join waitlist.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Handles the "Leave" button click for an event. This method facilitates the process of
     * an Entrant leaving the waitlist of a specific event. It performs the following actions:
     * <p>
     * 1. Calls the database to remove the current user from the specified event's 'waitListEntrants' array.
     * 2. Upon successful removal from the event's waitlist, it removes the event from the user's
     *    local registered event history.
     * 3. Saves the updated user object back to the database, reflecting the removal of the event.
     * 4. Displays a Toast message to confirm success or failure to the user.
     * 5. Refreshes the list of events to update the UI.
     * <p>
     * If any step in the database operation fails, an error is logged and a corresponding
     * Toast message is shown to the user.
     *
     * @param event The {@link Event} object from which the user is leaving the waitlist.
     */
    @Override
    public void onLeaveClick(Event event) {
        Log.d(TAG, "Leaving waitlist for: " + event.getEventName());

        // 1. Remove entrant from the event's 'waitListEntrants' array
        db.removeEntrantFromWaitlist(String.valueOf(event.getUniqueEventID()), currentUser)
                .addOnSuccessListener(aVoid -> {

                    // 2. Create a "clean" event object to find and remove
                    Event eventToRemove = new Event();
                    eventToRemove.setUniqueEventID(event.getUniqueEventID());
                    // We use our new equals() method, which only checks ID.

                    // 3. Remove the event from the user's local history
                    currentUser.removeEventFromRegisteredEventHistory(eventToRemove);

                    // 4. Save the updated user object
                    db.setUserData(currentUser.getHardwareID(), currentUser)
                            .addOnSuccessListener(aVoid1 -> {
                                Log.d(TAG, "User profile updated, event removed.");
                                Toast.makeText(getContext(), "Left waitlist.", Toast.LENGTH_SHORT).show();
                                loadEvents(); // Refresh
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error saving user data after leaving: ", e);
                                Toast.makeText(getContext(), "Error: Failed to update your profile.", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error leaving waitlist", e);
                    Toast.makeText(getContext(), "Error: Could not leave waitlist.", Toast.LENGTH_SHORT).show();
                });
    }
}