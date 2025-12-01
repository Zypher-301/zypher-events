package com.example.zypherevent;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.zypherevent.databinding.EntrantMainBinding;
import com.example.zypherevent.notifications.NotificationService;
import com.example.zypherevent.userTypes.Entrant;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.navigation.NavigationView;

/**
 * @author Elliot Chrystal
 * @author Tom Yang (Added functionality for notification listener)
 * @version 2.0
 * @see NotificationService
 *
 * Activity for entrant-facing navigation and UI. This activity receives an Entrant
 * instance from MainActivity, initializes the entrant layout and toolbar, and
 * configures the Navigation Component with a drawer for top-level destinations.
 * <p>
 * Also manages the NotificationService lifecycle, which provides real-time event
 * notifications to entrants when enabled by the user. The service runs as a foreground
 * service and listens for new notifications from Firestore.
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

    /** Callbacks for use after permission/result. */
    private Runnable pendingOnSuccess;
    private Runnable pendingOnFail;

    /** Navigation drawer components. */
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private NavController navController;

    /** Notification Service */
    private NotificationService notificationService;
    private boolean serviceBound = false;

    public NotificationService getNotificationService() {
        return notificationService;
    }

    /**
     * ServiceConnection callback for binding to the NotificationService.
     * When connected, starts listening for notifications if the user has notifications enabled.
     */
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            NotificationService.LocalBinder binder = (NotificationService.LocalBinder) service;
            notificationService = binder.getService();
            serviceBound = true;

            if (entrantUser != null && entrantUser.getWantsNotifications()) {
                notificationService.startListeningForNotifications((entrantUser.getHardwareID()));
                Log.d("EntrantActivity", "Notification service started and listening");
            } else {
                Log.d("EntrantActivity", "Notification service started but not listening (user preference)");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            notificationService = null;
            Log.d("EntrantActivity", "Notification service disconnected");
        }
    };

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

        // Start notification service if user want notifications
        if (entrantUser != null && entrantUser.getWantsNotifications()) {
            startNotificationService();
            requestNotificationPermission();
        }

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
        drawerLayout = binding.entrantDrawerLayout;
        navigationView = binding.entrantNavView;

        // Configure the top-level destinations for the entrant drawer menu (entrant_main_drawer.xml)
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_all_events, R.id.nav_joined_events, R.id.nav_qr_scanner, R.id.nav_notifications, R.id.nav_settings)
                .setOpenableLayout(drawerLayout)
                .build();

        // Find the NavController using the ID from content_entrant.xml
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_entrant);

        // Set up the ActionBar and NavigationView with the NavController
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Show or hide the notifications navigation item based on the entrant's notification preferences
        showOrHideNotifications(entrantUser.getWantsNotifications());

        // Handle notification tap (if app was in background and not killed)
        handleNotificationIntent(getIntent());
    }

    /**
     * Show or hide the notifications navigation item based on the entrant's notification preferences
     *
     * @param show true to show the item, false to hide it
     */
    public void showOrHideNotifications(boolean show) {
        Menu menu = navigationView.getMenu();
        MenuItem notificationsItem = menu.findItem(R.id.nav_notifications);
        if (notificationsItem != null) {
            notificationsItem.setVisible(show);
        }
        // if the notifications page is hidden and currently displayed, redirect user
        if (!show &&
                navController.getCurrentDestination() != null &&
                navController.getCurrentDestination().getId() == R.id.nav_notifications) {
            navController.navigate(R.id.nav_all_events);
        }
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

    /**
     * Starts the notification service as a foreground service and binds to it.
     * This method is called when the user has notifications enabled in their preferences.
     * The service will display a persistent notification and listen for new event notifications.
     */
    private void startNotificationService() {
        Intent serviceIntent = new Intent(this, NotificationService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Stops the notification service and unbinds from it.
     * This method is called when the user disables notifications in settings.
     * The service will stop listening for notifications and the persistent notification will disappear.
     */
    private void stopNotificationService() {
        if (serviceBound) {
            if (notificationService != null) {
                notificationService.stopListeningForNotifications();
            }
            unbindService(serviceConnection);
            serviceBound = false;
        }

        Intent serviceIntent = new Intent(this, NotificationService.class);
        stopService(serviceIntent);
    }

    /**
     * Handles changes to the user's notification preference from the settings screen.
     * This method starts or stops the notification service based on the enabled flag,
     * updates the UI to show or hide the notifications menu item.
     *
     * @param enabled true if the user enabled notifications, false if disabled
     */
    public void onNotificationPreferenceChanged(boolean enabled) {
        showOrHideNotifications(enabled);

        if (enabled) {
            startNotificationService();
        } else {
            stopNotificationService();
        }

        entrantUser.setWantsNotifications(enabled);
    }

    /**
     * Called when a new intent is delivered to an already-running activity.
     * This handles notification taps when the app is already open.
     * Delegates to handleNotificationIntent to perform the actual navigation.
     *
     * @param intent The new intent that was used to start the activity
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Log.d("EntrantActivity", "onNewIntent called");
        handleNotificationIntent(intent);
    }

    /**
     * Handles intents from notification taps and navigates to the appropriate destination.
     * When a user taps on a notification, this method extracts the navigation destination
     * from the intent extras and navigates to it using the NavController.
     * Currently supports navigating to the notifications screen.
     *
     * @param intent The intent that launched or was passed to this activity
     */
    private void handleNotificationIntent(Intent intent) {
        // Safety checks
        if (intent == null) {
            Log.d("EntrantActivity", "Intent is null");
            return;
        }

        // Only proceed if there's a navigation destination
        if (!intent.hasExtra("navigate_to")) {
            Log.d("EntrantActivity", "No navigation extra - just opening app");
            return;
        }

        String destination = intent.getStringExtra("navigate_to");
        Log.d("EntrantActivity", "Navigation destination: " + destination);

        if ("notifications".equals(destination)) {
            // Check if user is loaded and wants notifications
            if (entrantUser == null) {
                Log.w("EntrantActivity", "entrantUser is null, delaying navigation");
                // Retry after a short delay to let user data load
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (entrantUser != null && entrantUser.getWantsNotifications()) {
                        navigateToNotifications();
                    }
                }, 500);
                return;
            }

            if (!entrantUser.getWantsNotifications()) {
                Log.d("EntrantActivity", "User has notifications disabled");
                return;
            }

            navigateToNotifications();

            intent.removeExtra("navigate_to");
            setIntent(intent);
        }
    }

    private void navigateToNotifications() {
        if (navController == null) {
            Log.e("EntrantActivity", "NavController is null!");
            return;
        }

        // Use Handler to ensure navigation happens on the main thread
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            try {
                Log.d("EntrantActivity", "Attempting navigation to notifications");
                NavOptions navOptions = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .setPopUpTo(navController.getGraph().getStartDestinationId(), true) // reset stack to root
                        .setRestoreState(true)
                        .build();

                navController.navigate(R.id.nav_notifications, null, navOptions);

                // Close the drawer after navigating
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
                Log.d("EntrantActivity", "Navigation successful");
            } catch (IllegalArgumentException e) {
                Log.e("EntrantActivity", "Navigation destination not found", e);
            } catch (IllegalStateException e) {
                Log.e("EntrantActivity", "NavController in invalid state", e);
            } catch (Exception e) {
                Log.e("EntrantActivity", "Unexpected navigation error", e);
            }
        });
    }

    /**
     * Requests notification permission on Android 13+
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
    }

    /**
     * Handles permission request results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "You'll receive event notifications", Toast.LENGTH_SHORT).show();
                Log.d("EntrantActivity", "Notification permission granted");
            } else {
                stopNotificationService();
                Toast.makeText(this,
                        "Notification permission is required for real-time updates",
                        Toast.LENGTH_LONG).show();
                Log.d("EntrantActivity", "Notification permission denied");
            }
        }
    }

    /**
     * Called when the activity is being destroyed.
     * Unbinds from the notification service to prevent memory leaks.
     * The service may continue running in the background if it's still needed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }
}