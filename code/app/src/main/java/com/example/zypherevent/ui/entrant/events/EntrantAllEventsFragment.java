package com.example.zypherevent.ui.entrant.events;


import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.zypherevent.Database;
import com.example.zypherevent.EntrantActivity;
import com.example.zypherevent.Event;
import com.example.zypherevent.R;
import com.example.zypherevent.userTypes.Entrant;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * BUG: DOUBLE CLICKING NEEDED FOR LEAVING THE WAITLIST
 * A fragment that displays a comprehensive list of all available events to an Entrant.
 * It provides the functionality for users to join or leave the waitlist for an event.
 * This class is responsible for fetching event data from the database, displaying it in a
 * RecyclerView, and handling user interactions such as joining, leaving, and refreshing the event list.
 * <p>
 * This class implements the following user stories:
 * <ul>
 * <li>US 01.01.01: As an Entrant, I want to join the waitlist for an event.</li>
 * <li>US 01.01.02: As an Entrant, I want to leave the waitlist for an event.</li>
 * </ul>
 *
 * @author Elliot Chrystal
 * @author Arunavo Dutta
 * @version 3.0
 */

public class EntrantAllEventsFragment extends Fragment implements EntrantEventAdapter.OnItemClickListener {

    private static final String TAG = "EntrantAllEvents";

    private Database db;
    private RecyclerView recyclerView;
    private EntrantEventAdapter adapter;

    /** what is shown on the all events list after filters applied */
    private List<Event> eventList = new ArrayList<>();

    /** all the events, unfiltered */
    private List<Event> allEvents = new ArrayList<>();   // full dataset
    private String filterQuery = "";
    private Date filterStartDate = null;
    private Date filterEndDate = null;
    private Button refreshButton;
    private Button filterButton;
    private Entrant currentUser;
    private Long highlightEventId = null;

    /**
     * A date formatter to present event start and registration times in a detailed,
     * human-readable format for the event details dialog.
     * Example format: "Wed, Mar 13, 2024 at 5:00 PM".
     */
    private final SimpleDateFormat detailDateFormatter =
            new SimpleDateFormat("EEE, MMM d, yyyy 'at' h:mm a", Locale.getDefault());


    public EntrantAllEventsFragment() { }

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is optional, and non-graphical fragments can return null. This will be called between
     * {@link #onCreate(Bundle)} and {@link #onViewCreated(View, Bundle)}.
     * <p>A default View can be returned by calling Fragment in your
     * constructor. If you return a View from here, you will later be called in
     * {@link #onDestroyView} when the view is being released.
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return Return the View for the fragment's UI, or null.
     */
    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entrant_fragment_list_page, container, false);
    }

    /**
     * Added by Arunavo Dutta
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * has returned, but before any saved state has been restored in to the view.
     * This gives subclasses a chance to initialize themselves once
     * they know their view hierarchy has been completely created.
     * <p>
     * This method initializes the RecyclerView, its adapter, and the refresh button.
     * It also retrieves the current Entrant user from the activity and sets up a listener
     * for the refresh button to reload the list of events. The initial load of events
     * is also triggered here.
     *
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = new Database();
        if (getActivity() instanceof EntrantActivity) {
            currentUser = ((EntrantActivity) getActivity()).getEntrantUser();
        }
        if (currentUser == null) {
            Log.e(TAG, "Current Entrant user is NULL.");
            return;
        }

        // Check if we need to highlight a specific event from QR scan
        if (getArguments() != null) {
            highlightEventId = getArguments().getLong("highlightEventId", -1L);
            if (highlightEventId == -1L) {
                highlightEventId = null;
            }
        }

        recyclerView = view.findViewById(R.id.recycler_view);
        refreshButton = view.findViewById(R.id.refresh_button);
        filterButton = view.findViewById(R.id.filter_button);

        adapter = new EntrantEventAdapter(eventList, currentUser, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // refresh button listener
        refreshButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Refreshing list...", Toast.LENGTH_SHORT).show();
            loadEvents();
        });

        // filter button listener
        filterButton.setOnClickListener(v -> showFilterDialog());

        loadEvents();
    }

    /**
     * Added by Arunavo Dutta
     * Asynchronously loads all events from the Firestore database.
     * <p>
     * This method fetches the complete list of available events from the database.
     * It first clears the local allEvents list, then populates it with the newly fetched data
     * with the applied filters. Finally, it notifies the {@link EntrantEventAdapter} that the data
     * set has changed, prompting the {@link RecyclerView} to refresh and display the updated list
     * of events. If the database query fails, an error is logged.
     */
    private void loadEvents() {
        Log.d(TAG, "Attempting to query 'events' collection...");
        db.getAllEventsList().addOnCompleteListener(task -> {
            if (!isAdded() || getContext() == null) {
                Log.w(TAG, "loadEvents callback received, but fragment is detached.");
                return;
            }
            if (task.isSuccessful()) {
                List<Event> fetchedEvents = task.getResult();
                if (fetchedEvents == null) { return; }
                Log.d(TAG, "Firebase query successful. Found " + fetchedEvents.size() + " events.");

                allEvents.clear();
                allEvents.addAll(fetchedEvents);

                applyFilters();

                if (highlightEventId != null) {
                    scrollToEvent(highlightEventId);
                    highlightEventId = null;
                }
            } else {
                Log.e(TAG, "Error running query: ", task.getException());
            }
        });
    }

