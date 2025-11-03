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
 * @author Elliot Chrystal (Updated by Gemini)
 * @version 3.0
 * This fragment displays the events the Entrant has joined
 * and implements US 01.01.02 (Leave Waitlist).
 */
public class EntrantJoinedEventsFragment extends Fragment {

    private static final String TAG = "EntrantJoinedEvents";

    private Database db;
    private RecyclerView recyclerView;
    private JoinedEventAdapter adapter;
    private List<Event> eventList = new ArrayList<>();
    private Button refreshButton;
    private Entrant currentUser;

    public EntrantJoinedEventsFragment() { }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Use the generic list page layout
        return inflater.inflate(R.layout.fragment_entrant_fragment_list_page, container, false);
    }

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

        // Find views
        recyclerView = view.findViewById(R.id.recycler_view);
        refreshButton = view.findViewById(R.id.refresh_button);

        // Setup the adapter with our "Leave" click listener
        adapter = new JoinedEventAdapter(eventList, event -> {
            // This is the implementation of onLeaveClick (US 01.01.02)
            Log.d(TAG, "Leaving waitlist for: " + event.getEventName());

            // 1. Remove entrant from the event's 'waitListEntrants'
            db.removeEntrantFromWaitlist(String.valueOf(event.getUniqueEventID()), currentUser)
                    .addOnSuccessListener(aVoid -> {

                        // 2. ALSO remove the event from the user's local 'registeredEventHistory'
                        currentUser.removeEventFromRegisteredEventHistory(event);
                        // 3. Save the updated user object back to Firebase
                        db.setUserData(currentUser.getHardwareID(), currentUser);

                        Toast.makeText(getContext(), "Left waitlist.", Toast.LENGTH_SHORT).show();
                        loadJoinedEvents(); // Refresh the list
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error leaving waitlist", e);
                        Toast.makeText(getContext(), "Error: Could not leave waitlist.", Toast.LENGTH_SHORT).show();
                    });
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        refreshButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Refreshing list...", Toast.LENGTH_SHORT).show();
            loadJoinedEvents();
        });

        loadJoinedEvents();
    }

    /**
     * Fetches the events this Entrant has joined FROM THE LOCAL USER OBJECT.
     */
    private void loadJoinedEvents() {
        Log.d(TAG, "Loading joined events from local user object...");

        List<Event> joinedEvents = currentUser.getRegisteredEventHistory();

        eventList.clear();

        if (joinedEvents != null && !joinedEvents.isEmpty()) {
            eventList.addAll(joinedEvents);
        } else {
            Log.d(TAG, "User has not joined any events.");
            Toast.makeText(getContext(), "You have not joined any events.", Toast.LENGTH_SHORT).show();
        }

        adapter.notifyDataSetChanged();
    }
}