package com.example.zypherevent.ui.entrant.events;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.zypherevent.Database;
import com.example.zypherevent.EntrantActivity;
import com.example.zypherevent.Event;
import com.example.zypherevent.MainActivity;
import com.example.zypherevent.WaitlistEntry;
import com.example.zypherevent.databinding.FragmentEntrantSettingsBinding;
import com.example.zypherevent.userTypes.Entrant;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;

/**
 * @author Elliot Chrystal
 * @author Tom Yang (added delete profile function)
 * @version 2.0
 * @see EntrantActivity
 *
 * Fragment for the entrant's settings page. This is where entrants can change their profile information,
 * enable/disable geolocation, enable/disable notifications, and delete their profile.
 */
public class EntrantSettingsFragment extends Fragment {

    /** View binding for the entrant settings layout. */
    private FragmentEntrantSettingsBinding binding;

    /** The currently signed-in entrant associated with this fragment. */
    private Entrant currentUser;

    /** Reference to the application database interface. */
    private Database db;

    /** Required empty public constructor. */
    public EntrantSettingsFragment() { }

    /** Flag to track if deletion is currently in progress */
    private boolean isDeleting = false;

    /**
     * Inflates the fragment's view using ViewBinding.
     *
     * @param inflater  the LayoutInflater object that can be used to inflate any views
     * @param container the parent view that the fragment's UI should be attached to
     * @param savedInstanceState if non-null, this fragment is being re-constructed from a previous state
     * @return the root view for the fragment's layout
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentEntrantSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Called immediately after onCreateView. Initializes dependencies, loads the
     * current user from the host activity, populates UI fields, and registers listeners for edits.
     *
     * @param view the view returned by onCreateView
     * @param savedInstanceState if non-null, this fragment is being re-constructed from a previous state
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize database
        db = new Database();

        // Get the entrant user from the activity
        EntrantActivity activity = (EntrantActivity) requireActivity();
        currentUser = activity.getEntrantUser();

        // Set text boxes and toggles to reflect current user data
        binding.etFirstName.setText(currentUser.getFirstName());
        binding.etLastName.setText(currentUser.getLastName());
        binding.etEmail.setText(currentUser.getEmail());
        binding.etPhone.setText(currentUser.getPhoneNumber());
        binding.switchGeo.setChecked(currentUser.isUseGeolocation());
        binding.switchNotifications.setChecked(currentUser.getWantsNotifications());

        // Listen for changes, and update the user object accordingly
        binding.etFirstName.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                currentUser.setFirstName(s.toString());
            }
        });
        binding.etLastName.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                currentUser.setLastName(s.toString());
            }
        });
        binding.etEmail.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                currentUser.setEmail(s.toString());
            }
        });
        binding.etPhone.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                currentUser.setPhoneNumber(s.toString());
            }
        });

        // Switch listeners
        binding.switchGeo.setOnCheckedChangeListener((btn, checked) ->
                currentUser.setUseGeolocation(checked));

        binding.switchNotifications.setOnCheckedChangeListener((btn, checked) ->
                currentUser.setWantsNotifications(checked));

        // Persist changes
        binding.btnSaveChanges.setOnClickListener(v -> saveChanges(currentUser));

        // Delete profile (stub)
        binding.btnDeleteProfile.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    /**
     * Saves the provided entrant to the database.
     *
     * @param userToSave the entrant whose changes should be persisted
     * @return a {@link Task} representing the asynchronous save operation
     */
    private Task<Void> saveChanges(Entrant userToSave) {
        return db.setUserData(userToSave.getHardwareID(), userToSave)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(requireContext(), "Changes saved!", Toast.LENGTH_SHORT).show());
    }

    /**
     * Shows a confirmation dialog before deleting the profile.
     */
    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to delete your profile?\n\n" +
                        "This action cannot be undone and will permanently:\n" +
                        "• Remove all your personal information\n" +
                        "• Remove you from all event waitlists and registrations\n" +
                        "• Delete your account from our system")
                .setPositiveButton("Delete", (dialog, which) -> deleteProfile(currentUser))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Deletes the provided entrant's profile. Currently a placeholder; extend to implement
     * account deletion and cleanup.
     *
     * @param userToDelete the entrant whose profile should be deleted
     */
    private void deleteProfile(Entrant userToDelete) {
        if (userToDelete == null) {
            Log.e("EntrantSettingsFrag", "deleteProfile: Cannot identify user - userToDelete is null");
            showErrorDialog("Error: Cannot identify user");
            return;
        }

        String hardwareID = userToDelete.getHardwareID();
        if (hardwareID == null || hardwareID.isEmpty()) {
            Log.e("EntrantSettingsFrag", "Invalid user ID - hardwareID is null or empty");
            showErrorDialog("Error: Invalid user ID");
            return;
        }

        if (isDeleting) return; // Prevents multiple deletions

        setDeletingState(true);

        db.getUser(hardwareID)
                .addOnSuccessListener(user -> {
                    if (user instanceof Entrant) {
                        removeUserFromEvents((Entrant) user, hardwareID);
                    } else {
                        deleteUserData(hardwareID);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("EntrantSettingsFrag", "Failed to retrieve user from database");
                    deleteUserData(hardwareID);
                });
    }

    /**
     * Removes the entrant from all events they are in
     *
     * @param entrant The entrant to remove from events
     * @param hardwareID The entrant's hardware ID
     */
    private void removeUserFromEvents(Entrant entrant, String hardwareID) {
        ArrayList<Event> registeredEvents = entrant.getRegisteredEventHistory();

        if (registeredEvents == null || registeredEvents.isEmpty()) {
            deleteUserData(hardwareID);
            return;
        }

        // Count how many events need to be updated
        int totalEvents = registeredEvents.size();
        int[] completedEvents = {0}; // Using array to modify in lambda

        // Loop through each event and remove the user
        for (Event event : registeredEvents) {
            Long eventID = event.getUniqueEventID();

            // Get the latest event data from Firebase
            db.getEvent(eventID)
                    .addOnSuccessListener(eventFromDB -> {
                        if (eventFromDB != null) {
                            // Remove entrant from waitlist - find and remove the entry containing this entrant
                            WaitlistEntry entryToRemove = null;
                            for (WaitlistEntry entry : eventFromDB.getWaitListEntrants()) {
                                if (entry.getEntrant().equals(entrant)) {
                                    entryToRemove = entry;
                                    break;
                                }
                            }
                            if (entryToRemove != null) {
                                eventFromDB.removeEntrantFromWaitList(entryToRemove);
                            }

                            // Remove entrant from other lists
                            eventFromDB.removeEntrantFromAcceptedList(entrant);
                            eventFromDB.removeEntrantFromDeclinedList(entrant);

                            // Save updated event back to database
                            db.setEventData(eventID, eventFromDB)
                                    .addOnCompleteListener(task -> {
                                        completedEvents[0]++;
                                        // Check if all events have been processed
                                        if (completedEvents[0] >= totalEvents) {
                                            deleteUserData(hardwareID);
                                        }
                                    });
                        } else {
                            // Event doesn't exist anymore, move on
                            completedEvents[0]++;
                            if (completedEvents[0] >= totalEvents) {
                                deleteUserData(hardwareID);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Failed to get event, but continue anyway
                        completedEvents[0]++;
                        if (completedEvents[0] >= totalEvents) {
                            deleteUserData(hardwareID);
                        }
                    });
        }
    }

    /**
     * Deletes the user data from database after removing from events
     *
     * @param hardwareID The entrant's hardware ID
     */
    private void deleteUserData(String hardwareID) {
        db.removeUserData(hardwareID)
                .addOnSuccessListener(v ->{
                    setDeletingState(false);
                    handleProfileDeleted();
                })
                .addOnFailureListener(e -> {
                    setDeletingState(false);
                    showErrorDialog("Failed to delete profile: " + e.getMessage());
                });
    }

    /**
     * Handles post-deletion actions after profile has been successfully deleted.
     * Shows a success dialog and navigates back to MainActivity
     */
    private void handleProfileDeleted() {
        if (getContext() == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Profile Deleted")
                .setMessage("Your profile and all personal information have been successfully deleted. You have been removed from all events.")
                .setPositiveButton("OK", (dialog, which) -> {
                    Intent intent = new Intent(getActivity(), MainActivity.class); // Check which activity we are navigating to
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Updates UI to reflection deletion state
     *
     * @param deleting true if deletion is in progress, false otherwise
     */
    private void setDeletingState(boolean deleting) {
        isDeleting = deleting;
        if (binding != null) {
            binding.btnDeleteProfile.setEnabled(!deleting);
            binding.btnDeleteProfile.setText(deleting ? "Deleting..." : "Delete Profile");
        }
    }

    /**
     * Shows an error dialog with the specified message
     *
     * @param message The error message to display
     */
    private void showErrorDialog(String message) {
        if (getContext() == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * A small helper class that allows overriding only the required TextWatcher methods.
     */
    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) {}
    }

    /**
     * Clears the binding reference to avoid memory leaks when the view is destroyed.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}