    /**
     * Temporary holder for filter values while the filter dialog is open.
     * This class stores the in-progress query text and optional start and end dates so that the
     * user can adjust filters in the dialog without immediately applying them to the main list.
     * The values are copied back to the fragment's actual filter fields only when the user taps
     * the Apply button.
     */
    private static class TempFilterState {

        /** The search query entered in the filter dialog. */
        String query;

        /** The selected start date for filtering events, or null if none is set. */
        Date startDate;

        /** The selected end date for filtering events, or null if none is set. */
        Date endDate;
    }

    /**
     * Displays the filter dialog for refining the event list.
     * The dialog allows the user to adjust the search query and optional start and end dates used
     * to filter events. Changes are stored in a temporary state until the user taps Apply, at
     * which point the fragment's filter fields are updated and applyFilters() is called.
     * Tapping Clear resets the dialog fields to their default, and Cancel dismisses the dialog
     * without applying any changes.
     */
    private void showFilterDialog() {
        if (getContext() == null) return;

        // inflate the dialog box
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_event_filter, null);

        // grab the views
        EditText etSearch = dialogView.findViewById(R.id.et_search_events_dialog);
        Button btnFrom = dialogView.findViewById(R.id.btn_date_from_dialog);
        Button btnTo = dialogView.findViewById(R.id.btn_date_to_dialog);
        Button btnClear = dialogView.findViewById(R.id.btn_clear_dialog);
        Button btnApply = dialogView.findViewById(R.id.btn_apply_dialog);

        // Mutable holder for dialog-local state
        final TempFilterState temp = new TempFilterState();
        temp.query = filterQuery;
        temp.startDate = filterStartDate;
        temp.endDate = filterEndDate;

        // populate the dialog fields with the current filters
        etSearch.setText(temp.query);

        final SimpleDateFormat displayFormat =
                new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

        if (temp.startDate != null) {
            btnFrom.setText(displayFormat.format(temp.startDate));
        }
        if (temp.endDate != null) {
            btnTo.setText(displayFormat.format(temp.endDate));
        }

        // Date pickers
        btnFrom.setOnClickListener(v -> {
            showDatePicker(temp.startDate, date -> {
                temp.startDate = date;
                btnFrom.setText(displayFormat.format(date));
            });
        });

        btnTo.setOnClickListener(v -> {
            showDatePicker(temp.endDate, date -> {
                temp.endDate = date;
                btnTo.setText(displayFormat.format(date));
            });
        });

        // reset local filters and UI when clear button is pressed
        btnClear.setOnClickListener(v -> {
            temp.query = "";
            temp.startDate = null;
            temp.endDate = null;

            etSearch.setText("");
            btnFrom.setText("From date");
            btnTo.setText("To date");
        });

        // create the dialog
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle("Filter events")
                .setView(dialogView)
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .create();

