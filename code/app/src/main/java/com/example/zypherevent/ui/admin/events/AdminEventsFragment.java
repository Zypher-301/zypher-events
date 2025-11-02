package com.example.zypherevent.ui.admin.events; // Use your actual package name

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button; // Import Button
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.example.zypherevent.Event;
import com.example.zypherevent.R; // Make sure R is imported
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap; // Import HashMap
import java.util.Map; // Import Map

/**
 * @author Arunavo Dutta
 * @version 4.0
 * @See AdminBaseListFragment
 * @See Event
 * @See AdminEventsAdapter
 * @See FirebaseFirestore
 * @see res/layout/fragment_admin_events.xml
 * Completes US 03.01.01 As an administrator, I want to be able to remove events.
 * Completes US 03.04.01 As an administrator, I want to be able to browse events.
 */
public class AdminEventsFragment extends AdminBaseListFragment {

    private static final String TAG = "AdminEventsFragment";
    private FirebaseFirestore db;
    private AdminEventsAdapter adapter;
    private List<Event> eventList = new ArrayList<>();
    private Button refreshButton;

    /**
     * Called immediately after {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, Bundle)}
     * has returned, but before any saved state has been restored in to the view.
     * <p>
     * This method initializes the fragment's view components. It sets up the Firestore instance,
     * creates and configures the {@link AdminEventsAdapter} for the RecyclerView, and sets up
     * a click listener for the delete functionality on each list item. It also initializes
     * the refresh button with a click listener to reload the event data. Finally, it
     * triggers an initial load of events from Firestore by calling {@link #loadEvents()}.
     *
     * @param view The View returned by {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @see #loadEvents()
     * @see #handleDeleteEvent(Event)
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

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


    /**
     * Fetches all events from the Firestore "events" collection and updates the RecyclerView.
     * <p>
     * This method initiates an asynchronous query to the Firestore database to retrieve
     * all documents from the "events" collection. On successful completion, it attempts
     * to automatically map the retrieved documents to a list of {@link Event} objects.
     * <p>
     * The existing local {@code eventList} is cleared and then populated with the newly
     * fetched data. Finally, it notifies the {@code adapter} that the dataset has changed,
     * which triggers a refresh of the UI to display the latest events.
     * <p>
     * If the query fails (e.g., due to network issues or security rule violations), an
     * error is logged, and a toast message is displayed to the user. It also handles
     * the case where the query is successful but returns a null result.
     *
     * @see FirebaseFirestore#collection(String)
     * @see com.google.android.gms.tasks.OnCompleteListener
     * @see QuerySnapshot#toObjects(Class)
     * @see AdminEventsAdapter#notifyDataSetChanged()
     */
    private void loadEvents() {
        // Defining the name of the collection we want to read
        String collectionName = "events";

        Log.d(TAG, "Attempting to query 'events' collection...");

        // Start the asynchronous call to get the data from Firestore
        // We are going to be using db.collection() directly, not from the Database class
        db.collection(collectionName).get().addOnCompleteListener(task -> {

            // The task is the asynchronous job. Check if it was successful.
            if (task.isSuccessful()) {
                // Task is successful then we get the list of results.
                com.google.firebase.firestore.QuerySnapshot snapshot = task.getResult();

                // Check if the result is null
                if (snapshot == null) {
                    Log.e(TAG, "Query successful but snapshot is null!");
                    return; // Exit the function
                }

                // Firestore automatically converts all documents into Event objects and puts them in a List.
                List<Event> fetchedEvents = snapshot.toObjects(Event.class);

                // Update our local list

                // 1. Clear the old list of events
                eventList.clear();
                // 2. Add all the new events we just fetched
                eventList.addAll(fetchedEvents);

                // 3. Tell the adapter that the data has changed, so that it updates the UI
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
     * Handles the deletion of an event from the Firestore database.
     * <p>
     * This method first queries the "events" collection to find the document
     * that matches the {@code uniqueEventID} of the given {@code Event} object.
     * If a matching document is found, it proceeds to delete it.
     * <p>
     * Upon successful deletion from Firestore, the event is also removed from the
     * local {@code eventList} and the RecyclerView's adapter is notified to update the UI.
     * Toasts are displayed to provide user feedback on the deletion process.
     * Error messages are logged if the event cannot be found or if the deletion fails.
     *
     * @param event The {@link Event} object to be deleted. The object must not be null
     *              and must have a valid {@code uniqueEventID}.
     */
    private void handleDeleteEvent(Event event) {
        if (event == null || event.getUniqueEventID() == null) {
            Toast.makeText(getContext(), "Error: Event has no ID", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getContext(), "Deleting " + event.getEventName(), Toast.LENGTH_SHORT).show();

        db.collection("events")
                .whereEqualTo("uniqueEventID", event.getUniqueEventID())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        String documentId = task.getResult().getDocuments().get(0).getId();
                        db.collection("events").document(documentId).delete()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Successfully deleted event: " + event.getEventName());
                                    eventList.remove(event);
                                    adapter.notifyDataSetChanged();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to delete event", e);
                                    Toast.makeText(getContext(), "Failed to delete event", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Log.e(TAG, "Could not find document to delete with uniqueEventID: " + event.getUniqueEventID());
                    }
                });
    }
}