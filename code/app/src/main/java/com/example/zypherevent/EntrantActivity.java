package com.example.zypherevent;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import com.example.zypherevent.userTypes.UserType;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
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

    /** Fused location client for getting current location. */
    private FusedLocationProviderClient fused;

    /** Activity result launcher for requesting location permission. */
    private ActivityResultLauncher<String> requestLocationPerm;

    private Runnable pendingOnSuccess;
    private Runnable pendingOnFail;

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

        // create fused location client
        fused = LocationServices.getFusedLocationProviderClient(this);

        requestLocationPerm = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        fetchOnceAndSaveLocation();   // after save, we’ll run pendingOnSuccess
                    } else {
                        if (pendingOnFail != null) pendingOnFail.run();
                        pendingOnFail = null;
                        pendingOnSuccess = null;
                        Log.w("EntrantActivity", "Location permission denied");
                    }
                }
        );

        // Get the entrant user object that was passed along from MainActivity
        entrantUser = (Entrant) getIntent().getSerializableExtra("entrantUser");
        Log.d("EntrantActivityLogic", "Got Entrant from MainActivity");

        // If user opted in and we don’t have a location yet → get one
        if (entrantUser.getUseGeolocation() && entrantUser.getLocation() == null) {
            if (hasLocationPermission()) {
                fetchOnceAndSaveLocation();
            } else {
                requestLocationPerm.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }

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
     * Requests the entrant's location from the device if it is missing from their profile.
     * If geolocation is enabled for the current entrant and no saved location exists,
     * this method checks for location permission. If granted, it fetches the location once
     * and saves it to Firestore. If permission is missing, it launches a permission request.
     * Otherwise, the success callback is run immediately since no action is required.
     *
     * @param onSuccess callback to run after a successful location update
     * @param onFail    callback to run if the location could not be fetched or saved
     */
    public void requestEntrantLocationIfMissing(@Nullable Runnable onSuccess,
                                                @Nullable Runnable onFail) {
        // save callbacks for use after permission/result
        pendingOnSuccess = onSuccess;
        pendingOnFail = onFail;

        if (entrantUser.getUseGeolocation() && entrantUser.getLocation() == null) {
            if (hasLocationPermission()) {
                fetchOnceAndSaveLocation();   // will call pendingOnSuccess later
            } else {
                requestLocationPerm.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        } else {
            // Nothing to do; call success immediately if you want
            if (pendingOnSuccess != null) pendingOnSuccess.run();
            pendingOnSuccess = null;
            pendingOnFail = null;
        }
    }

    /**
     * Checks whether the app currently has either fine or coarse location permission.
     *
     * @return true if at least one location permission is granted. false otherwise
     */
    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Attempts to retrieve the device's current location once and save it to the entrant profile.
     * If a fresh fix is unavailable, falls back to the last known location. Updates the entrant's
     * Firestore record with the retrieved coordinates and triggers success or failure callbacks.
     */
    private void fetchOnceAndSaveLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission not granted.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Notify user that we’re starting the location fetch
        Toast.makeText(this, "Fetching your location, this may take a moment...", Toast.LENGTH_SHORT).show();

        fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener(loc -> {
                    if (loc != null) {
                        saveEntrantLocation(loc);
                    } else {
                        // Fallback to last known if no fresh fix available
                        fused.getLastLocation()
                                .addOnSuccessListener(last -> {
                                    if (last != null) {
                                        Toast.makeText(this, "Location found. Saving to profile!", Toast.LENGTH_SHORT).show();
                                        saveEntrantLocation(last);
                                    } else {
                                        Toast.makeText(this, "Unable to get your location. Please try again later.", Toast.LENGTH_LONG).show();
                                        Log.w("EntrantActivity", "No location available");
                                    }
                                })
                                .addOnFailureListener(e -> Log.e("EntrantActivity", "LastLocation failed", e));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("EntrantActivity", "getCurrentLocation failed", e);
                    Toast.makeText(this, "Error getting location: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Saves the entrant's location to Firestore and updates their profile object.
     *
     * @param loc the retrieved Android android.location.Location to save
     */
    private void saveEntrantLocation(android.location.Location loc) {
        com.google.firebase.firestore.GeoPoint gp =
                new com.google.firebase.firestore.GeoPoint(loc.getLatitude(), loc.getLongitude());

        entrantUser.setLocation(gp);

        db.setUserData(entrantUser.getHardwareID(), entrantUser)
                .addOnSuccessListener(v -> {
                    Log.d("EntrantActivity", "Saved location for entrant");
                    if (pendingOnSuccess != null) pendingOnSuccess.run();
                    pendingOnSuccess = null;
                    pendingOnFail = null;
                })
                .addOnFailureListener(e -> {
                    Log.e("EntrantActivity", "Failed to save location", e);
                    if (pendingOnFail != null) pendingOnFail.run();
                    pendingOnSuccess = null;
                    pendingOnFail = null;
                });
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