        // copy temp into real filters and update list when apply is clicked
        btnApply.setOnClickListener(v -> {
            filterQuery = etSearch.getText().toString().trim();
            filterStartDate = temp.startDate;
            filterEndDate = temp.endDate;

            applyFilters();
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Updates the visual state of the filter button based on active filters.
     * When any filter is applied (query, start date, or end date), the button text and background
     * tint are adjusted to indicate that filters are active. If no filters are set, the button is
     * reset to its default label and tint.
     */
    private void updateFilterButtonState() {
        if (filterButton == null || getContext() == null) return;

        boolean hasFilters =
                (filterQuery != null && !filterQuery.isEmpty())
                        || filterStartDate != null
                        || filterEndDate != null;

        if (hasFilters) {
            filterButton.setText("Filter ✓");
            // change tint to indicate active filters
            filterButton.setBackgroundTintList(
                    ContextCompat.getColorStateList(requireContext(), android.R.color.holo_orange_light)
            );
        } else {
            filterButton.setText("Filter");

            // default AppCompat theme colorPrimary
            TypedValue tv = new TypedValue();
            requireContext().getTheme().resolveAttribute(
                    androidx.appcompat.R.attr.colorPrimary,
                    tv,
                    true
            );

            int defaultColor = tv.data;

            filterButton.setBackgroundTintList(ColorStateList.valueOf(defaultColor));
        }
    }

    /**
     * Callback interface for receiving a date selected from a date picker.
     * Implementations are notified when the user confirms a date. Allows the caller to update
     * local state or UI elements with the chosen value.
     */
    private interface DateSelectedCallback {

        /**
         * Called when the user selects a date.
         *
         * @param date the date chosen by the user
         */
        void onDateSelected(Date date);
    }

    /**
     * Shows a date picker dialog and returns the selected date via callback.
     * The dialog is optionally initialized with the provided initialDate. When the user
     * confirms a date, the result is normalized to midnight in the current timezone and passed to
     * the supplied callback. If the context is unavailable, the dialog is not shown.
     *
     * @param initialDate an optional initial date to pre-select in the picker, or null for today
     * @param callback    the callback to invoke when a date is selected
     */
    private void showDatePicker(@Nullable Date initialDate, @NonNull DateSelectedCallback callback) {
        if (getContext() == null) return;

        final java.util.Calendar cal = java.util.Calendar.getInstance();
        if (initialDate != null) {
            cal.setTime(initialDate);
        }

        int year = cal.get(java.util.Calendar.YEAR);
        int month = cal.get(java.util.Calendar.MONTH);
        int day = cal.get(java.util.Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(
                getContext(),
                (view, y, m, d) -> {
                    java.util.Calendar chosen = java.util.Calendar.getInstance();
                    chosen.set(java.util.Calendar.YEAR, y);
                    chosen.set(java.util.Calendar.MONTH, m);
                    chosen.set(java.util.Calendar.DAY_OF_MONTH, d);
                    chosen.set(java.util.Calendar.HOUR_OF_DAY, 0);
                    chosen.set(java.util.Calendar.MINUTE, 0);
                    chosen.set(java.util.Calendar.SECOND, 0);
                    chosen.set(java.util.Calendar.MILLISECOND, 0);

                    callback.onDateSelected(chosen.getTime());
                },
                year, month, day
        );

        dialog.show();
    }

    /**
     * Applies the current filter settings to the event list.
     * This method clears the displayed event list, re-evaluates all events against the active
     * query and date range using matchesQuery(Event, String) and matchesDateRange(Event, Date, Date)},
     * and repopulates the list with matching events. The adapter is then notified of the changes,
     * and the filter button state is updated to reflect whether any filters are active.
     */
    private void applyFilters() {
        eventList.clear();

        for (Event event : allEvents) {
            if (!matchesQuery(event, filterQuery)) continue;
            if (!matchesDateRange(event, filterStartDate, filterEndDate)) continue;
            eventList.add(event);
        }

        adapter.notifyDataSetChanged();

        // update the filter button
        updateFilterButtonState();
    }

    /**
     * Determines whether an event matches the given text query.
     * The query is compared against the event's name, description, and location using
     * case-insensitive substring matching. If the query is null or empty, the event is treated as
     * a match.
     *
     * @param event the event to evaluate against the query
     * @param query the search text entered by the user, or null if none
     * @return true if the event matches the query, false otherwise
     */
    private boolean matchesQuery(Event event, String query) {
        if (query == null || query.isEmpty()) {
            return true;
        }
        String lowerQuery = query.toLowerCase(Locale.getDefault());

        String name = event.getEventName() != null
                ? event.getEventName().toLowerCase(Locale.getDefault())
                : "";
        String desc = event.getEventDescription() != null
                ? event.getEventDescription().toLowerCase(Locale.getDefault())
                : "";
        String location = event.getLocation() != null
                ? event.getLocation().toLowerCase(Locale.getDefault())
                : "";

        return name.contains(lowerQuery) || desc.contains(lowerQuery) || location.contains(lowerQuery);
    }

    /**
     * Determines whether an event falls within the specified date range.
     * The event's start time is compared against optional start and end boundaries. If the event
     * date is null, it is treated as a match. If a start date is provided, events before
     * that date are excluded. If an end date is provided, events after that date are excluded.
     *
     * @param event the event whose date should be evaluated
     * @param start the inclusive start date of the filter range, or null if unbounded
     * @param end   the inclusive end date of the filter range, or null if unbounded
     * @return true if the event matches the date range, false otherwise
     */
    private boolean matchesDateRange(Event event, @Nullable Date start, @Nullable Date end) {
        // Choose which date you want to filter on; here I use event start time.
        Date eventDate = event.getStartTime();
        if (eventDate == null) {
            return true; // or false, depending on your preference
        }

        if (start != null && eventDate.before(start)) {
            return false;
        }
        if (end != null && eventDate.after(end)) {
            return false;
        }
        return true;
    }

    /**
     * Added by Arunoavo Dutta
     * Handles the click event for an item in the RecyclerView.
     * This is triggered when a user taps on an event card.
     * Currently, it displays a Toast message with the name of the clicked event.
     * <p>
     * This method has been expanded to navigate to a detailed view of the event.
     *
     * @param event The {@link Event} object corresponding to the clicked item.
     */
    /**
     * Handles the click event for an item in the RecyclerView by showing a details dialog.
     *
     * @param event The {@link Event} object corresponding to the clicked item.
     */
    @Override
    public void onItemClick(Event event) {
        // Check if context is available before creating a dialog
        if (getContext() == null) {
            Log.e(TAG, "onItemClick: Context is null, cannot show dialog.");
            return;
        }

        // Build the details string
        StringBuilder details = new StringBuilder();
        details.append("Description:\n").append(event.getEventDescription()).append("\n\n");
        details.append("Location: ").append(event.getLocation()).append("\n\n");
        details.append("Event Starts: ")
                .append(formatDate(event.getStartTime())).append("\n\n");
        details.append("Registration Opens: ")
                .append(formatDate(event.getRegistrationStartTime())).append("\n");
        details.append("Registration Closes: ")
                .append(formatDate(event.getRegistrationEndTime())).append("\n\n");
        details.append("Organizer: ").append(event.getEventOrganizerHardwareID());

        // Show the details in an AlertDialog
        new AlertDialog.Builder(getContext())
                .setTitle(event.getEventName()) // Set the event name as the title
                .setMessage(details.toString()) // Set the built string as the message
                .setPositiveButton("Close", (dialog, which) -> dialog.dismiss()) // Close button
                .show(); // Display the dialog
    }

    /**
     * Scrolls to and highlights a specific event in the list.
     * Called after scanning a QR code to show the user which event was scanned.
     * 
     * @param eventId The unique ID of the event to highlight
     */
    private void scrollToEvent(Long eventId) {
        if (eventId == null) {
            Toast.makeText(getContext(), "Invalid event ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // Find the event position
        int position = -1;
        for (int i = 0; i < eventList.size(); i++) {
            if (eventList.get(i).getUniqueEventID().equals(eventId)) {
                position = i;
                break;
            }
        }

        // Scroll to it or show not found message
        if (position != -1) {
            final int finalPosition = position;
            recyclerView.post(() -> {
                recyclerView.smoothScrollToPosition(finalPosition);
                Toast.makeText(getContext(),
                        "Found: " + eventList.get(finalPosition).getEventName(),
                        Toast.LENGTH_SHORT).show();
            });
        } else {
            Toast.makeText(getContext(), "Event not found in current list",
                    Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Added by Arunavo Dutta
     * Handles the click event for joining an event's waitlist.
     * <p>
     * This method orchestrates the process of an entrant joining an event. It performs a sequence
     * of asynchronous database operations:
     * <ol>
     * <li>Adds the current user to the specified event's waitlist in the database.</li>
     * <li>Upon success, creates a simplified "clean" version of the event object to avoid
     * nested data issues in Firestore when saving to the user's profile.</li>
     * <li>Adds this clean event to the user's local list of registered events.</li>
     * <li>Saves the updated user object (with the new event history) back to the database.</li>
     * <li>Finally, refreshes the event list to update the UI, typically changing the
     * "Join" button to a "Leave" button.</li>
     * </ol>
     * Error handling is implemented at each step to log failures and provide feedback to the user
     * via a {@link Toast}.
     *
     * @param event The {@link Event} object that the user has chosen to join.
     */
    @Override
    public void onJoinClick(Event event) {
        Log.d(TAG, "Joining waitlist for: " + event.getEventName());

        if (event.getRequiresGeolocation()) {
            if (!currentUser.getUseGeolocation()) {
                Log.d(TAG, "Geolocation is required for this event, but Entrant does not have it enabled.");
                Toast.makeText(getContext(), "Geolocation is required for this event. Please enable it in your settings.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        db.addEntrantToWaitlist(String.valueOf(event.getUniqueEventID()), currentUser)
                .addOnSuccessListener(aVoid -> {

                    Event eventForHistory = new Event(
                            event.getUniqueEventID(),
                            event.getEventName(),
                            event.getEventDescription(),
                            event.getStartTime(),
                            event.getLocation(),
                            event.getRegistrationStartTime(),
                            event.getRegistrationEndTime(),
                            event.getEventOrganizerHardwareID(),
                            event.getPosterURL(),
                            event.getRequiresGeolocation()
                    );

                    currentUser.addEventToRegisteredEventHistory(eventForHistory.getUniqueEventID());

                    db.setUserData(currentUser.getHardwareID(), currentUser)
                            .addOnSuccessListener(aVoid1 -> {
                                Log.d(TAG, "User profile updated with new event.");
                                Toast.makeText(getContext(), "Joined waitlist!", Toast.LENGTH_SHORT).show();
                                loadEvents(); // Refresh
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error saving user data after joining: ", e);
                                Toast.makeText(getContext(), "Error saving to profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error joining waitlist", e);
                    String message = e.getMessage();
                    if (message != null && !message.isEmpty()) {
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Failed to join waitlist", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Added by Arunavo Dutta
     * BUG: DOUBLE CLICKING NEEDED FOR LEAVING THE WAITLIST
     * Handles the "Leave Waitlist" button click for an event.
     * <p>
     * This method orchestrates the process for an entrant to leave the waitlist of a specific event.
     * It performs the following sequential, asynchronous operations:
     * <ol>
     * <li>Calls the database to remove the current user from the specified event's waitlist.</li>
     * <li>Upon successful removal from the event's waitlist, it removes the corresponding event from the user's
     * local list of registered events.</li>
     * <li>Saves the updated user object back to the database to persist the change in their event history.</li>
     * <li>Refreshes the list of all events to update the UI, which will now show the option to "Join" the waitlist again for that event.</li>
     * </ol>
     * Each step includes error handling. If a database operation fails, an error is logged, and a
     * {@link Toast} message is shown to the user to inform them of the failure. A success message is shown upon completion.
     *
     * @param event The {@link Event} object from which the user is leaving the waitlist.
     */
    // BUG: DOUBLE CLICKING NEEDED FOR LEAVING THE WAITLIST
    @Override
    public void onLeaveClick(Event event) {
        Log.d(TAG, "Leaving waitlist for: " + event.getEventName());

        currentUser.removeEventFromRegisteredEventHistory(event.getUniqueEventID());
        adapter.notifyDataSetChanged();

        db.removeEntrantFromWaitlist(String.valueOf(event.getUniqueEventID()), currentUser)
                .addOnSuccessListener(aVoid -> {

                    // use the ID directly
                    currentUser.removeEventFromRegisteredEventHistory(event.getUniqueEventID());

                    db.setUserData(currentUser.getHardwareID(), currentUser)
                            .addOnSuccessListener(aVoid1 -> {
                                Log.d(TAG, "User profile updated, event removed.");
                                Toast.makeText(getContext(), "Left waitlist.", Toast.LENGTH_SHORT).show();
                                loadEvents(); // same as before
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error saving user data after leaving: ", e);
                                Toast.makeText(getContext(), "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error leaving waitlist", e);
                    Toast.makeText(getContext(), "Error leaving waitlist: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Added by Arunavo Dutta
     * Helper method to safely format a Date object.
     * Returns "N/A" if the date is null.
     */
    private String formatDate(Date date) {
        if (date == null) {
            return "N/A";
        }
        try {
            return detailDateFormatter.format(date);
        } catch (Exception e) {
            Log.e(TAG, "Error formatting date", e);
            return "N/A";
        }
    }
}
