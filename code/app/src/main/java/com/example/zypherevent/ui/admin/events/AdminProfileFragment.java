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
 * @author Arunavo Dutta
 * @version 3.0
 * @see AdminBaseListFragment
 * @see User
 * @see AdminProfilesAdapter
 * @see Database
 *
 * Completes US 03.02.01 As an administrator, I want to be able to remove profiles.
 * Completes US 03.05.01 As an administrator, I want to be able to browse profiles.
 * Completes US 03.07.01 As an administrator, I want to remove organizers.
 */
public class AdminProfileFragment extends AdminBaseListFragment {

    private static final String TAG = "AdminProfileFragment";

    private Database db;
    private FirebaseFirestore firestoreDb;

    private AdminProfilesAdapter adapter;
    private List<User> profileList = new ArrayList<>();
    private Button refreshButton;


    /**
     * Called immediately after {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, Bundle)}
     * has returned, but before any saved state has been restored in to the view.
     * This gives subclasses a chance to initialize themselves once they know their view hierarchy has been completely created.
     * In this implementation, it initializes the database connection, sets up the {@link androidx.recyclerview.widget.RecyclerView}
     * with an {@link AdminProfilesAdapter}, defines the action to be taken on profile deletion, and triggers the initial loading of user profiles.
     *
     * @param view The View returned by {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
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
     * Fetches the list of all user profiles from the database.
     * This method communicates with the {@link Database} class to retrieve all users.
     * On successful retrieval, it clears the local {@code profileList}, populates it with the
     * fetched users, and notifies the {@link AdminProfilesAdapter} to refresh the UI.
     * If the retrieval fails, it logs the error and displays a toast message to the user.
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
     * Handles the top-level request to delete a user profile.
     * Shows a confirmation dialog.
     * Checks if the user is an Organizer to determine if a cascading delete is needed.
     *
     * @param profile The {@link User} object representing the profile to be deleted.
     */
    private void handleDeleteProfile(User profile) {
        if (profile == null || profile.getHardwareID() == null) {
            Toast.makeText(getContext(), "Error: Profile has no ID", Toast.LENGTH_SHORT).show();
            return;
        }

        String profileName = profile.getFirstName() + " " + profile.getLastName();

        // --- Start: Ask for confirmation before deletion ---
        new AlertDialog.Builder(getContext())
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete the profile for '" + profileName + "'? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // User clicked "Delete".
                    Toast.makeText(getContext(), "Deleting " + profileName + "...", Toast.LENGTH_SHORT).show();

                    // --- Start: Check if cascading delete is needed ---
                    if (profile.getUserType() == UserType.ORGANIZER) {
                        // This is an organizer. We must delete their events first.
                        handleDeleteOrganizerEvents(profile);
                    } else {
                        // This is a basic user (Entrant/Admin). Just delete their profile.
                        handleDeleteBasicUser(profile);
                    }
                    // --- End ---

                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // User clicked "Cancel", so dismiss the dialog.
                    dialog.dismiss();
                })
                .show(); // Display the confirmation dialog
        // --- End ---
    }


    /**
     * [AC #8] Handles the cascading delete for an Organizer.
     * This method first deletes all events created by the organizer and, only upon
     * success, proceeds to delete the organizer's own profile.
     *
     * @param profile The User object (who must be an Organizer) to delete.
     */
    private void handleDeleteOrganizerEvents(User profile) {
        String organizerId = profile.getHardwareID();
        Log.d(TAG, "Deleting organizer. First, finding events for: " + organizerId);

        // 1. Find all events created by this organizer
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

                        // 2. Create a batch write to delete all their events at once
                        WriteBatch batch = firestoreDb.batch();
                        for (DocumentSnapshot document : documents) {
                            batch.delete(document.getReference());
                        }

                        // 3. Commit the batch
                        batch.commit().addOnSuccessListener(aVoid -> {
                            // 4. After events are deleted, delete the organizer's profile
                            Log.d(TAG, "Successfully deleted organizer's events. Now deleting profile.");
                            handleDeleteBasicUser(profile);

                        }).addOnFailureListener(e -> {
                            // Failed to delete the events. Do NOT delete the organizer.
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
     * This is called for basic users, or *after* an organizer's events are deleted.
     * [AC #5] Includes success message.
     *
     * @param profile The User to be deleted.
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

                // --- Start: Show success message ---
                Toast.makeText(getContext(), "Profile deleted successfully", Toast.LENGTH_SHORT).show();
                // --- End ---

            } else {
                Log.e(TAG, "Error deleting profile: ", task.getException());
                Toast.makeText(getContext(), "Failed to delete profile", Toast.LENGTH_SHORT).show();
            }
        });
    }
}