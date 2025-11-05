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
import com.example.zypherevent.userTypes.Administrator;
import com.example.zypherevent.userTypes.Entrant;
import com.example.zypherevent.userTypes.User;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * @author Elliot Chrystal
 * @version 1.0
 *
 * Activity for entrant-facing navigation and UI. This activity receives an Entrant
 * instance from MainActivity, initializes the entrant layout and toolbar, and
 * configures the Navigation Component with a drawer for top-level destinations.
 */
public class EntrantActivity extends AppCompatActivity {

    /** Navigation configuration for top-level destinations in the drawer. */
    private AppBarConfiguration mAppBarConfiguration;

    /** ViewBinding for the entrant main layout. */
    private EntrantMainBinding binding;

    /** Reference to the application database interface. */
    private Database db;

    /** The entrant user passed in from MainActivity. */
    private Entrant entrantUser;

    /**
     * Initializes the activity, inflates the layout, and sets up navigation.
     *
     * @param savedInstanceState if non-null, the activity is being re-constructed from a previous state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // create new instance of database
        db = new Database();

        // Get the entrant user object that was passed along from MainActivity
        entrantUser = (Entrant) getIntent().getSerializableExtra("entrantUser");
        Log.d("EntrantActivityLogic", "Got Entrant from MainActivity");

        // Inflate the entrant layout using its binding class (entrant_main.xml)
        binding = EntrantMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up the toolbar from the included entrant_bar_main.xml (entrant_main.xml)
        setSupportActionBar(binding.entrantBarMain.toolbar);

        // Initialize DrawerLayout and NavigationView (entrant_main.xml)
        DrawerLayout drawer = binding.entrantDrawerLayout;
        NavigationView navigationView = binding.entrantNavView;

        // Configure the top-level destinations for the entrant drawer menu (entrant_main_drawer.xml)
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_all_events, R.id.nav_joined_events, R.id.nav_qr_scanner, R.id.nav_notifications, R.id.nav_settings)
                .setOpenableLayout(drawer)
                .build();

        // Find the NavController using the ID from content_entrant.xml
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_entrant);

        // Set up the ActionBar and NavigationView with the NavController
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
    }

    /**
     * Returns the entrant that was passed to this activity from MainActivity.
     *
     * @return the current Entrant for this session
     */
    public Entrant getEntrantUser() {
        return entrantUser;
    }

    /**
     * Handles the ActionBar's "Up" button behavior using the NavController.
     *
     * @return true if navigation up was handled; otherwise defers to the superclass
     */
    @Override
    public boolean onSupportNavigateUp() {
        // Handle the "Up" button by using the NavController (content_entrant.xml)
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_entrant);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}