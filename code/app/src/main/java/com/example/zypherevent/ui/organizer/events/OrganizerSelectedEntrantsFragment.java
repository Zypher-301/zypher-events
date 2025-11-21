package com.example.zypherevent.ui.organizer.events;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.zypherevent.Database;
import com.example.zypherevent.Event;
import com.example.zypherevent.R;
import com.example.zypherevent.userTypes.Entrant;
import com.example.zypherevent.userTypes.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A fragment that displays lists of entrants for a specific event, categorized into groups.
 * Organizers can use a dropdown spinner to switch between viewing "Accepted", "Invited", and
 * "Declined" entrants. For each selected group, it fetches the corresponding entrant details
 * from the database and displays them in a RecyclerView.
 *
 * <p>This fragment requires an {@code eventId} to be passed in its arguments bundle to identify
 * which event's entrants to display. It handles fetching event data, populating the spinner,
 * and dynamically loading entrant information based on the organizer's selection.
 * </p>
 *
 * @author Arunavo Dutta
 * @see Event
 * @see Entrant
 * @see Database
 */
public class OrganizerSelectedEntrantsFragment extends Fragment {

    private static final String TAG = "SelectedEntrantsFrag";
    private Database db;
    private Long eventId;
    private Event currentEvent;
    private Spinner groupSpinner;
    private RecyclerView entrantRecyclerView;
    private EntrantInfoAdapter adapter;
    private TextView groupLabel;

    /**
     * A list of strings representing the different categories of entrants for an event.
     * These strings are used to populate the spinner (dropdown menu) that allows the organizer
     * to filter and view entrants based on their status: accepted, invited, or declined.
     */
    private final List<String> groupNames = Arrays.asList(
            "Accepted Entrants",
            "Invited Entrants",
            "Declined Entrants (Cancelled)"
    );

    public OrganizerSelectedEntrantsFragment() {
        // Required empty public constructor
    }

