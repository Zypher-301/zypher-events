package com.example.zypherevent;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;

import com.example.zypherevent.userTypes.User;

import androidx.navigation.ui.AppBarConfiguration;
import androidx.appcompat.app.AppCompatActivity;

import com.example.zypherevent.databinding.ActivityMainBinding;
import com.example.zypherevent.userTypes.UserType;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private Database db;

    private String userHardwareID;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize database
        db = new Database();

        // Get hardare ID from user's device
        userHardwareID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        Log.d("MainActivityLogic", "User hardware id: " + userHardwareID);

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

    private void showProfileInformationPage() {
        // Set the view to the startup role declaration page
        setContentView(R.layout.startup_role_declaration_page);

        // Add the buttons
        Button btnEntrant = findViewById(R.id.btnEntrant);
        Button btnOrganizer = findViewById(R.id.btnOrganizer);

        // Listen for entrant click
        btnEntrant.setOnClickListener(v -> {
            Log.d("MainActivity", "Entrant button clicked");
            // start the profile information activity (maybe fragment) as entrant
        });

        // Listen for Organizer click
        btnOrganizer.setOnClickListener(v -> {
            Log.d("MainActivity", "Organizer button clicked");
            // start the profile information activity (maybe fragment) as organizer
        });
    }

    private void goToEntrant(User curUser) {
//        // intent is used to switch activites
//        Intent intent = new Intent(this, EntrantActivity.class);
//        // add user object to intent
//        intent.putExtra("entrantUser", curUser);
//        // Actually switch
//        startActivity(intent);
//        finish();
    }

    private void goToOrganizer(User curUser) {
//        // intent is used to switch activites
//        Intent intent = new Intent(this, OrganizerActivity.class);
//        // add user object to intent
//        intent.putExtra("organizerUser", curUser);
//        // Actually switch
//        startActivity(intent);
//        finish();
    }

    private void goToAdministrator(User curUser) {
        // intent is used to switch activites
        Intent intent = new Intent(this, AdminActivity.class);
        // add user object to intent
        intent.putExtra("adminUser", curUser);
        // Actually switch
        startActivity(intent);
        finish();
    }
}