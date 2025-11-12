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
 * @author Arunavo Dutta
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
     *
     * <p>
     * This method initializes the user interface. It sets up the {@link androidx.recyclerview.widget.RecyclerView}
     * with an {@link AdminImagesAdapter}, configures the refresh button's click listener to reload
     * the list of images, and triggers the initial data load by calling {@link #loadImages()}.
     *
     * @param view The View returned by {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        database = new Database();

        adapter = new AdminImagesAdapter(eventListWithPosters, this);
        recyclerView.setAdapter(adapter);

        // --- REFRESH BUTTON LOGIC ---
        refreshButton = view.findViewById(R.id.refresh_button);
        refreshButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Refreshing images...", Toast.LENGTH_SHORT).show();
            loadImages();
        });
        // --- END REFRESH LOGIC ---

        // Load the images initially
        loadImages();
    }

    /**
     * Fetches all events from the database and filters them to show only those with posters.
     * <p>
     * This method retrieves the complete list of events from Firestore using {@link Database#getAllEventsList()}.
     * It then filters this list to include only events that have a non-null and non-empty poster URL.
     * The resulting list is passed to the {@link AdminImagesAdapter} to update the UI. This fulfills
     * the requirement for <b>US 03.06.01 (Browse images)</b>.
     * </p>
     * <p>
     * A safety check is included to prevent {@link NullPointerException} or other crashes. The check ensures
     * that UI updates are only attempted if the fragment is still attached to its activity when the
     * asynchronous database call completes. If the fragment is not attached, the operation is aborted.
     * </p>
     */
    private void loadImages() {
        Log.d(TAG, "Attempting to query 'events' collection to find posters...");

        database.getAllEventsList().addOnCompleteListener(task -> {
            // Check if the Fragment is still added to its Activity.
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
     * Handles the delete image request from the {@link AdminImagesAdapter}.
     * <p>
     * This method fulfills the user story <b>US 03.03.01 (Remove images)</b>.
     * It first displays a confirmation dialog to the administrator to prevent
     * accidental removal. If the administrator confirms, it calls the
     * {@code removeImageFromEvent} method to nullify the event's {@code posterURL}
     * in the Firestore database. A safety check is included to prevent dialogs
     * from being shown if the fragment's context is no longer valid.
     * </p>
     *
     * @param event The {@link Event} object associated with the image to be removed.
     * @param position The adapter position of the item. This is used for efficient UI
     *                 updates upon successful removal.
     */
    @Override
    public void onDelete(Event event, int position) {
        // Check context before showing a dialog.
        if (getContext() == null) {
            Log.e(TAG, "Cannot show delete dialog, context is null.");
            return;
        }

        // Show Confirmation Dialog
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
     * Removes the poster image associated with a specific event.
     * <p>
     * This method effectively "deletes" the image by setting the {@code posterURL} field of the
     * specified {@link Event} object to {@code null} and then updating the event document in Firestore.
     * Upon successful update in the database, the item is removed from the local RecyclerView adapter
     * to reflect the change immediately in the UI.
     * </p>
     * <p>
     * Includes safety checks to prevent crashes if the fragment is detached from the activity
     * during the asynchronous database operation.
     * </p>
     *
     * @param event The {@link Event} object from which to remove the poster URL. Must not be null
     *              and must have a valid unique ID.
     * @param position The adapter position of the item being removed. This is used to efficiently
     *                 notify the adapter to remove the item from the view.
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
                    // Check if fragment is still attached
                    if (!isAdded() || getContext() == null) {
                        Log.w(TAG, "Image removal successful, but fragment is detached.");
                        return;
                    }

                    Log.d(TAG, "Successfully nulled posterURL in Firestore for: " + event.getEventName());
                    Toast.makeText(getContext(), "Image removed.", Toast.LENGTH_SHORT).show();

                    // 3. Remove the item from the adapter locally
                    adapter.removeItem(position);
                })
                .addOnFailureListener(e -> {
                    // Check if fragment is still attached
                    if (!isAdded() || getContext() == null) {
                        Log.w(TAG, "Image removal failed, and fragment is detached.");
                        return;
                    }

                    Log.e(TAG, "Failed to update event to remove posterURL", e);
                    Toast.makeText(getContext(), "Failed to remove image. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }
}