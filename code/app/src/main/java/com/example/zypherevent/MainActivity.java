package com.example.zypherevent;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Menu;

import com.example.zypherevent.userTypes.Administrator;
import com.example.zypherevent.userTypes.Entrant;
import com.example.zypherevent.userTypes.Organizer;
import com.example.zypherevent.userTypes.User;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.example.zypherevent.databinding.ActivityMainBinding;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    // should NEVER be run on the main thread!
    public void informalTesting() throws ExecutionException, InterruptedException {

        Log.d("informalTesting", "Hello world!");

        Database myDatabase = new Database();

        // Create some Users
        Entrant aaron = new Entrant(
                "TestingHardwareID1",
                "Aaron",
                "Mramba",
                "aaron@email.com",
                "780-123-1234");

        Entrant elliot = new Entrant(
                "TestingHardwareID2",
                "Elliot",
                "Chrystal",
                "elliot@email.com");

        Organizer britney = new Organizer(
                "TestingHardwareID3",
                "Britney",
                "Kunchidi");

        Organizer arunavo = new Organizer(
                "TestingHardwareID4",
                "Arunavo",
                "Dutta");

        Administrator tom = new Administrator(
                "TestingHardwareID5",
                "Tom",
                "Yang");

        Administrator noor = new Administrator(
                "TestingHardwareID6",
                "Noordeep",
                "Behla");

        // Add users to database, page name is hardwareID and page content is the User object
        myDatabase.setUserData(aaron.getHardwareID(), aaron);
        myDatabase.setUserData(elliot.getHardwareID(), elliot);
        myDatabase.setUserData(britney.getHardwareID(), britney);
        myDatabase.setUserData(noor.getHardwareID(), noor);
        myDatabase.setUserData(tom.getHardwareID(), tom);
        myDatabase.setUserData(arunavo.getHardwareID(), arunavo);

        // Create some events now!

        LocalDateTime now = null;

        // LocalDateTime needs this for some reason?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            now = LocalDateTime.now();
        }

        // create event with a poster
        // this asks the database for an event ID that has not yet been used
        String secretPosterUrl = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEjgO2V_jJiVSFSb4ZX4AN6hDpNER4pWuqj89_MBRyEMQmyxGwL3S6fIcVSqLUg2I_qIJ0dKZ_U6oo5Tr5AGKKWwGRUC4ZG_ewRq1MS8oUJLUDxTsex_4U9kSnOioqflkJ6oyfYACL6ozFo/s1600/Todd_thumb.jpg";
        Long uniqueEventID = Tasks.await(myDatabase.getUniqueEventID());
        Event secretMeeting = new Event(
                uniqueEventID,
                "Arunavo's Secret Meeting",
                "This is a test event",
                now.toString(),                 // event startTime
                "Dark side of the Moon",        // event location
                now.toString(),                 // event registrationStartTime
                now.toString(),                 // event registrationEndTime
                arunavo.getHardwareID(),        // event Organizer Hardware ID
                secretPosterUrl);               // event poster URL

        // create event without a poster
        // this asks the database for an event ID that has not yet been used

        uniqueEventID = Tasks.await(myDatabase.getUniqueEventID());
        Event movieReview = new Event(
                uniqueEventID,
                "Britney's Movie Discussion",
                "This is a test event",
                now.toString(),                 // event startTime
                "Metro Cinema Theatre",         // event location
                now.toString(),                 // event registrationStartTime
                now.toString(),                 // event registrationEndTime
                britney.getHardwareID());       // event Organizer Hardware ID

        // Add the Events to the database, page name is eventID and page content is the Event object
        myDatabase.setEventData(movieReview.getUniqueEventID(), movieReview);
        myDatabase.setEventData(secretMeeting.getUniqueEventID(), secretMeeting);

        //Test data reading!
        myDatabase.getUser("TestingHardwareID2").addOnSuccessListener(user -> {
            // Code here is only executed once the user is fetched
            Log.d("informalTesting", "Fetched user - " +
                    user.getFirstName() +
                    " " +
                    user.getLastName());
        }); // you can also add a .addOnFailureListener() to handle errors

        myDatabase.getEvent(uniqueEventID).addOnSuccessListener(event -> {
            // Code here is only executed once the user is fetched
            Log.d("informalTesting", "Fetched event - " +
                    event.getEventName() +
                    " " +
                    event.getLocation());
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // run some informal tests on database (on a thread other than main!!)
//        new Thread(() -> {
//            try {
//                Log.d("informalTesting", "Starting informal testing...");
//                informalTesting();
//            } catch (ExecutionException | InterruptedException e) {
//                Log.d("informalTesting", "Exception in informalTesting!!");
//                throw new RuntimeException(e);
//            }
//        }).start();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}