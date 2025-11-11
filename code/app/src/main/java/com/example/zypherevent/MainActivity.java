package com.example.zypherevent;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.LocationRequest;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.example.zypherevent.userTypes.Administrator;
import com.example.zypherevent.userTypes.Entrant;
import com.example.zypherevent.userTypes.Organizer;
import com.example.zypherevent.userTypes.User;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.appcompat.app.AppCompatActivity;

import com.example.zypherevent.databinding.ActivityMainBinding;
import com.example.zypherevent.userTypes.UserType;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;

import androidx.appcompat.app.AlertDialog;

/**
 * @author Elliot Chrystal
 * @version 1.0
 *
 * Entry point activity for the Zypher Event app. On launch, this activity attempts to look up
 * the current device's user by hardware ID and routes recognized users to the appropriate
 * activity (Entrant, Organizer, or Administrator). If no user is found, it presents a simple
 * profile creation flow.
 */
public class MainActivity extends AppCompatActivity {
    /** Navigation configuration for top-level destinations (if/when set). */
    private AppBarConfiguration mAppBarConfiguration;

    /** ViewBinding for the main activity layout. */
    private ActivityMainBinding binding;

    /** Reference to the application database interface. */
    private Database db;

    /** The current device's unique hardware ID used to identify the user. */
    private String userHardwareID;

    /**
     * This function:
     * 1. shows a blank page
     * 2. requests data from the database about the user
     * 3. sends the user (if found) to the corresponding activity
     * 4. if no user is found in database, then prompt the user to create a profile as an
     *    entrant or organizer.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in onSaveInstanceState.
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize database
        db = new Database();

        // Get hardware ID from user's device
        userHardwareID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        Log.d("MainActivityLogic", "User hardware id: " + userHardwareID);

        // For testing, to create a user entry in the database, uncomment one of the following!
//        setCurrentToEntrant(db);
//        setCurrentToOrganizer(db);
//        setCurrentToAdministrator(db);
        // By leaving these commented, if there is not a pre-existing entry for the hwid in the
        // database, then you will be prompted as a new user!

        // Start the task for getting the user from hardware ID
        db.getUser(userHardwareID).addOnCompleteListener(task -> {
            // if unsuccessful, then just show the startup page (no internet maybe?)
            if (!task.isSuccessful()) {
                Log.e("MainActivityLogic", "getUser failed", task.getException());
                showProfileInformationPage(); // treat as new user on error
                return;
            }

            // Get the user from the task's result
            User curUser = task.getResult();

            // If the result is null, no matching user was found for the hardware ID
            if (curUser == null) {
                Log.d("MainActivityLogic", "New user (HWID not recognized).");
                showProfileInformationPage();
                return;
            }

            // Recognized users are routed to their activities!
            if (curUser.getUserType() == UserType.ENTRANT) {
                Log.d("MainActivityLogic", "Recognized Entrant");
                goToEntrant(curUser);

            } else if (curUser.getUserType() == UserType.ORGANIZER) {
                Log.d("MainActivityLogic", "Recognized Organizer");
                goToOrganizer(curUser);

            } else if (curUser.getUserType() == UserType.ADMINISTRATOR) {
                Log.d("MainActivityLogic", "Recognized Administrator");
                goToAdministrator(curUser);

            }
        });
    }

    /**
     * Displays the role selection page for new users, allowing them to choose Entrant or Organizer.
     */
    private void showProfileInformationPage() {
        // Set the view to the startup role declaration page
        setContentView(R.layout.startup_role_declaration_page);

        // Add the buttons
        Button btnEntrant = findViewById(R.id.btnEntrant);
        Button btnOrganizer = findViewById(R.id.btnOrganizer);

        // Listen for entrant click
        btnEntrant.setOnClickListener(v -> {
            Log.d("MainActivityLogic", "Entrant button clicked");

            showProfileInformationPageEntrant();
        });

        // Listen for Organizer click
        btnOrganizer.setOnClickListener(v -> {
            Log.d("MainActivityLogic", "Organizer button clicked");

            showProfileInformationPageOrganizer();
        });
    }

    /**
     * Shows the Entrant profile creation page and handles input validation and persistence.
     * On save, creates an Entrant with the provided information, stores it in the database,
     * and routes to the Entrant activity.
     */
    private void showProfileInformationPageEntrant() {
        // set the view to the profile information page for entrants
        setContentView(R.layout.profile_information_page);

        // Get references to XML elements
        EditText editTextFirstName = findViewById(R.id.etFirstName);
        EditText editTextLastName = findViewById(R.id.etLastName);
        EditText editTextEmail = findViewById(R.id.etEmail);
        EditText editTextPhone = findViewById(R.id.etPhone);
        Switch switchGeolocation = findViewById(R.id.switchGeo);
        Button btnSaveProfile = findViewById(R.id.btnSaveProfile);

        // Listener for save button
        btnSaveProfile.setOnClickListener(v -> {
            // Get the info that was entered by the user
            String firstName = editTextFirstName.getText().toString();
            String lastName = editTextLastName.getText().toString();
            String email = editTextEmail.getText().toString();
            String phone = editTextPhone.getText().toString();
            boolean useGeolocation = switchGeolocation.isChecked();

            // Validate input fields
            if (firstName.isEmpty()) {
                editTextFirstName.setError("First name is required");
                editTextFirstName.requestFocus();
                return;
            }
            if (lastName.isEmpty()) {
                editTextLastName.setError("Last name is required");
                editTextLastName.requestFocus();
                return;
            }
            if (email.isEmpty()) {
                editTextEmail.setError("Email is required");
                editTextEmail.requestFocus();
                return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                editTextEmail.setError("Enter a valid email");
                editTextEmail.requestFocus();
                return;
            }
            if (phone.isEmpty()) {
                editTextPhone.setError("Phone number is required");
                editTextPhone.requestFocus();
                return;
            }
            if (!Patterns.PHONE.matcher(phone).matches()) {
                editTextPhone.setError("Enter a valid phone number");
                editTextPhone.requestFocus();
                return;
            }

            // Create a new Entrant object with the entered information
            Entrant newEntrant = new Entrant(userHardwareID, firstName, lastName, email, phone, useGeolocation);

            // Save the new Entrant object to the database
            db.setUserData(userHardwareID, newEntrant);

            // Switch to the Entrant activity
            goToEntrant(newEntrant);
        });
    }

