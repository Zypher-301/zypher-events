package com.example.zypherevent;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.example.zypherevent.databinding.AdminMainBinding;
import com.example.zypherevent.databinding.EntrantMainBinding;
import com.example.zypherevent.databinding.OrganizerMainBinding;
import com.example.zypherevent.userTypes.Administrator;
import com.example.zypherevent.userTypes.Entrant;
import com.example.zypherevent.userTypes.Organizer;
import com.example.zypherevent.userTypes.User;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * @author Elliot Chrystal
 *
 * copy of AdminActivity by Arunavo
 *
 * @version 1.0
 */
public class OrganizerActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private OrganizerMainBinding binding;
    private Database db;
    private Organizer organizerUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // create new instance of database
        db = new Database();

        // Get the organizer user object that was passed along from MainActivity
        organizerUser = (Organizer) getIntent().getSerializableExtra("organizerUser");
        Log.d("OrganizerActivityLogic", "Got Organizer from MainActivity");

        // Inflate the organizer layout using its binding class (organizer_main.xml)
        binding = OrganizerMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up the toolbar from the included organizer_bar_main.xml (organizer_main.xml)
        setSupportActionBar(binding.organizerBarMain.toolbar);

        // Initialize DrawerLayout and NavigationView (organizer_main.xml)
        DrawerLayout drawer = binding.organizerDrawerLayout;
        NavigationView navigationView = binding.organizerNavView;

        // Configure the top-level destinations for the organizer drawer menu (organizer_main_drawer.xml)
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_my_events, R.id.nav_events_map)
                .setOpenableLayout(drawer)
                .build();

        // Find the NavController using the ID from content_organizer.xml
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_organizer);

        // Set up the ActionBar and NavigationView with the NavController
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Handle the "Up" button by using the NavController (content_organizer.xml)
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_organizer);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}