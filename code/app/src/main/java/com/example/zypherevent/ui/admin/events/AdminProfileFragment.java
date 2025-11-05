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

/**
 * A fragment for administrators to browse and manage user profiles.
 *
 * <p>This class provides the user interface and logic for administrators to view a list of all
 * user profiles in the system. It supports key administrative functions, including the ability
 * to remove user profiles. When removing an organizer, this class handles the cascading deletion
 * of all events associated with that organizer to maintain data integrity.</p>
 *
 * <p>This fragment extends {@link AdminBaseListFragment} to reuse the basic list layout and
 * functionality. It uses {@link AdminProfilesAdapter} to populate the {@link androidx.recyclerview.widget.RecyclerView}
 * with user data fetched from the Firestore database via the {@link Database} class.</p>
 *
 * <p><b>User Stories Fulfilled:</b></p>
 * <ul>
 *     <li><b>US 03.02.01:</b> As an administrator, I want to be able to remove profiles.</li>
 *     <li><b>US 03.05.01:</b> As an administrator, I want to be able to browse profiles.</li>
 *     <li><b>US 03.07.01:</b> As an administrator, I want to remove organizers.</li>
 * </ul>
 *
 * @author Arunavo Dutta
 * @version 3.0
 * @see AdminBaseListFragment
 * @see AdminProfilesAdapter
 * @see User
 * @see Database
 */
public class AdminProfileFragment extends AdminBaseListFragment {

    private static final String TAG = "AdminProfileFragment";

    private Database db;
    private FirebaseFirestore firestoreDb;

    private AdminProfilesAdapter adapter;
    private List<User> profileList = new ArrayList<>();
    private Button refreshButton;


