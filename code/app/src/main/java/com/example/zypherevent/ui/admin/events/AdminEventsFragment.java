package com.example.zypherevent.ui.admin.events;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.app.AlertDialog;

import com.example.zypherevent.Database;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.example.zypherevent.Event;
import com.example.zypherevent.R;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * A fragment for administrators to browse and manage all events in the application.
 *
 * <p>
 * This class extends {@link AdminBaseListFragment} to provide a user interface
 * for displaying a list of all events stored in the Firestore database. Administrators
 * can view the list of events and have the ability to delete any event from the system.
 * The fragment fetches event data from the "events" collection in Firestore and populates
 * a RecyclerView using the {@link AdminEventsAdapter}.
 * </p>
 *
 * <p>
 * Functionality includes:
 * <ul>
 *     <li>Displaying a list of all events.</li>
 *     <li>Deleting an event after a confirmation dialog.</li>
 *     <li>A refresh button to manually reload the list of events from the database.</li>
 * </ul>
 * </p>
 *
 * <p>
 * This class fulfills the following user stories:
 * <ul>
 *     <li><b>US 03.01.01:</b> As an administrator, I want to be able to remove events.</li>
 *     <li><b>US 03.04.01:</b> As an administrator, I want to be able to browse events.</li>
 * </ul>
 * </p>
 *
 * @see AdminBaseListFragment
 * @see AdminEventsAdapter
 * @see Event
 * @see FirebaseFirestore
 * @see res/layout/fragment_admin_events.xml
 * @author Arunavo Dutta
 * @version 4.0
 */
public class AdminEventsFragment extends AdminBaseListFragment {

    private static final String TAG = "AdminEventsFragment";
    private FirebaseFirestore firestoreDb; // <-- Renamed for clarity
    private Database db;                  // <-- Add this instance of your custom class
    private AdminEventsAdapter adapter;
    private List<Event> eventList = new ArrayList<>();
    private Button refreshButton;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firestoreDb = FirebaseFirestore.getInstance(); // <-- Initialize the raw instance
        db = new Database();                          // <-- Initialize your custom class

        adapter = new AdminEventsAdapter(eventList, event -> {
            handleDeleteEvent(event);
        });

        recyclerView.setAdapter(adapter);

        // --- REFRESH BUTTON LOGIC ---
        refreshButton = view.findViewById(R.id.refresh_button);
        refreshButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Refreshing list...", Toast.LENGTH_SHORT).show();
            loadEvents();
        });
        // --- REFRESH BUTTON LOGIC ---

        loadEvents();
    }



    private void loadEvents() {
        Log.d(TAG, "Attempting to query 'events' collection using db.getAllEventsList()...");

        // Now this will work, because 'db' is your custom Database class
        db.getAllEventsList().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<Event> fetchedEvents = task.getResult();
                if (fetchedEvents == null) {
                    Log.e(TAG, "Query successful but fetchedEvents is null!");
                    return;
                }

                eventList.clear();
                eventList.addAll(fetchedEvents);
                adapter.notifyDataSetChanged();
                Log.d(TAG, "Successfully fetched and converted " + eventList.size() + " events.");

            } else {
                // The task failed
                Log.e(TAG, "Error running query: ", task.getException());
                Toast.makeText(getContext(), "Error fetching events", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Handles the deletion of a specific event after user confirmation.
     * <p>
     * This method first presents an {@link AlertDialog} to the user to confirm the deletion.
     * If the user confirms, it proceeds to find and delete the event from the Firestore "events"
     * collection. The search is based on the {@code uniqueEventID} of the provided {@link Event} object.
     * <p>
     * Upon successful deletion from Firestore, the event is also removed from the local
     * {@code eventList}, and the adapter is notified to refresh the RecyclerView. A success
     * message is shown to the user. If the deletion fails or the event document cannot be
     * found, an error message is logged and displayed via a {@link Toast}. If the user
     * cancels the dialog, no action is taken.
     *
     * @param event The {@link Event} object to be deleted. It must not be null and should
     * contain a valid {@code uniqueEventID}.
     */
    private void handleDeleteEvent(Event event) {
        if (event == null || event.getUniqueEventID() == null) {
            Toast.makeText(getContext(), "Error: Event has no ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ask for confirmation before deletion
        new AlertDialog.Builder(getContext())
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete '" + event.getEventName() + "'? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // User clicked "Delete". Proceed with the original deletion logic.

                    Toast.makeText(getContext(), "Deleting " + event.getEventName(), Toast.LENGTH_SHORT).show();

                    // --- FIX: Use 'firestoreDb' variable ---
                    firestoreDb.collection("events")
                            .whereEqualTo("uniqueEventID", event.getUniqueEventID())
                            .get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                    String documentId = task.getResult().getDocuments().get(0).getId();

                                    // --- FIX: Use 'firestoreDb' variable ---
                                    firestoreDb.collection("events").document(documentId).delete()
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d(TAG, "Successfully deleted event: " + event.getEventName());
                                                eventList.remove(event);
                                                adapter.notifyDataSetChanged();

                                                // --- Start: Show success message ---
                                                Toast.makeText(getContext(), "Event deleted successfully", Toast.LENGTH_SHORT).show();
                                                // --- End ---

                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "Failed to delete event", e);
                                                Toast.makeText(getContext(), "Failed to delete event", Toast.LENGTH_SHORT).show();
                                            });
                                } else {
                                    Log.e(TAG, "Could not find document to delete with uniqueEventID: " + event.getUniqueEventID());
                                }
                            });
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // User clicked "Cancel", so dismiss the dialog.
                    dialog.dismiss();
                })
                .show(); // Display the confirmation dialog
        // ---  End ---
    }
}