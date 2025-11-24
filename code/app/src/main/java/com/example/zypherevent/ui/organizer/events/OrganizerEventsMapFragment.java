package com.example.zypherevent.ui.organizer.events;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.zypherevent.Database;
import com.example.zypherevent.OrganizerActivity;
import com.example.zypherevent.R;
import com.example.zypherevent.WaitlistEntry;
import com.example.zypherevent.model.LabelInfoWindow;
import com.example.zypherevent.model.MapPoint;
import com.example.zypherevent.Event;
import com.example.zypherevent.userTypes.Entrant;
import com.example.zypherevent.userTypes.Organizer;
import com.example.zypherevent.userTypes.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;


import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays an interactive map view of all entrant locations for events created by an organizer
 * This fragment provides organizers with a map of participation in their events.
 * Entrants who have geolocation enabled and are associated with a selected event are shown as
 * a marker (waitlisted, invited, accepted, or denied). Each marker reflects the
 * entrant’s current relationship to the event, and clicking a marker displays a label with
 * the entrant’s name.
 *
 * @author Elliot Chrystal
 * @version 2.0
 */
public class OrganizerEventsMapFragment extends Fragment {

    /**
     * The map view used to display entrant locations for organizer events.
     */
    private MapView mapView;

    /**
     * The currently logged-in organizer viewing their events.
     */
    private Organizer currentOrganizer;

    /**
     * The list of events created by the current organizer, used to populate the spinner.
     */
    private List<Event> organizerEvents;

    /**
     * Public no-argument constructor required for fragment instantiation.
     */
    public OrganizerEventsMapFragment() {
        // public no-arg constructor required
    }

    /**
     * Inflates the organizer events map layout and configures the map and event filter spinner.
     * This method initializes the osmdroid MapView, loads the current organizer
     * from the hosting OrganizerActivity, fetches all events, filters them to those
     * owned by the organizer, and populates the spinner with event names. When an event is
     * selected, the map is updated with mappoints representing entrants for that event.
     *
     * @param inflater           the layout inflater used to inflate the fragment view
     * @param container          the parent view that the fragment's UI should be attached to, or null
     * @param savedInstanceState the previously saved state, or Null if none
     * @return the root view for this fragment's UI
     */
    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate layout
        View view = inflater.inflate(R.layout.fragment_organizer_events_map, container, false);

        // Get the Organizer user from the activity
        OrganizerActivity activity = (OrganizerActivity) requireActivity();
        currentOrganizer = activity.getOrganizerUser();

