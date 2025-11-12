package com.example.zypherevent.ui.admin.events;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.zypherevent.Database;
import com.example.zypherevent.Event;
import com.example.zypherevent.R;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A fragment for administrators to browse and remove event posters.
 *
 * <p>
 * This class provides the user interface and logic for fulfilling the following user stories:
 * <ul>
 * <li><b>US 03.06.01:</b> As an administrator, I want to be able to browse images that are
 * uploaded so I can remove them if necessary.</li>
 * <li><b>US 03.03.01:</b> As an administrator, I want to be able to remove images.</li>
 * </ul>
 * </p>
 *
 * <p>
 * This fragment extends {@link AdminBaseListFragment} and uses {@link AdminImagesAdapter}
 * to display a list of all events that have a non-null {@code posterURL}.
 * The "remove" functionality is implemented by setting the event's {@code posterURL} field
 * to {@code null} in the Firestore database, which effectively removes the image from the app.
 * </p>
 *
 * @author Arunavo Dutta (Refactored)
 * @version 2.0
 * @see AdminBaseListFragment
 * @see AdminImagesAdapter
 * @see Event
 * @see Database
 */
public class AdminImagesFragment extends AdminBaseListFragment implements AdminImagesAdapter.OnDeleteListener {

    private static final String TAG = "AdminImagesFragment";
    private AdminImagesAdapter adapter;
    private Database database;
    private List<Event> eventListWithPosters = new ArrayList<>();
    private Button refreshButton;

    /**
     * Called when the fragment's view has been created.
     * <p>
     * Initializes the database, sets up the RecyclerView with the {@link AdminImagesAdapter},
     * configures the refresh button, and triggers the initial loading of images.
     *
     * @param view The View returned by {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState); // Sets up recyclerView

        database = new Database();

        // Initialize Adapter with the empty list and set this fragment as the delete listener
        adapter = new AdminImagesAdapter(eventListWithPosters, this);
        recyclerView.setAdapter(adapter);

        // --- REFRESH BUTTON LOGIC ---
        refreshButton = view.findViewById(R.id.refresh_button);
        refreshButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Refreshing images...", Toast.LENGTH_SHORT).show();
            loadImages();
        });
        // --- END REFRESH LOGIC ---

        // Load the images for the first time
        loadImages();
    }

    /**
     * Fetches all events from the database and filters them to show only those with posters.
     * This fulfills <b>US 03.06.01 (Browse images)</b>.
     * <p>
     * This method adds a safety check to prevent crashes if the database callback
     * returns after the fragment has been detached from the activity.
     */
    private void loadImages() {
        Log.d(TAG, "Attempting to query 'events' collection to find posters...");

        database.getAllEventsList().addOnCompleteListener(task -> {
            // **CRASH PREVENTION**: Check if the Fragment is still added to its Activity.
            // If not, (e.g., user navigated away), do not proceed with UI updates.
            if (!isAdded() || getContext() == null) {
                Log.w(TAG, "loadImages callback received, but fragment is detached. Aborting UI update.");
                return;
            }

            if (task.isSuccessful()) {
                List<Event> allEvents = task.getResult();
                if (allEvents != null) {
                    // Filter the list to include only events that have a posterURL
                    List<Event> filteredList = allEvents.stream()
                            .filter(event -> event.getPosterURL() != null && !event.getPosterURL().isEmpty())
                            .collect(Collectors.toList());

                    Log.d(TAG, "Found " + filteredList.size() + " events with posters.");
                    adapter.updateData(filteredList);
                }
            } else {
                Log.e(TAG, "Error getting documents: ", task.getException());
                Toast.makeText(getContext(), "Failed to load event images.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Handles the delete image request from the adapter.
     * This fulfills <b>US 03.03.01 (Remove images)</b>.
     * <p>
     * It shows a confirmation dialog. If confirmed, it proceeds to nullify the
     * {@code posterURL} in the Firestore document for the specified event.
     *
     * @param event The {@link Event} object whose poster is to be "deleted".
     * @param position The adapter position of the item, used for efficient UI removal.
     */
    @Override
    public void onDelete(Event event, int position) {
        // **CRASH PREVENTION**: Check context before showing a dialog.
        if (getContext() == null) {
            Log.e(TAG, "Cannot show delete dialog, context is null.");
            return;
        }

        // 1. Show Confirmation Dialog
        new AlertDialog.Builder(getContext())
                .setTitle("Confirm Removal")
                .setMessage("Are you sure you want to remove the poster for '" + event.getEventName() + "'? This action cannot be undone.")
                .setPositiveButton("Remove", (dialog, which) -> {
                    // User confirmed, proceed with deletion
                    removeImageFromEvent(event, position);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // User cancelled, do nothing
                    dialog.dismiss();
                })
                .show();
    }

    /**
     * Performs the database operation to "remove" the image.
     * <p>
     * This method sets the {@code posterURL} of the given event to {@code null} and then
     * updates the event in the Firestore database using {@link Database#setEventData(Long, Event)}.
     * <p>
     * On success, it removes the item from the adapter locally for an immediate and
     * efficient UI update, rather than re-fetching the entire list.
     *
     * @param event The {@link Event} object to update.
     * @param position The adapter position, to be passed to the adapter for removal.
     */
    private void removeImageFromEvent(Event event, int position) {
        if (event == null || event.getUniqueEventID() == null) {
            Toast.makeText(getContext(), "Error: Invalid event.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Set the poster URL to null
        event.setPosterURL(null);

        // 2. Update the Event in Firestore
        database.setEventData(event.getUniqueEventID(), event)
                .addOnSuccessListener(aVoid -> {
                    // **CRASH PREVENTION**: Check if fragment is still attached
                    if (!isAdded() || getContext() == null) {
                        Log.w(TAG, "Image removal successful, but fragment is detached.");
                        return;
                    }

                    Log.d(TAG, "Successfully nulled posterURL in Firestore for: " + event.getEventName());
                    Toast.makeText(getContext(), "Image removed.", Toast.LENGTH_SHORT).show();

                    // 3. Remove the item from the adapter locally
                    // This is much faster than calling loadImages() again.
                    adapter.removeItem(position);
                })
                .addOnFailureListener(e -> {
                    // **CRASH PREVENTION**: Check if fragment is still attached
                    if (!isAdded() || getContext() == null) {
                        Log.w(TAG, "Image removal failed, and fragment is detached.");
                        return;
                    }

                    Log.e(TAG, "Failed to update event to remove posterURL", e);
                    Toast.makeText(getContext(), "Failed to remove image. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }
}