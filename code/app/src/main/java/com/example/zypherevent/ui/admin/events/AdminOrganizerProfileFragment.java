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
import com.example.zypherevent.R;
import com.example.zypherevent.userTypes.User;
import com.example.zypherevent.userTypes.UserType;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A fragment for administrators to browse and manage ONLY Organizer profiles.
 *
 * <p>This class provides the user interface and logic for administrators to view a list of all
 * Organizers in the system. It supports key administrative functions, including the ability
 * to remove organizers. When removing an organizer, this class handles the cascading deletion
 * of all events associated with that organizer to maintain data integrity.</p>
 *
 * <p>This fragment extends {@link AdminBaseListFragment} and uses {@link AdminOrganizerProfileAdapter}
 * to populate the {@link androidx.recyclerview.widget.RecyclerView} with organizer data.</p>
 *
 * <p><b>User Stories Fulfilled:</b></p>
 * <ul>
 * <li><b>US 03.05.01:</b> As an administrator, I want to be able to browse profiles. (Filtered for Organizers)</li>
 * <li><b>US 03.07.01:</b> As an administrator, I want to remove organizers.</li>
 * </ul>
 *
 * @author Arunavo Dutta
 * @version 1.0
 * @see AdminBaseListFragment
 * @see AdminOrganizerProfileAdapter
 * @see User
 * @see Database
 */
public class AdminOrganizerProfileFragment extends AdminBaseListFragment {

    private static final String TAG = "AdminOrgProfileFrag";

    private Database db;
    private FirebaseFirestore firestoreDb;

    private AdminOrganizerProfileAdapter adapter;
    private List<User> organizerList = new ArrayList<>();
    private Button refreshButton;


