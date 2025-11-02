package com.example.zypherevent.ui.entrant.events;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.zypherevent.Database;
import com.example.zypherevent.EntrantActivity;
import com.example.zypherevent.databinding.FragmentEntrantSettingsBinding;
import com.example.zypherevent.userTypes.Entrant;
import com.google.android.gms.tasks.Task;

/**
 * @author Elliot Chrystal
 * @version 1.0
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
        binding.btnDeleteProfile.setOnClickListener(v -> deleteProfile(currentUser));
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
     * Deletes the provided entrant's profile. Currently a placeholder; extend to implement
     * account deletion and cleanup.
     *
     * @param userToDelete the entrant whose profile should be deleted
     * @return null (no-op for now)
     */
    private Void deleteProfile(Entrant userToDelete) {
        // do nothing now
        return null;
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