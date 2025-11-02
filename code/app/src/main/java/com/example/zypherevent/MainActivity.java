package com.example.zypherevent;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;

import com.example.zypherevent.userTypes.Administrator;
import com.example.zypherevent.userTypes.Entrant;
import com.example.zypherevent.userTypes.Organizer;
import com.example.zypherevent.userTypes.User;

import androidx.navigation.ui.AppBarConfiguration;
import androidx.appcompat.app.AppCompatActivity;

import com.example.zypherevent.databinding.ActivityMainBinding;
import com.example.zypherevent.userTypes.UserType;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity {
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private Database db;
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
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
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
     * Used to switch to the profile information view from inside the main activity.
     */
    private void showProfileInformationPage() {
        // Set the view to the startup role declaration page
        setContentView(R.layout.startup_role_declaration_page);

        // Add the buttons
        Button btnEntrant = findViewById(R.id.btnEntrant);
        Button btnOrganizer = findViewById(R.id.btnOrganizer);

        // Listen for entrant click
        btnEntrant.setOnClickListener(v -> {
            Log.d("MainActivity", "Entrant button clicked");

            // set the view to the profile information page for entrants

        });

        // Listen for Organizer click
        btnOrganizer.setOnClickListener(v -> {
            Log.d("MainActivity", "Organizer button clicked");

            // set the view to the profile information page for entrants

        });
    }

    /**
     * Used to switch to the Entrant activity from the main activity.
     * Sends along the user object for the activity to use
     * @param curUser
     */
    private void goToEntrant(User curUser) {
//        // intent is used to switch activites
//        Intent intent = new Intent(this, EntrantActivity.class);
//        // add user object to intent
//        intent.putExtra("entrantUser", curUser);
//        // Actually switch
//        startActivity(intent);
//        finish();
    }

    /**
     * Used to switch to the Organizer activity from the main activity.
     * Sends along the user object for the activity to use
     * @param curUser
     */
    private void goToOrganizer(User curUser) {
//        // intent is used to switch activites
//        Intent intent = new Intent(this, OrganizerActivity.class);
//        // add user object to intent
//        intent.putExtra("organizerUser", curUser);
//        // Actually switch
//        startActivity(intent);
//        finish();
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