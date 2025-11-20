package com.example.zypherevent;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;

import com.example.zypherevent.userTypes.Administrator;
import com.example.zypherevent.userTypes.Entrant;
import com.example.zypherevent.userTypes.Organizer;
import com.example.zypherevent.userTypes.User;

import androidx.navigation.ui.AppBarConfiguration;
import androidx.appcompat.app.AppCompatActivity;

import com.example.zypherevent.databinding.ActivityMainBinding;
import com.example.zypherevent.userTypes.UserType;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.GeoPoint;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
//        userHardwareID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        userHardwareID = "ElliotTestOrg1";

        Log.d("MainActivityLogic", "User hardware id: " + userHardwareID);

        // For testing, to create a user entry in the database, uncomment one of the following!
//         setCurrentToEntrant(db);
//         setCurrentToOrganizer(db);
//        setCurrentToAdministrator(db);
        // By leaving these commented, if there is not a pre-existing entry for the hwid in the
        // database, then you will be prompted as a new user!

        // Create a sample database -----  CAREFUL PLEASE!!!!!!
//        createSampleDatabase();

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
     * Populates the database with sample users, events, and notifications.
     * This method creates a set of organizers, entrants, and an administrator, assigns locations
     * to entrants, and persists them to the database. It then creates three sample events with
     * predefined registration windows, participants in various waitlisted, accepted, and declined
     * states, and updates each entrant's registered event history and each organizer's created
     * events list. Finally, it generates a small set of test notifications addressed to a sample
     * entrant. All event and notification records use IDs obtained from the database's unique ID
     * generators.
     */
    private void createSampleDatabase() {

        Database db = new Database();

        // --- Organizers ---
        Organizer organizer1 = new Organizer("organizer1", "John", "Doe");
        Organizer organizer2 = new Organizer("organizer2", "Jane", "Daren");
        Organizer organizer3 = new Organizer("organizer3", "June", "Dunley");

        db.setUserData(organizer1.getHardwareID(), organizer1);
        db.setUserData(organizer2.getHardwareID(), organizer2);
        db.setUserData(organizer3.getHardwareID(), organizer3);

        // --- Entrants ---
        Entrant entrant1  = new Entrant("entrant1",  "Joshua",   "Smith",   "joshua.smith@email.com");
        Entrant entrant2  = new Entrant("entrant2",  "Jacob",    "McMann",  "jacob.mcmann@email.com",  "7801231234", true);
        Entrant entrant3  = new Entrant("entrant3",  "Emily",    "Turner",  "emily.turner@email.com");
        Entrant entrant4  = new Entrant("entrant4",  "Michael",  "Andrews", "michael.andrews@email.com", "5875550192", false);
        Entrant entrant5  = new Entrant("entrant5",  "Sophie",   "Lee",     "sophie.lee@email.com", "5875570192", true);
        Entrant entrant6  = new Entrant("entrant6",  "Daniel",   "Harris",  "daniel.harris@email.com", "4039914421", true);
        Entrant entrant7  = new Entrant("entrant7",  "Victoria", "Nguyen",  "victoria.nguyen@email.com", "123123123", true);
        Entrant entrant8  = new Entrant("entrant8",  "Ethan",    "Marshall","ethan.marshall@email.com", "8257129943", true);
        Entrant entrant9  = new Entrant("entrant9",  "Olivia",   "Khan",    "olivia.khan@email.com", "123123123123123", true);
        Entrant entrant10 = new Entrant("entrant10", "Liam",     "Fraser",  "liam.fraser@email.com", "7802239811", true);

        entrant1.setLocation(new GeoPoint(53.5552, -113.6284));
        entrant2.setLocation(new GeoPoint(53.5927, -113.4219));
        entrant3.setLocation(new GeoPoint(53.5203, -113.3811));
        entrant4.setLocation(new GeoPoint(53.5058, -113.5627));
        entrant5.setLocation(new GeoPoint(53.5739, -113.4972));
        entrant6.setLocation(new GeoPoint(53.5334, -113.6715));
        entrant7.setLocation(new GeoPoint(53.6108, -113.5582));
        entrant8.setLocation(new GeoPoint(53.4947, -113.4479));
        entrant9.setLocation(new GeoPoint(53.5611, -113.3563));
        entrant10.setLocation(new GeoPoint(53.5289, -113.5994));

        // --- Admin ---
        Administrator admin1 = new Administrator("administrator1", "Arnold", "Nimda");

        db.setUserData(entrant1.getHardwareID(), entrant1);
        db.setUserData(entrant2.getHardwareID(), entrant2);
        db.setUserData(entrant3.getHardwareID(), entrant3);
        db.setUserData(entrant4.getHardwareID(), entrant4);
        db.setUserData(entrant5.getHardwareID(), entrant5);
        db.setUserData(entrant6.getHardwareID(), entrant6);
        db.setUserData(entrant7.getHardwareID(), entrant7);
        db.setUserData(entrant8.getHardwareID(), entrant8);
        db.setUserData(entrant9.getHardwareID(), entrant9);
        db.setUserData(entrant10.getHardwareID(), entrant10);
        db.setUserData(admin1.getHardwareID(), admin1);

        // --- Event 1 ---
        db.getUniqueEventID().addOnSuccessListener(uniqueEventID -> {
            Event event1 = new Event(
                    uniqueEventID,
                    "Group Swimming",
                    "Learn to swim at the new Edmonton Swimming School!",
                    new Date("December 19, 2025 00:00:00"),
                    "Edmonton Swimming School",
                    new Date("November 1, 2025 00:00:00"),
                    new Date("December 10, 2025 00:00:00"),
                    organizer1.getHardwareID(),
                    "https://upload.wikimedia.org/wikipedia/commons/a/a7/40._Schwimmzonen-_und_Mastersmeeting_Enns_2017_100m_Brust_Herren_USC_Traun-9897.jpg",
                    false
            );

            // These calls will succeed as long as today is within the reg window.
            event1.addEntrantToWaitList(entrant1.getHardwareID());
            event1.addEntrantToWaitList(entrant2.getHardwareID());
            event1.addEntrantToWaitList(entrant3.getHardwareID());
            event1.addEntrantToWaitList(entrant4.getHardwareID());
            event1.addEntrantToWaitList(entrant5.getHardwareID());

            event1.addEntrantToInvitedList(entrant6.getHardwareID());

            event1.addEntrantToAcceptedList(entrant7.getHardwareID());

            event1.addEntrantToDeclinedList(entrant8.getHardwareID());

            entrant1.addEventToRegisteredEventHistory(event1.getUniqueEventID());
            entrant2.addEventToRegisteredEventHistory(event1.getUniqueEventID());
            entrant3.addEventToRegisteredEventHistory(event1.getUniqueEventID());
            entrant4.addEventToRegisteredEventHistory(event1.getUniqueEventID());
            entrant5.addEventToRegisteredEventHistory(event1.getUniqueEventID());
            entrant6.addEventToRegisteredEventHistory(event1.getUniqueEventID());
            entrant7.addEventToRegisteredEventHistory(event1.getUniqueEventID());
            entrant8.addEventToRegisteredEventHistory(event1.getUniqueEventID());

            organizer1.addCreatedEvent(uniqueEventID);

            db.setUserData(entrant1.getHardwareID(), entrant1);
            db.setUserData(entrant2.getHardwareID(), entrant2);
            db.setUserData(entrant3.getHardwareID(), entrant3);
            db.setUserData(entrant4.getHardwareID(), entrant4);
            db.setUserData(entrant5.getHardwareID(), entrant5);
            db.setUserData(entrant6.getHardwareID(), entrant6);
            db.setUserData(entrant7.getHardwareID(), entrant7);
            db.setUserData(entrant8.getHardwareID(), entrant8);

            db.setUserData(organizer1.getHardwareID(), organizer1);

            db.setEventData(uniqueEventID, event1);
        });

        // --- Event 2 ---
        db.getUniqueEventID().addOnSuccessListener(uniqueEventID -> {
            Event event2 = new Event(
                    uniqueEventID,
                    "Chess Club Meeting",
                    "Playing chess, talking about chess, making friends!",
                    new Date("December 1, 2025 00:00:00"),
                    "Vancouver Chess Stadium",
                    new Date("November 1, 2025 00:00:00"),
                    new Date("November 29, 2025 00:00:00"),
                    organizer2.getHardwareID(),
                    "https://upload.wikimedia.org/wikipedia/commons/6/6f/ChessSet.jpg",
                    false
            );

            event2.addEntrantToInvitedList(entrant1.getHardwareID());

            event2.addEntrantToWaitList(entrant5.getHardwareID());
            event2.addEntrantToWaitList(entrant6.getHardwareID());
            event2.addEntrantToWaitList(entrant7.getHardwareID());
            event2.addEntrantToWaitList(entrant8.getHardwareID());
            event2.addEntrantToWaitList(entrant9.getHardwareID());

            entrant1.addEventToRegisteredEventHistory(event2.getUniqueEventID());
            entrant5.addEventToRegisteredEventHistory(event2.getUniqueEventID());
            entrant6.addEventToRegisteredEventHistory(event2.getUniqueEventID());
            entrant7.addEventToRegisteredEventHistory(event2.getUniqueEventID());
            entrant8.addEventToRegisteredEventHistory(event2.getUniqueEventID());
            entrant9.addEventToRegisteredEventHistory(event2.getUniqueEventID());

            organizer2.addCreatedEvent(uniqueEventID);

            db.setUserData(entrant1.getHardwareID(), entrant1);
            db.setUserData(entrant5.getHardwareID(), entrant5);
            db.setUserData(entrant6.getHardwareID(), entrant6);
            db.setUserData(entrant7.getHardwareID(), entrant7);
            db.setUserData(entrant8.getHardwareID(), entrant8);
            db.setUserData(entrant9.getHardwareID(), entrant9);

            db.setUserData(organizer2.getHardwareID(), organizer2);

            db.setEventData(uniqueEventID, event2);
        });

        // --- Event 3 ---
        db.getUniqueEventID().addOnSuccessListener(uniqueEventID -> {
            Event event3 = new Event(
                    uniqueEventID,
                    "Geocache Searching!",
                    "Come find geo-caches around Edmonton!",
                    new Date("December 25, 2025 00:00:00"),
                    "Edmonton River Valley",
                    new Date("November 1, 2025 00:00:00"),
                    new Date("December 20, 2025 00:00:00"),
                    organizer3.getHardwareID(),
                    true
            );

            event3.addEntrantToAcceptedList(entrant5.getHardwareID());
            event3.addEntrantToAcceptedList(entrant6.getHardwareID());
            event3.addEntrantToAcceptedList(entrant7.getHardwareID());
            event3.addEntrantToAcceptedList(entrant8.getHardwareID());
            event3.addEntrantToAcceptedList(entrant9.getHardwareID());

            event3.addEntrantToInvitedList(entrant10.getHardwareID());

            event3.addEntrantToDeclinedList(entrant1.getHardwareID());

            entrant1.addEventToRegisteredEventHistory(event3.getUniqueEventID());
            entrant5.addEventToRegisteredEventHistory(event3.getUniqueEventID());
            entrant6.addEventToRegisteredEventHistory(event3.getUniqueEventID());
            entrant7.addEventToRegisteredEventHistory(event3.getUniqueEventID());
            entrant8.addEventToRegisteredEventHistory(event3.getUniqueEventID());
            entrant9.addEventToRegisteredEventHistory(event3.getUniqueEventID());
            entrant10.addEventToRegisteredEventHistory(event3.getUniqueEventID());

            organizer3.addCreatedEvent(uniqueEventID);

            db.setUserData(entrant1.getHardwareID(), entrant1);
            db.setUserData(entrant5.getHardwareID(), entrant5);
            db.setUserData(entrant6.getHardwareID(), entrant6);
            db.setUserData(entrant7.getHardwareID(), entrant7);
            db.setUserData(entrant8.getHardwareID(), entrant8);
            db.setUserData(entrant9.getHardwareID(), entrant9);
            db.setUserData(entrant10.getHardwareID(), entrant10);

            db.setUserData(organizer3.getHardwareID(), organizer3);

            db.setEventData(uniqueEventID, event3);
        });

        // --- Notifications ---
        db.getUniqueNotificationID().addOnSuccessListener(notificationID -> {
            Notification notification1 = new Notification(
                    notificationID,
                    organizer1.getHardwareID(),
                    entrant1.getHardwareID(),
                    "Test Notification 1",
                    "Notification Body"
            );
            db.setNotificationData(notificationID, notification1);
        });

        db.getUniqueNotificationID().addOnSuccessListener(notificationID -> {
            Notification notification2 = new Notification(
                    notificationID,
                    organizer2.getHardwareID(),
                    entrant1.getHardwareID(),
                    "Test Notification 2",
                    "Notification Body"
            );
            db.setNotificationData(notificationID, notification2);
        });

        db.getUniqueNotificationID().addOnSuccessListener(notificationID -> {
            Notification notification3 = new Notification(
                    notificationID,
                    organizer3.getHardwareID(),
                    entrant1.getHardwareID(),
                    "Test Notification 3",
                    "Notification Body"
            );
            db.setNotificationData(notificationID, notification3);
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
            // Phone number is optional!!!
//            if (phone.isEmpty()) {
//                editTextPhone.setError("Phone number is required");
//                editTextPhone.requestFocus();
//                return;
//            }
            if (phone != null && !phone.isEmpty()) {
                if (!Patterns.PHONE.matcher(phone).matches()) {
                    editTextPhone.setError("Enter a valid phone number");
                    editTextPhone.requestFocus();
                    return;
                }
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
