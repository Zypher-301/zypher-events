// package com.example.zypherevent.ui.admin.events;
package com.example.zypherevent.ui.admin.events; // Use your actual package name

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.zypherevent.Database;
import com.example.zypherevent.R;
import com.example.zypherevent.userTypes.User; // Using your real User model
import java.util.ArrayList;
import java.util.List;

/**
 * @author Arunavo Dutta
 * @version 2.0
 * @see AdminBaseListFragment
 * @see User
 * @see AdminProfilesAdapter
 * @see Database
 *
 * Completes US 03.02.01 As an administrator, I want to be able to remove profiles.
 * Completes US 03.05.01 As an administrator, I want to be able to browse profiles.
 */
public class AdminProfileFragment extends AdminBaseListFragment {

    private static final String TAG = "AdminProfileFragment";
    private Database db;
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

        // Set up the adapter with an empty list
        adapter = new AdminProfilesAdapter(profileList, profile -> {
            // This is the delete logic (US 03.02.01)
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
     * Handles the deletion of a user profile.
     * This method is triggered when an administrator decides to remove a profile from the list.
     * It first checks if the profile and its ID are valid. If not, it shows an error message.
     * It then proceeds to call the {@link Database#removeUserData(String)} method to delete the user's data from Firebase,
     * using the user's hardware ID.
     * Upon successful deletion from the database, the profile is removed from the local {@code profileList},
     * and the {@link AdminProfilesAdapter} is notified to update the UI.
     * If the deletion fails, an error is logged, and a toast message is displayed to the administrator.
     *
     * @param profile The {@link User} object representing the profile to be deleted.
     */
    private void handleDeleteProfile(User profile) {
        if (profile == null || profile.getHardwareID() == null) {
            Toast.makeText(getContext(), "Error: Profile has no ID", Toast.LENGTH_SHORT).show();
            return;
        }

        String profileName = profile.getFirstName() + " " + profile.getLastName();
        Toast.makeText(getContext(), "Deleting " + profileName, Toast.LENGTH_SHORT).show();

        // Call database to remove the user
        db.removeUserData(profile.getHardwareID()).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Successfully deleted profile: " + profileName);
                // Remove from local list and update UI
                profileList.remove(profile);
                adapter.notifyDataSetChanged();
            } else {
                Log.e(TAG, "Error deleting profile: ", task.getException());
                Toast.makeText(getContext(), "Failed to delete profile", Toast.LENGTH_SHORT).show();
            }
        });
    }
}