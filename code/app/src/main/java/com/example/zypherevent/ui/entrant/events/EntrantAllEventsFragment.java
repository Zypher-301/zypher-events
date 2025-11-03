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

public class EntrantAllEventsFragment extends Fragment implements EntrantEventAdapter.OnItemClickListener {

    private static final String TAG = "EntrantAllEvents";

    private Database db;
    private RecyclerView recyclerView;
    private EntrantEventAdapter adapter;
    private List<Event> eventList = new ArrayList<>();
    private Button refreshButton;
    private Entrant currentUser;

    public EntrantAllEventsFragment() { }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
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

    @Override
    public void onItemClick(Event event) {
        Toast.makeText(getContext(), "Clicked on: " + event.getEventName(), Toast.LENGTH_SHORT).show();
    }

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