        // Load osmdroid configuration for the map
        Configuration.getInstance().load(
                getContext().getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getContext().getApplicationContext())
        );

        // Initialize the MapView
        mapView = view.findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);  // set tile source to OSM tiles
        mapView.setMultiTouchControls(true);              // allow pinch to zoom

        // Set initial zoom + position
        mapView.getController().setZoom(5.0);

        // close the labels for the points when the map is tapped
        mapView.setOnTouchListener((v, event) -> {
            InfoWindow.closeAllInfoWindowsOn(mapView);
            return false; // let map still pan/zoom
        });

        // Initialize the spinner for event specific map points
        Spinner eventsFilterSpinner = view.findViewById(R.id.eventsFilterSpinner);

        // get the events from the Organizer and display them in the spinner
        getEvents().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<Event> fetchedEvents = task.getResult();
                List<Event> myEvents = new ArrayList<>();

                for (Event event : fetchedEvents) {
                    if (event.getEventOrganizerHardwareID().equals(currentOrganizer.getHardwareID())) {
                        myEvents.add(event);
                    }
                }

                // Convert to list of event names for the spinner
                List<String> eventNames = new ArrayList<>();

                // Placeholder entry for "no event selected" state
                eventNames.add("Select an event");

                for (Event e : myEvents) {
                    eventNames.add(e.getEventName());
                }

                // Create adapter for spinner
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        eventNames
                );

                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                // attach adapter to spinner
                eventsFilterSpinner.setAdapter(adapter);

                // Save the events to use them later
                this.organizerEvents = myEvents;

            } else {
                Exception e = task.getException();
                Log.d("OrganizerEventMapFrag", "Error getting events:" + e.getMessage());
            }
        });

        // Handle spinner selection
        eventsFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // position 0 = "Select an event", clear map and do nothing
                if (position == 0) {
                    if (mapView != null) {
                        InfoWindow.closeAllInfoWindowsOn(mapView);
                        mapView.getOverlays().clear();
                        mapView.invalidate();
                    }
                    return;
                }

                String selectedName = parent.getItemAtPosition(position).toString();

                Event selectedEvent = null;
                if (organizerEvents != null) {
                    for (Event e : organizerEvents) {
                        if (e.getEventName().equals(selectedName)) {
                            selectedEvent = e;
                            break;
                        }
                    }
                }

                if (selectedEvent != null) {
                    // use the Task-based getMapPoints
                    getMapPoints(selectedEvent).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            List<MapPoint> pointsForThisEvent = task.getResult();
                            showMapPoints(pointsForThisEvent);
                        } else {
                            Log.w("OrganizerEventsMapFrag",
                                    "Failed to get MapPoints for event", task.getException());
                            // clear map on error:
                            InfoWindow.closeAllInfoWindowsOn(mapView);
                            mapView.getOverlays().clear();
                            mapView.invalidate();
                        }
                    });
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        return view;
    }

    /**
     * Retrieves all events from the database.
     * This method delegates to {@link Database#getAllEventsList()} and returns the
     * resulting task. The full event list is later filtered to those owned by the
     * current organizer before being used to populate the spinner.
     *
     * @return a task that resolves to a list of all Event objects
     */
    public Task<List<Event>> getEvents() {
        Database db = new Database();
        return db.getAllEventsList();
    }

    /**
     * Builds a list of mapPoints instances representing participants for the given event.
     * This method inspects the event's waitlisted, invited, accepted, and declined entrants,
     * looks up each corresponding User from the database, and converts those users
     * who are Entrants with geolocation enabled into MapPoints. Each map point
     * is tagged with a Status reflecting the entrant's status for the event.
     * If the event is null or no entrants are found, an empty list is returned.
     *
     * @param event the event whose participant locations should be resolved
     * @return a task that resolves to a list of MapPoints for the event
     */
    public Task<List<MapPoint>> getMapPoints(Event event) {
        if (event == null) {
            return Tasks.forResult(new ArrayList<>());
        }

        Database db = new Database();
        List<Task<User>> lookupTasks = new ArrayList<>();
        List<MapPoint.Status> statuses = new ArrayList<>();

        // --- WAITLISTED (WaitlistEntry -> hardware ID) ---
        ArrayList<WaitlistEntry> waitlistEntrants = event.getWaitListEntrants();
        Log.d("OrganizerEventsMapFrag", "Waitlist Entrys: " + waitlistEntrants);
        if (waitlistEntrants != null) {
            for (WaitlistEntry entry : waitlistEntrants) {
                if (entry == null) continue;
                String hardwareID = entry.getEntrantHardwareID();
                if (hardwareID == null || hardwareID.isEmpty()) continue;

                lookupTasks.add(db.getUser(hardwareID));
                statuses.add(MapPoint.Status.WAITLISTED);
            }
        }

        // --- INVITED (list of hardware IDs) ---
        ArrayList<String> invitedIds = event.getInvitedEntrants();
        Log.d("OrganizerEventsMapFrag", "Invited IDs: " + invitedIds);
        if (invitedIds != null) {
            for (String hardwareID : invitedIds) {
                if (hardwareID == null || hardwareID.isEmpty()) continue;

                lookupTasks.add(db.getUser(hardwareID));
                statuses.add(MapPoint.Status.INVITED);
            }
        }

        // --- ACCEPTED (list of hardware IDs) ---
        ArrayList<String> acceptedIds = event.getAcceptedEntrants();
        Log.d("OrganizerEventsMapFrag", "Accepted IDs: " + acceptedIds);
        if (acceptedIds != null) {
            for (String hardwareID : acceptedIds) {
                if (hardwareID == null || hardwareID.isEmpty()) continue;

                lookupTasks.add(db.getUser(hardwareID));
                statuses.add(MapPoint.Status.ACCEPTED);
            }
        }

        // --- DENIED (list of hardware IDs) ---
        ArrayList<String> declinedIds = event.getDeclinedEntrants();
        Log.d("OrganizerEventsMapFrag", "Declined IDs: " + declinedIds);
        if (declinedIds != null) {
            for (String hardwareID : declinedIds) {
                if (hardwareID == null || hardwareID.isEmpty()) continue;

                lookupTasks.add(db.getUser(hardwareID));
                statuses.add(MapPoint.Status.DENIED);
            }
        }

        // No entrants at all
        if (lookupTasks.isEmpty()) {
            return Tasks.forResult(new ArrayList<>());
        }

        // Wait until all lookups complete, then walk lookupTasks in order
        return Tasks.whenAllComplete(lookupTasks)
                .continueWith(task -> {
                    List<MapPoint> mapPoints = new ArrayList<>();

                    for (int i = 0; i < lookupTasks.size(); i++) {
                        Task<User> userTask = lookupTasks.get(i);

                        if (!userTask.isSuccessful()) {
                            Log.w("OrganizerEventsMapFrag",
                                    "Failed to fetch user for map point", userTask.getException());
                            continue;
                        }

                        User user = userTask.getResult();
                        if (!(user instanceof Entrant)) {
                            // This shouldn't happen for event participants, but be safe
                            Log.w("OrganizerEventsMapFrag",
                                    "User is not an Entrant, skipping: " + (user != null ? user.getHardwareID() : "null"));
                            continue;
                        }

                        Entrant entrant = (Entrant) user;
                        if (entrant == null) continue;

                        // Only use geo-location for those who have it enabled
                        if (entrant.getUseGeolocation() == true) {

                            com.google.firebase.firestore.GeoPoint location = entrant.getLocation();
                            if (location == null) {
                                Log.w("OrganizerEventsMapFrag",
                                        "Entrant has null location: "
                                                + entrant.getFirstName() + " " + entrant.getLastName());
                                continue;
                            }

                            String label = entrant.getFirstName() + " " + entrant.getLastName();
                            MapPoint.Status status = statuses.get(i); // same index as lookupTasks

                            mapPoints.add(new MapPoint(location, label, status));
                        }
                    }

                    return mapPoints;
                });
    }

    /**
     * Adds all provided map points to the map and adjusts the viewport.
     * Existing markers and info windows are cleared, new markers are added for each
     * mappoint, and the map is either centered on a single point or zoomed
     * to fit all points within a bounding box when multiple locations are present.
     *
     * @param mapPoints a list of MapPoint objects to display
     */
    public void showMapPoints(@NonNull List<MapPoint> mapPoints) {
        if (mapPoints == null || mapPoints.isEmpty() || mapView == null) {
            return;
        }

        // clear existing markers/info windows
        InfoWindow.closeAllInfoWindowsOn(mapView);
        mapView.getOverlays().clear();

        // add markers for each MapPoint
        List<GeoPoint> geoPoints = new ArrayList<>();
        for (MapPoint mapPoint : mapPoints) {
            if (mapPoint == null) {
                continue;
            }

            // Add marker for this MapPoint
            addMapPoint(mapPoint);

            // Track for bounding box
            geoPoints.add(new GeoPoint(mapPoint.getLatitude(), mapPoint.getLongitude()));
        }

        if (geoPoints.isEmpty()) {
            return;
        }

        if (geoPoints.size() == 1) {
            // Single point: just center and zoom in
            GeoPoint only = geoPoints.get(0);
            mapView.getController().setCenter(only);
            mapView.getController().setZoom(16.0); // tweak as you like
        } else {
            // Multiple points: compute bounding box and zoom to fit
            BoundingBox boundingBox = BoundingBox.fromGeoPoints(geoPoints);
            // Second param = animated, third = padding in pixels
            mapView.zoomToBoundingBox(boundingBox, true, 100);
        }

        mapView.invalidate();
    }

    /**
     * Adds a marker to the map for the given MapPoint.
     * The marker's icon is chosen based on the point's MapPoint.Status, and the
     * entrant label is shown in a custom LabelInfoWindow when the marker is tapped.
     * If the map view is not available, no marker is added.
     *
     * @param mapPoint the MapPoint to display on the map
     */
    public void addMapPoint(@NonNull MapPoint mapPoint) {
        if (mapView == null) {
            return;
        }

        // Convert Firebase GeoPoint to osmdroid GeoPoint
        GeoPoint point = new GeoPoint(mapPoint.getLatitude(), mapPoint.getLongitude());

        Marker marker = new Marker(mapView);
        marker.setPosition(point);

        // Choose icon based on status
        int iconResId;
        MapPoint.Status status = mapPoint.getStatus();

        if (status == null) {
            // Fallback if status is somehow null
            iconResId = R.drawable.marker_dot_waitlisted;
        } else {
            switch (status) {
                case ACCEPTED:
                    Log.d("OrganizerEventsMapFrag", "Accepted status added, " + mapPoint.getLabel());
                    iconResId = R.drawable.marker_dot_accepted;
                    break;
                case DENIED:
                    Log.d("OrganizerEventsMapFrag", "Denied status added, " + mapPoint.getLabel());
                    iconResId = R.drawable.marker_dot_denied;
                    break;
                case INVITED:
                    Log.d("OrganizerEventsMapFrag", "Invited status added, " + mapPoint.getLabel());
                    iconResId = R.drawable.marker_dot_invited;
                    break;
                case WAITLISTED:
                    Log.d("OrganizerEventsMapFrag", "Waitlisted status added, " + mapPoint.getLabel());
                default:
                    iconResId = R.drawable.marker_dot_waitlisted;
                    break;
            }
        }

        marker.setIcon(ContextCompat.getDrawable(requireContext(), iconResId));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);

        // Label for the popup (e.g., entrant name)
        marker.setTitle(mapPoint.getLabel());

        // Use your custom simple info window
        marker.setInfoWindow(new LabelInfoWindow(mapView));

        mapView.getOverlays().add(marker);
        mapView.invalidate();
    }

    /**
     * Resumes the map view when the fragment becomes active.
     * This forwards the lifecycle event to osmdroid's MapView so that it can
     * properly manage resources such as the compass, location overlays, and tile loading.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume(); // needed for osmdroid
    }

    /**
     * Pauses the map view when the fragment is no longer in the foreground.
     * This forwards the lifecycle event to osmdroid's MapView so that it can
     * pause ongoing work and release resources while the fragment is not visible.
     */
    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause(); // needed for osmdroid
    }
}