    // A setter is useful for testing, but the App uses getArguments() below
    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    /**
     * Called when the fragment is first created. This is where you should do all of your normal
     * static set up: create views, bind data to lists, etc. This method also runs when the system
     * re-creates the fragment for configuration changes.
     * It initializes the {@link Database} instance.
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new Database();
    }

    /**
     * Called to have the fragment instantiate its user interface view. This method inflates the layout
     * for this fragment's view from the XML resource file.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     *                  The fragment should not add the view itself, but this can be used to generate
     *                  the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous
     *                           saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.popup_organizer_selected_entrants, container, false);
    }

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * has returned, but before any saved state has been restored in to the view.
     * This gives subclasses a chance to initialize themselves once
     * they know their view hierarchy has been completely created.
     *
     * <p>This method initializes the UI components of the fragment, retrieves the event ID
     * from the fragment's arguments, and triggers the loading of event data. It sets up
     * the RecyclerView for displaying entrants and handles cases where the event ID is missing.</p>
     *
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            eventId = getArguments().getLong("eventId");
        }

        if (eventId == null || eventId == 0) {
            Toast.makeText(getContext(), "Error: Event ID missing.", Toast.LENGTH_SHORT).show();
            if (getActivity() != null) getActivity().getSupportFragmentManager().popBackStack();
            return;
        }

        groupSpinner = view.findViewById(R.id.dropdown);
        entrantRecyclerView = view.findViewById(R.id.entrant_info);
        groupLabel = view.findViewById(R.id.label1);

        adapter = new EntrantInfoAdapter(new ArrayList<>());
        entrantRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        entrantRecyclerView.setAdapter(adapter);

        loadEventData();
    }

    /**
     * Fetches the event data from the database using the event ID.
     * On success, it stores the event details, sets up the group selection spinner,
     * and loads the entrant data for the default group ("Accepted Entrants").
     * On failure, it logs an error and displays a toast message to the user.
     */
    private void loadEventData() {
        db.getEvent(eventId)
                .addOnSuccessListener(event -> {
                    currentEvent = event;
                    if (currentEvent == null) {
                        Toast.makeText(getContext(), "Event not found.", Toast.LENGTH_SHORT).show();
                        if (getActivity() != null) getActivity().getSupportFragmentManager().popBackStack();
                        return;
                    }
                    setupGroupSpinner();
                    loadEntrantsForSelectedGroup(groupNames.get(0));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load event data", e);
                    Toast.makeText(getContext(), "Error loading event.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Initializes and configures the spinner (dropdown menu) for selecting entrant groups.
     * This method creates an {@link ArrayAdapter} using the predefined {@code groupNames} list
     * and sets it to the {@code groupSpinner}. It also sets an
     * {@link AdapterView.OnItemSelectedListener} to handle user selections.
     * When a user selects a group from the spinner, the listener updates a label to show the
     * selected category (e.g., "Accepted") and calls {@link #loadEntrantsForSelectedGroup(String)}
     * to fetch and display the corresponding list of entrants.
     */
    private void setupGroupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                groupNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        groupSpinner.setAdapter(adapter);

        groupSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedGroup = groupNames.get(position);
                // Just show the first word (Accepted, Invited, Declined) in the big label
                groupLabel.setText(selectedGroup.split(" ")[0]);
                loadEntrantsForSelectedGroup(selectedGroup);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /**
     * Retrieves the appropriate list of hardware IDs based on the selected entrant group.
     * This method acts as a helper to select the correct list of IDs (accepted, invited,
     * or declined) from the {@code currentEvent} object based on the string provided
     * from the group spinner.
     *
     * @param selectedGroup A string representing the chosen group, e.g., "Accepted Entrants (Final List)".
     * @return A {@link List} of strings containing the hardware IDs for the specified group.
     *         Returns an empty list if the {@code currentEvent} is null or if the group name is not recognized.
     */
    private List<String> getHardwareIdsForGroup(String selectedGroup) {
        if (currentEvent == null) return new ArrayList<>();

        switch (selectedGroup) {
            case "Accepted Entrants":
                return currentEvent.getAcceptedEntrants();
            case "Invited Entrants":
                return currentEvent.getInvitedEntrants();
            case "Declined Entrants (Cancelled)":
                return currentEvent.getDeclinedEntrants();
            default:
                return new ArrayList<>();
        }
    }

    /**
     * Asynchronously loads the details of entrants for a given group and updates the RecyclerView.
     * <p>
     * This method first retrieves the list of hardware IDs corresponding to the selected group
     * (e.g., "Accepted Entrants"). If the list is empty, it clears the adapter. Otherwise,
     * it creates a list of asynchronous tasks to fetch {@link User} data for each hardware ID.
     * <p>
     * Using {@link Tasks#whenAllComplete(java.util.Collection)}, it waits for all database lookups
     * to finish. It then processes the results, filters for successful lookups that return an
     * {@link Entrant}, and compiles a list of these entrants. Finally, it updates the
     * {@link EntrantInfoAdapter} with the new list of entrants on the main thread, or displays
     * an error message if any part of the process fails.
     *
     * @param selectedGroup The name of the entrant group to load, corresponding to one of the
     *                      values in {@code groupNames} (e.g., "Accepted Entrants (Final List)").
     */
    private void loadEntrantsForSelectedGroup(String selectedGroup) {
        List<String> hardwareIds = getHardwareIdsForGroup(selectedGroup);

        if (hardwareIds == null || hardwareIds.isEmpty()) {
            adapter.updateData(new ArrayList<>());
            return;
        }

        List<Task<User>> lookupTasks = new ArrayList<>();
        for (String id : hardwareIds) {
            if (id != null && !id.isEmpty()) {
                lookupTasks.add(db.getUser(id));
            }
        }

        if (lookupTasks.isEmpty()) {
            adapter.updateData(new ArrayList<>());
            return;
        }

        Tasks.whenAllComplete(lookupTasks)
                .continueWith(task -> {
                    List<Entrant> resultEntrants = new ArrayList<>();
                    for (Task<User> userTask : lookupTasks) {
                        if (userTask.isSuccessful()) {
                            User user = userTask.getResult();
                            if (user instanceof Entrant) {
                                resultEntrants.add((Entrant) user);
                            }
                        }
                    }
                    return resultEntrants;
                })
                .addOnSuccessListener(resultEntrants -> {
                    adapter.updateData(resultEntrants);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load entrants", e);
                    Toast.makeText(getContext(), "Failed to load details.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * A RecyclerView adapter for displaying a list of {@link Entrant} objects.
     * This adapter is used to populate a RecyclerView with the details of entrants,
     * such as their name, email, and phone number. It provides a method to update
     * the list of entrants dynamically and refresh the view.
     */
    private static class EntrantInfoAdapter extends RecyclerView.Adapter<EntrantInfoAdapter.EntrantInfoViewHolder> {
        private List<Entrant> entrants;

        /**
         * Constructs an adapter with an initial list of entrants.
         *
         * @param entrants The list of {@link Entrant} objects to be displayed.
         *                 This list can be empty if no data is available initially.
         */
        public EntrantInfoAdapter(List<Entrant> entrants) {
            this.entrants = entrants;
        }

        /**
         * Updates the list of entrants displayed in the RecyclerView.
         * This method clears the current list of entrants, adds all entrants from the
         * new list, and then notifies the adapter that the data set has changed,
         * which triggers a refresh of the RecyclerView.
         *
         * @param newEntrants The new list of {@link Entrant} objects to display.
         */
        public void updateData(List<Entrant> newEntrants) {
            this.entrants.clear();
            this.entrants.addAll(newEntrants);
            notifyDataSetChanged();
        }

        /**
         * Called when RecyclerView needs a new {@link EntrantInfoViewHolder} of the given type to represent an item.
         * <p>
         * This new ViewHolder should be constructed with a new View that can represent the items
         * of the given type. You can either create a new View manually or inflate it from an XML
         * layout file.
         * <p>
         * The new ViewHolder will be used to display items of the adapter using
         * {@link #onBindViewHolder(EntrantInfoViewHolder, int)}. Since it will be re-used to display
         * different items in the data set, it is a good idea to cache references to sub-views of
         * the View to avoid unnecessary {@link View#findViewById(int)} calls.
         *
         * @param parent The ViewGroup into which the new View will be added after it is bound to
         *               an adapter position.
         * @param viewType The view type of the new View.
         * @return A new EntrantInfoViewHolder that holds a View of the given view type.
         */
        @NonNull
        @Override
        public EntrantInfoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new EntrantInfoViewHolder(view);
        }

        /**
         * Called by RecyclerView to display the data at the specified position. This method
         * updates the contents of the {@link EntrantInfoViewHolder#itemView} to reflect the
         * entrant at the given position in the data set.
         *
         * <p>It retrieves the {@link Entrant} object from the list, formats a string with the
         * entrant's full name, and another string with their email and phone number (if available).
         * These strings are then set as the text for the two {@link TextView}s in the holder's view.</p>
         *
         * @param holder   The ViewHolder which should be updated to represent the contents of the
         *                 item at the given position in the data set.
         * @param position The position of the item within the adapter's data set.
         */
        @Override
        public void onBindViewHolder(@NonNull EntrantInfoViewHolder holder, int position) {
            Entrant entrant = entrants.get(position);
            String name = entrant.getFirstName() + " " + entrant.getLastName();
            String details = "Email: " + entrant.getEmail();

            if (entrant.getPhoneNumber() != null && !entrant.getPhoneNumber().isEmpty()) {
                details += "\nPhone: " + entrant.getPhoneNumber();
            }

            holder.text1.setText(name);
            holder.text2.setText(details);
        }

        /**
         * Returns the total number of items in the data set held by the adapter.
         *
         * @return The total number of entrants in the list.
         */
        @Override
        public int getItemCount() {
            return entrants.size();
        }

        /**
         * A {@link RecyclerView.ViewHolder} that holds the view for a single entrant item in the list.
         * It displays the entrant's full name and their contact details (email and phone number).
         * The layout is based on {@code android.R.layout.simple_list_item_2}, which provides
         * two {@link TextView} elements ({@code text1} and {@code text2}).
         */
        public static class EntrantInfoViewHolder extends RecyclerView.ViewHolder {
            TextView text1;
            TextView text2;

            public EntrantInfoViewHolder(@NonNull View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}