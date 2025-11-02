package com.example.zypherevent.ui.admin.events; // Use your actual package name

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.zypherevent.Database;
import com.example.zypherevent.Event;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Arunavo Dutta
 * @version 2.0
 * @see AdminBaseListFragment
 * @see AdminEventsAdapter
 * @see Database
 * @see Event
 *
 * Fulfills US 03.04.01: As an administrator, I want to be able to browse events.
 * Fulfills US 03.01.01: As an administrator, I want to be able to remove events.
 */
public class AdminEventsFragment extends AdminBaseListFragment {

    private static final String TAG = "AdminEventsFragment";
    private Database db;
    private AdminEventsAdapter adapter;
    private List<Event> eventList = new ArrayList<>(); // <-- USING REAL EVENT

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = new Database(); // Initialize database

        // Set up the adapter with an empty list first
        // The 'event' in the lambda is now the REAL Event type
        adapter = new AdminEventsAdapter(eventList, event -> {
            // This is the delete logic (US 03.01.01)
            handleDeleteEvent(event); // <-- This will now work
        });

        recyclerView.setAdapter(adapter);

        // Fetch the events from Firebase
        loadEvents();
    }

    private void loadEvents() {
        db.getAllEvents().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                eventList.clear();
                List<Event> fetchedEvents = task.getResult();
                if (fetchedEvents != null) {
                    eventList.addAll(fetchedEvents);
                }
                adapter.notifyDataSetChanged();
                Log.d(TAG, "Successfully fetched " + eventList.size() + " events.");
            } else {
                Log.e(TAG, "Error fetching events: ", task.getException());
                Toast.makeText(getContext(), "Error fetching events", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // This method correctly expects the REAL Event model
    private void handleDeleteEvent(Event event) {
        if (event == null || event.getUniqueEventID() == null) {
            Toast.makeText(getContext(), "Error: Event has no ID", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getContext(), "Deleting " + event.getEventName(), Toast.LENGTH_SHORT).show();

        db.removeEventData(event.getUniqueEventID()).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Successfully deleted event: " + event.getEventName());
                eventList.remove(event);
                adapter.notifyDataSetChanged();
            } else {
                Log.e(TAG, "Error deleting event: ", task.getException());
                Toast.makeText(getContext(), "Failed to delete event", Toast.LENGTH_SHORT).show();
            }
        });
    }
}