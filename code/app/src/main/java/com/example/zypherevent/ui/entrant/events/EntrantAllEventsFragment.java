package com.example.zypherevent.ui.entrant.events;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zypherevent.R;
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
import com.example.zypherevent.Event;
import com.example.zypherevent.R;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Elliot Chrystal
 * @author Arunavo Dutta
 * @version 1.0
 * This fragment displays a list of ALL available events.
 * It uses the EntrantEventAdapter to show the events in a list.
 * Fulfils Entrant Story: US 01.05.04
 */
public class EntrantAllEventsFragment extends Fragment {

    private static final String TAG = "EntrantAllEvents";

    private Database db;
    private RecyclerView recyclerView;
    private EntrantEventAdapter adapter;
    private List<Event> eventList = new ArrayList<>();
    private Button refreshButton;

    public EntrantAllEventsFragment() {
        // public no-arg constructor required
    }

    /**
     * Called to have the fragment instantiate its user interface view. This is optional, and non-graphical
     * fragments can return null. This will be called between onCreate(Bundle) and onActivityCreated(Bundle).
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     *                  The fragment should not add the view itself, but this can be used to generate the
     *                  LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous
     *                           saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout that contains the RecyclerView and Refresh button
        return inflater.inflate(R.layout.fragment_entrant_fragment_list_page, container, false);
    }

    /**
     * Added by Arunavo Dutta
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * has returned, but before any saved state has been restored in to the view.
     * This gives subclasses a chance to initialize themselves once
     * they know their view hierarchy has been completely created.
     *
     * This method initializes the database, finds the necessary UI components (RecyclerView, Button),
     * sets up the {@link EntrantEventAdapter} for the RecyclerView, defines the click listener for the
     * refresh button, and triggers the initial loading of events from the database.
     *
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Database
        db = new Database();

        // Find views
        recyclerView = view.findViewById(R.id.recycler_view);
        refreshButton = view.findViewById(R.id.refresh_button);

        // Setup the adapter and click listener
        adapter = new EntrantEventAdapter(eventList, event -> {
            Toast.makeText(getContext(), "Clicked on: " + event.getEventName(), Toast.LENGTH_SHORT).show();
        });

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Refresh button listener
        refreshButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Refreshing list...", Toast.LENGTH_SHORT).show();
            loadEvents();
        });

        // Load events for the first time
        loadEvents();
    }

    /**
     * Added by Arunavo Dutta
     * Fetches all events from the database and updates the RecyclerView.
     * It retrieves the list of events using the {@code getAllEventsList} method from the {@link Database} class.
     * On successful retrieval, it clears the current event list, adds the newly fetched events,
     * and notifies the adapter to refresh the view. If the fetch fails, it logs an error
     * and displays a toast message to the user.
     */

    private void loadEvents() {
        Log.d(TAG, "Attempting to query 'events' collection...");

        // Call the new method that returns a List<Event>
        db.getAllEventsList().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {

                List<Event> fetchedEvents = task.getResult();

                if (fetchedEvents == null) {
                    Log.e(TAG, "Failed to get event list (task result was null).");
                    return;
                }

                Log.d(TAG, "Firebase query successful. Found " + fetchedEvents.size() + " events.");

                eventList.clear();
                eventList.addAll(fetchedEvents);
                adapter.notifyDataSetChanged();

            } else {
                Log.e(TAG, "Error running query: ", task.getException());
                Toast.makeText(getContext(), "Error fetching events", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
