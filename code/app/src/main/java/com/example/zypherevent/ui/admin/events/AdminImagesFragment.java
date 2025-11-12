package com.example.zypherevent.ui.admin.events;


import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.zypherevent.Database;
import com.example.zypherevent.Event;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Arunavo Dutta
 * @version 1.3
 * @see AdminBaseListFragment
 * @see Event
 * @see AdminImagesAdapter
 * @see "res/navigation/admin_navigation.xml"
 */

public class AdminImagesFragment extends AdminBaseListFragment {

    private static final String TAG = "AdminImagesFragment";
    private AdminImagesAdapter adapter;
    private Database database;
    private List<Event> eventList = new ArrayList<>();

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        database = new Database();

        // Pass the deleteEventPoster method reference to the adapter's listener
        adapter = new AdminImagesAdapter(eventList, this::deleteEventPoster);

        recyclerView.setAdapter(adapter);

        fetchEventPosters();
    }

    private void fetchEventPosters() {
        database.getAllEventsList().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<Event> allEvents = task.getResult();
                if (allEvents != null) {
                    // Filter events that have a posterURL
                    List<Event> eventsWithPosters = allEvents.stream()
                            .filter(event -> event.getPosterURL() != null && !event.getPosterURL().isEmpty())
                            .collect(Collectors.toList());
                    adapter.updateData(eventsWithPosters);
                }
            } else {
                Log.e(TAG, "Error getting documents: ", task.getException());
                Toast.makeText(getContext(), "Failed to load event images.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Deletes the poster for a given event from both Firestore and Firebase Storage.
     *
     * @param event The event whose poster is to be deleted.
     */
    private void deleteEventPoster(Event event) {
        String posterUrl = event.getPosterURL();
        if (posterUrl == null || posterUrl.isEmpty()) {
            Toast.makeText(getContext(), "No image to delete for this event.", Toast.LENGTH_SHORT).show();
            return;
        }

        // First, set the posterURL in the Firestore document to null
        event.setPosterURL(null);
        database.setEventData(event.getUniqueEventID(), event)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Event poster URL removed from Firestore for event: " + event.getEventName());
                    // If Firestore update is successful, delete the image from Firebase Storage
                    deleteImageFromStorage(posterUrl);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to remove poster URL from Firestore", e);
                    Toast.makeText(getContext(), "Failed to update event details.", Toast.LENGTH_SHORT).show();
                    // IMPORTANT: Restore the URL locally if the database update fails
                    // This prevents an inconsistent state where the UI might not match the backend
                    event.setPosterURL(posterUrl);
                });
    }

    /**
     * Deletes an image file from Firebase Storage using its download URL.
     *
     * @param imageUrl The full HTTPS download URL of the image to be deleted.
     */
    private void deleteImageFromStorage(String imageUrl) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReferenceFromUrl(imageUrl);

        storageRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Image successfully deleted from Firebase Storage.");
                    Toast.makeText(getContext(), "Image deleted successfully.", Toast.LENGTH_SHORT).show();
                    // Refresh the list from the database to reflect the change in the UI
                    fetchEventPosters();
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Failed to delete image from Firebase Storage", exception);
                    Toast.makeText(getContext(), "Failed to delete image file.", Toast.LENGTH_SHORT).show();
                });
    }
}