    /**
     * Called when the fragment's view has been created.
     *
     * <p>This method initializes the user interface and sets up the necessary components for the fragment.
     * It performs the following actions:
     * <ul>
     *     <li>Initializes the Firestore database connection.</li>
     *     <li>Sets up the {@link androidx.recyclerview.widget.RecyclerView} with an {@link AdminProfilesAdapter}.
     *     The adapter is configured to handle profile deletion requests.</li>
     *     <li>Configures the refresh button to reload the list of profiles when clicked.</li>
     *     <li>Triggers the initial fetch of all user profiles from the database to populate the list.</li>
     * </ul>
     *
     * @param view The View returned by {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = new Database();
        firestoreDb = FirebaseFirestore.getInstance(); // <-- ADDED

        // Set up the adapter with an empty list
        adapter = new AdminProfilesAdapter(profileList, profile -> {
            // This contains delete logic (US 03.02.01 & US 03.07.01)
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
     * Fetches all user profiles from the Firestore database.
     * <p>
     * This method initiates an asynchronous call to the {@link Database#getAllUsers()} method.
     * Upon successful completion, it clears the current {@code profileList}, repopulates it with the
     * fetched user data, and then notifies the {@link AdminProfilesAdapter} to refresh the
     * {@link androidx.recyclerview.widget.RecyclerView}, displaying the updated list of profiles.
     * If the database call fails, an error is logged and a toast message is displayed to the user,
     * indicating the failure.
     * </p>
     */
    private void loadProfiles() {
        db.getAllUsers().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                profileList.clear();
                List<User> fetchedUsers = task.getResult();
                if (fetchedUsers != null) {
                    profileList.addAll(fetchedUsers);
                }
                adapter.notifyDataSetChanged();
                Log.d(TAG, "Successfully fetched " + profileList.size() + " profiles.");
            } else {
                Log.e(TAG, "Error fetching profiles: ", task.getException());
                Toast.makeText(getContext(), "Error fetching profiles", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Initiates the process of deleting a user profile.
     * <p>
     * This method first displays an {@link AlertDialog} to confirm the deletion action with the administrator.
     * If the user confirms, it checks the {@link UserType} of the profile.
     * <ul>
     *     <li>If the user is an {@code ORGANIZER}, it triggers a cascading delete by calling
     *     {@link #handleDeleteOrganizerEvents(User)} to remove their associated events first.</li>
     *     <li>If the user is a basic user (e.g., {@code ENTRANT}), it proceeds directly to delete
     *     the user's profile by calling {@link #handleDeleteBasicUser(User)}.</li>
     * </ul>
     * If the user cancels the dialog, no action is taken.
     *
     * @param profile The {@link User} object representing the profile to be deleted.
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
                .setMessage("Are you sure you want to delete the profile for '" + profileName + "'? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // User clicked "Delete".
                    Toast.makeText(getContext(), "Deleting " + profileName + "...", Toast.LENGTH_SHORT).show();

                    // Check if cascading delete is needed
                    if (profile.getUserType() == UserType.ORGANIZER) {
                        // This is an organizer. We must delete their events first.
                        handleDeleteOrganizerEvents(profile);
                    } else {
                        // This is a basic user (Entrant/Admin). We just delete their profile.
                        handleDeleteBasicUser(profile);
                    }

                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // User clicked "Cancel", so dismiss the dialog.
                    dialog.dismiss();
                })
                .show(); // Display the confirmation dialog
    }


    /**
     * Manages the cascading deletion of an organizer and their associated events.
     * <p>
     * This method is specifically invoked when the user to be deleted is an {@link UserType#ORGANIZER}. It performs a multi-step process to ensure data integrity:
     * <ol>
     *     <li>It queries the 'events' collection in Firestore to find all events created by the specified organizer, identified by their hardware ID.</li>
     *     <li>If events are found, it uses a {@link WriteBatch} to delete all of them in a single atomic operation. This prevents leaving orphaned events in the database.</li>
     *     <li>Only after the successful deletion of all associated events (or if the organizer had no events to begin with), it proceeds to call {@link #handleDeleteBasicUser(User)} to delete the organizer's own profile document.</li>
     * </ol>
     * If any step in this process fails (e.g., failing to query for events or failing to delete them), the entire operation is aborted to prevent partial data deletion, and an error message is displayed to the user.
     *
     * @param profile The {@link User} object representing the organizer whose profile and events are to be deleted.
     */
    private void handleDeleteOrganizerEvents(User profile) {
        String organizerId = profile.getHardwareID();
        Log.d(TAG, "Deleting organizer. First, finding events for: " + organizerId);

        // Find all events created by this organizer
        firestoreDb.collection("events")
                .whereEqualTo("eventOrganizerHardwareID", organizerId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<DocumentSnapshot> documents = task.getResult().getDocuments();
                        if (documents.isEmpty()) {
                            // No events to delete. Proceed to delete the user.
                            Log.d(TAG, "Organizer has no events. Deleting profile.");
                            handleDeleteBasicUser(profile);
                            return;
                        }

                        Log.d(TAG, "Found " + documents.size() + " events to delete. Starting batch delete.");

                        // Create a batch write to delete all their events at once
                        WriteBatch batch = firestoreDb.batch();
                        for (DocumentSnapshot document : documents) {
                            batch.delete(document.getReference());
                        }

                        // Commit the batch
                        batch.commit().addOnSuccessListener(aVoid -> {
                            // After events are deleted, we delete the organizer's profile
                            Log.d(TAG, "Successfully deleted organizer's events. Now deleting profile.");
                            handleDeleteBasicUser(profile);

                        }).addOnFailureListener(e -> {
                            // If failed to delete the events. Do NOT delete the organizer.
                            Log.e(TAG, "Error deleting organizer's events", e);
                            Toast.makeText(getContext(), "Error: Failed to delete organizer's events.", Toast.LENGTH_SHORT).show();
                        });

                    } else {
                        // Query to find events failed. Do NOT delete the organizer.
                        Log.e(TAG, "Error finding events for organizer", task.getException());
                        Toast.makeText(getContext(), "Error: Could not find organizer's events.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Handles the final deletion of a single user document from Firestore.
     * <p>
     * This method is called directly for non-organizer users (Entrants, Admins) or
     * as the final step for an Organizer after all their associated events have been successfully deleted.
     * It communicates with the database to remove the user's data.
     * <p>
     * On successful deletion, it removes the user from the local list, updates the UI adapter,
     * and displays a confirmation toast. On failure, it logs the error and notifies the user via a toast.
     *
     * @param profile The {@link User} object representing the user profile to be deleted from the database.
     */
    private void handleDeleteBasicUser(User profile) {
        String profileName = profile.getFirstName() + " " + profile.getLastName();

        // Call database to remove the user
        db.removeUserData(profile.getHardwareID()).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Successfully deleted profile: " + profileName);
                // Remove from local list and update UI
                profileList.remove(profile);
                adapter.notifyDataSetChanged();

                // Show success message
                Toast.makeText(getContext(), "Profile deleted successfully", Toast.LENGTH_SHORT).show();

            } else {
                Log.e(TAG, "Error deleting profile: ", task.getException());
                Toast.makeText(getContext(), "Failed to delete profile", Toast.LENGTH_SHORT).show();
            }
        });
    }
}