// package com.example.zypherevent.ui.admin.events;
package com.example.zypherevent.ui.admin.events; // Use your actual package name

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.zypherevent.Database;
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
 * Fulfills US 03.05.01: As an administrator, I want to be able to browse profiles.
 * Fulfills US 03.02.01: As an administrator, I want to be able to remove profiles.
 */
public class AdminProfileFragment extends AdminBaseListFragment {

    private static final String TAG = "AdminProfileFragment";
    private Database db;
    private AdminProfilesAdapter adapter;
    private List<User> profileList = new ArrayList<>(); // Use real User model

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

        // Fetch users from Firebase
        loadProfiles();
    }

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