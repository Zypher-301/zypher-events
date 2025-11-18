package com.example.zypherevent.ui.entrant.events;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
 * @author Tom Yang (added functionality to display entrant's joined event history)
 *
 * @version 1.0
 */
public class EntrantJoinedEventsFragment extends Fragment {
    private static final String TAG = "EntrantJoinedEvents";
    private RecyclerView recyclerView;
    private EntrantJoinedEventsAdapter adapter;
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
        adapter = new EntrantJoinedEventsAdapter(eventList, currentUser);
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
     * Called when the fragment is visible to the user and actively running.
     * Refreshes the event list when returning to this fragment.
     */
    @Override
    public void onResume() {
        super.onResume();
        loadEvents();
    }
}