    /**
     * Shows the Organizer profile creation page, limiting inputs to name fields.
     * On save, creates an Organizer, stores it in the database, and routes to the Organizer activity.
     */
    private void showProfileInformationPageOrganizer() {
        // set the view to the profile information page for entrants
        // set the view to the profile information page for entrants
        setContentView(R.layout.profile_information_page);

        // Get references to XML elements
        EditText editTextFirstName = findViewById(R.id.etFirstName);
        EditText editTextLastName = findViewById(R.id.etLastName);
        EditText editTextEmail = findViewById(R.id.etEmail);
        EditText editTextPhone = findViewById(R.id.etPhone);
        Switch switchGeolocation = findViewById(R.id.switchGeo);
        Button btnSaveProfile = findViewById(R.id.btnSaveProfile);
        LinearLayout firstNameLinearLayout = findViewById(R.id.firstNameRow);
        LinearLayout lastNameLinearLayout = findViewById(R.id.lastNameRow);
        LinearLayout emailLinearLayout = findViewById(R.id.emailRow);
        LinearLayout phoneLinearLayout = findViewById(R.id.phoneRow);
        LinearLayout geolocationLinearLayout = findViewById(R.id.geolocationRow);

        // hide the email, phone, and geolocation feilds
        emailLinearLayout.setVisibility(View.GONE);
        phoneLinearLayout.setVisibility(View.GONE);
        geolocationLinearLayout.setVisibility(View.GONE);

        btnSaveProfile.setOnClickListener(v -> {
            // Get the info that was entered by the user
            String firstName = editTextFirstName.getText().toString();
            String lastName = editTextLastName.getText().toString();

            // Validate input fields
            if (firstName.isEmpty()) {
                editTextFirstName.setError("First name is required");
                editTextFirstName.requestFocus();
                return;
            }
            if (lastName.isEmpty()) {
                editTextLastName.setError("Last name is required");
                editTextLastName.requestFocus();
                return;
            }

            // Create a new Organizer object with the entered information
            Organizer newOrganizer = new Organizer(userHardwareID, firstName, lastName);

            // Save the new Entrant object to the database
            db.setUserData(userHardwareID, newOrganizer);

            // Switch to the Entrant activity
            goToOrganizer(newOrganizer);
        });
    }

    /**
     * Used to switch to the Entrant activity from the main activity.
     * Sends along the user object for the activity to use
     * @param curUser
     */
    private void goToEntrant(User curUser) {
        // intent is used to switch activites
        Intent intent = new Intent(this, EntrantActivity.class);
        // add user object to intent
        intent.putExtra("entrantUser", curUser);
        // Actually switch
        startActivity(intent);
        finish();
    }

    /**
     * Used to switch to the Organizer activity from the main activity.
     * Sends along the user object for the activity to use
     * @param curUser
     */
    private void goToOrganizer(User curUser) {
        // intent is used to switch activites
        Intent intent = new Intent(this, OrganizerActivity.class);
        // add user object to intent
        intent.putExtra("organizerUser", curUser);
        // Actually switch
        startActivity(intent);
        finish();
    }

    /**
     * Used to switch to the Administrator activity from the main activity.
     * Sends along the user object for the activity to use
     * @param curUser
     */
    private void goToAdministrator(User curUser) {
        // intent is used to switch activites
        Intent intent = new Intent(this, AdminActivity.class);
        // add user object to intent
        intent.putExtra("adminUser", curUser);
        // Actually switch
        startActivity(intent);
        finish();
    }

    /**
     * This function will set the database information for the current hardware ID to create
     * a pre-existing Entrant in the database! FOR TESTING ONLY!!
     *
     * @param db
     * @return
     */
    private Task<Void> setCurrentToEntrant(Database db) {
        Entrant defaultEntrant = new Entrant(
                userHardwareID,
                "John",
                "Doe",
                "John.doe@email.com");

        return db.setUserData(userHardwareID, defaultEntrant);
    }

    /**
     * This function will set the database information for the current hardware ID to create
     * a pre-existing Organizer in the database! FOR TESTING ONLY!!
     *
     * @param db
     * @return
     */
    private Task<Void> setCurrentToOrganizer(Database db) {
        Organizer defaultOrganizer = new Organizer(
                userHardwareID,
                "John",
                "Doe");

        return db.setUserData(userHardwareID, defaultOrganizer);
    }

    /**
     * This function will set the database information for the current hardware ID to create
     * a pre-existing Administrator in the database! FOR TESTING ONLY!!
     *
     * @param db
     * @return
     */
    private Task<Void> setCurrentToAdministrator(Database db) {
        Administrator defaultAdministrator = new Administrator(
                userHardwareID,
                "John",
                "Doe");

        return db.setUserData(userHardwareID, defaultAdministrator);
    }
}
