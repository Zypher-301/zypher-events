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

    private FragmentEntrantSettingsBinding binding;

    public EntrantSettingsFragment() { }

    private Entrant currentUser;

    private Database db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentEntrantSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

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
        binding.switchNotifications.setChecked(currentUser.wantsNotifications());

        // listen for changes, and change the user object to reflect changes
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

        binding.btnSaveChanges.setOnClickListener(v -> saveChanges(currentUser));

        binding.btnDeleteProfile.setOnClickListener(v ->
                deleteProfile(currentUser));
    }

    private Void saveChanges(Entrant userToSave) {
        // do nothing now
        return null;
    }

    private Void deleteProfile(Entrant userToDelete) {
        // do nothing now
        return null;
    }

    /**
     * A small helper class to change only what is needed
     */
    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) {}
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}