    /**
     * Called when the fragment's view has been created.
     *
     * <p>This method initializes the UI, database, and sets up the
     * {@link androidx.recyclerview.widget.RecyclerView} with an {@link AdminOrganizerProfileAdapter}.
     * The adapter is configured to handle profile deletion requests.</p>
     *
     * @param view The View returned by {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = new Database();
        firestoreDb = FirebaseFirestore.getInstance();

        adapter = new AdminOrganizerProfileAdapter(organizerList, profile -> {
            // This contains delete logic (US 03.07.01)
            handleDeleteProfile(profile);
        });

        recyclerView.setAdapter(adapter);

        refreshButton = view.findViewById(R.id.refresh_button);
        refreshButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Refreshing list...", Toast.LENGTH_SHORT).show();
            loadProfiles();
        });

        // Fetch users from Firebase
        loadProfiles();
    }

    /**
     * Fetches all user profiles from the Firestore database and FILTERS for Organizers.
     * <p>
     * This method initiates an asynchronous call to the {@link Database#getAllUsers()} method.
     * Upon successful completion, it filters the complete user list to find only those
     * with {@link UserType#ORGANIZER}. It then clears the current {@code organizerList},
     * repopulates it with the filtered data, and notifies the adapter to refresh the UI.
     * </p>
     */
    private void loadProfiles() {
        db.getAllUsers().addOnCompleteListener(task -> {
            // Check if fragment is still added
            if (!isAdded() || getContext() == null) {
                Log.w(TAG, "loadProfiles callback received, but fragment is detached.");
                return;
            }

            if (task.isSuccessful()) {
                organizerList.clear();
                List<User> fetchedUsers = task.getResult();

                if (fetchedUsers != null) {
                    // Filter the list to only include Organizers
                    List<User> organizersOnly = fetchedUsers.stream()
                            .filter(user -> user.getUserType() == UserType.ORGANIZER)
                            .collect(Collectors.toList());

                    organizerList.addAll(organizersOnly);
                }

                adapter.notifyDataSetChanged();
                Log.d(TAG, "Successfully fetched and filtered " + organizerList.size() + " organizer profiles.");
            } else {
                Log.e(TAG, "Error fetching profiles: ", task.getException());
                Toast.makeText(getContext(), "Error fetching profiles", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Initiates the process of deleting an organizer profile.
     * <p>
     * This method displays an {@link AlertDialog} to confirm the deletion.
     * If the user confirms, it triggers a cascading delete by calling
     * {@link #handleDeleteOrganizerEvents(User)} to remove their associated events first.
     *
     * @param profile The {@link User} object (who is an Organizer) to be deleted.
     */
    private void handleDeleteProfile(User profile) {
        if (profile == null || profile.getHardwareID() == null) {
            Toast.makeText(getContext(), "Error: Profile has no ID", Toast.LENGTH_SHORT).show();
            return;
        }

        String profileName = profile.getFirstName() + " " + profile.getLastName();

        // Ask for confirmation before deletion
        new AlertDialog.Builder(getContext())
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete the organizer '" + profileName + "'? " +
                        "This will also delete ALL events created by them. This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    Toast.makeText(getContext(), "Deleting " + profileName + "...", Toast.LENGTH_SHORT).show();

                    // Since this is only for Organizers, we are directly call the cascade delete logic.
                    handleDeleteOrganizerEvents(profile);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }


    /**
     * Manages the cascading deletion of an organizer and their associated events.
     * (This logic is identical to AdminProfileFragment)
     *
     * @param profile The {@link User} object representing the organizer.
     */
    private void handleDeleteOrganizerEvents(User profile) {
        String organizerId = profile.getHardwareID();
        Log.d(TAG, "Deleting organizer. First, finding events for: " + organizerId);

        // Find all events created by this organizer
        firestoreDb.collection("events")
                .whereEqualTo("eventOrganizerHardwareID", organizerId)
                .get()
                .addOnCompleteListener(task -> {
                    if (!isAdded() || getContext() == null) {
                        Log.w(TAG, "handleDeleteOrganizerEvents callback received, but fragment is detached.");
                        return;
                    }

                    if (task.isSuccessful()) {
                        List<DocumentSnapshot> documents = task.getResult().getDocuments();
                        if (documents.isEmpty()) {
                            // No events to delete. Proceed to delete the user.
                            Log.d(TAG, "Organizer has no events. Deleting profile.");
                            handleDeleteBasicUser(profile);
                            return;
                        }

                        Log.d(TAG, "Found " + documents.size() + " events to delete. Starting batch delete.");

                        // Create a batch write to delete all their events
                        WriteBatch batch = firestoreDb.batch();
                        for (DocumentSnapshot document : documents) {
                            batch.delete(document.getReference());
                        }

                        // Commit the batch
                        batch.commit().addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Successfully deleted organizer's events. Now deleting profile.");
                            handleDeleteBasicUser(profile);

                        }).addOnFailureListener(e -> {
                            Log.e(TAG, "Error deleting organizer's events", e);
                            Toast.makeText(getContext(), "Error: Failed to delete organizer's events.", Toast.LENGTH_SHORT).show();
                        });

                    } else {
                        Log.e(TAG, "Error finding events for organizer", task.getException());
                        Toast.makeText(getContext(), "Error: Could not find organizer's events.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Handles the final deletion of a single user document from Firestore.
     * (This logic is identical to AdminProfileFragment)
     *
     * @param profile The {@link User} object representing the user profile to be deleted.
     */
    private void handleDeleteBasicUser(User profile) {
        String profileName = profile.getFirstName() + " " + profile.getLastName();

        // Call database to remove the user
        db.removeUserData(profile.getHardwareID()).addOnCompleteListener(task -> {
            if (!isAdded() || getContext() == null) {
                Log.w(TAG, "handleDeleteBasicUser callback received, but fragment is detached.");
                return;
            }

            if (task.isSuccessful()) {
                Log.d(TAG, "Successfully deleted profile: " + profileName);
                // Remove from local list and update UI
                organizerList.remove(profile);
                adapter.notifyDataSetChanged();
                Toast.makeText(getContext(), "Profile deleted successfully", Toast.LENGTH_SHORT).show();

            } else {
                Log.e(TAG, "Error deleting profile: ", task.getException());
                Toast.makeText(getContext(), "Failed to delete profile", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
