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
import com.example.zypherevent.EntrantActivity;
import com.example.zypherevent.OrganizerActivity;
import com.example.zypherevent.R;
import com.example.zypherevent.WaitlistEntry;
import com.example.zypherevent.model.LabelInfoWindow;
import com.example.zypherevent.model.MapPoint;
import com.example.zypherevent.Event;
import com.example.zypherevent.userTypes.Entrant;
import com.example.zypherevent.userTypes.Organizer;
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
 * @author Elliot Chrystal
 *
 * @version 1.0
 */
public class OrganizerEventsMapFragment extends Fragment {

    private MapView mapView;

    private Organizer currentOrganizer;

    private List<Event> organizerEvents;

    public OrganizerEventsMapFragment() {
        // public no-arg constructor required
    }

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

    public Task<List<Event>> getEvents() {
        Database db = new Database();
        return db.getAllEventsList();
    }

    public Task<List<MapPoint>> getMapPoints(Event event) {
        if (event == null) {
            return Tasks.forResult(new ArrayList<>());
        }

        // update the entrants' info in the event (location data may have been updated!)
        return event.updateEntrantInformationInLists()
                .continueWith(task -> {
                    List<MapPoint> mapPoints = new ArrayList<>();

                    if (!task.isSuccessful()) {
                        Log.w("OrganizerEventsMapFrag",
                                "Failed to update entrants", task.getException());
                        return mapPoints; // empty list on failure
                    }

                    // --- WAITLISTED ---
                    ArrayList<WaitlistEntry> waitlistEntrants = event.getWaitListEntrants();
                    if (waitlistEntrants != null) {
                        for (WaitlistEntry entry : waitlistEntrants) {
                            Entrant entrant = entry.getEntrant();
                            if (entrant == null) continue;

                            com.google.firebase.firestore.GeoPoint location = entrant.getLocation();
                            if (location == null) {
                                Log.w("OrganizerEventsMapFrag",
                                        "Waitlisted entrant has null location: "
                                                + entrant.getFirstName() + " " + entrant.getLastName());
                                continue;
                            }

                            String label = entrant.getFirstName() + " " + entrant.getLastName();
                            mapPoints.add(new MapPoint(location, label, MapPoint.Status.WAITLISTED));
                        }
                    }

                    // --- ACCEPTED ---
                    ArrayList<Entrant> acceptedEntrants = event.getAcceptedEntrants();
                    if (acceptedEntrants != null) {
                        for (Entrant entrant : acceptedEntrants) {
                            if (entrant == null) continue;

                            com.google.firebase.firestore.GeoPoint location = entrant.getLocation();
                            if (location == null) {
                                Log.w("OrganizerEventsMapFrag",
                                        "Accepted entrant has null location: "
                                                + entrant.getFirstName() + " " + entrant.getLastName());
                                continue;
                            }

                            String label = entrant.getFirstName() + " " + entrant.getLastName();
                            mapPoints.add(new MapPoint(location, label, MapPoint.Status.ACCEPTED));
                        }
                    }

                    // --- DENIED ---
                    ArrayList<Entrant> declinedEntrants = event.getDeclinedEntrants();
                    if (declinedEntrants != null) {
                        for (Entrant entrant : declinedEntrants) {
                            if (entrant == null) continue;

                            com.google.firebase.firestore.GeoPoint location = entrant.getLocation();
                            if (location == null) {
                                Log.w("OrganizerEventsMapFrag",
                                        "Declined entrant has null location: "
                                                + entrant.getFirstName() + " " + entrant.getLastName());
                                continue;
                            }

                            String label = entrant.getFirstName() + " " + entrant.getLastName();
                            mapPoints.add(new MapPoint(location, label, MapPoint.Status.DENIED));
                        }
                    }

                    return mapPoints;
                });
    }

    /**
     * Adds all provided map points to the map, centers the view on them,
     * and adjusts the zoom level so that all points fit within the viewport.
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
     * Adds a marker to the map for the given MapPoint, using a status-specific icon
     * and a label displayed in the marker info window.
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
                    iconResId = R.drawable.marker_dot_accepted;
                    break;
                case DENIED:
                    iconResId = R.drawable.marker_dot_denied;
                    break;
                case WAITLISTED:
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

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume(); // needed for osmdroid
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause(); // needed for osmdroid
    }
}