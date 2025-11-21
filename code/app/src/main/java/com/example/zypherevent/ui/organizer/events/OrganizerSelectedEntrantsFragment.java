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
 * Fragment to display lists of entrants by status (Accepted, Invited, Declined).
 * Implements US 02.06.01, US 02.06.02, and US 02.06.03.
 * Uses popup_organizer_selected_entrants.xml as its layout.
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

    // Defines the groups for the spinner and their corresponding list names
    private final List<String> groupNames = Arrays.asList(
            "Accepted Entrants (Final List)",
            "Invited Entrants",
            "Declined Entrants (Cancelled)"
    );

    public OrganizerSelectedEntrantsFragment() {
        // Required empty public constructor
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new Database();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Uses the layout defined in popup_organizer_selected_entrants.xml
        return inflater.inflate(R.layout.popup_organizer_selected_entrants, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (eventId == null) {
            Toast.makeText(getContext(), "Error: Event ID missing.", Toast.LENGTH_SHORT).show();
            if (getActivity() != null) getActivity().getSupportFragmentManager().popBackStack();
            return;
        }

        groupSpinner = view.findViewById(R.id.dropdown);
        entrantRecyclerView = view.findViewById(R.id.entrant_info);
        groupLabel = view.findViewById(R.id.label1);

        // Setup RecyclerView
        adapter = new EntrantInfoAdapter(new ArrayList<>());
        entrantRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        entrantRecyclerView.setAdapter(adapter);

        // Load event data and setup spinner
        loadEventData();
    }

    /**
     * Loads the event data from the database and initializes the spinner logic.
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
                    // Load the first group (Accepted Entrants) by default
                    loadEntrantsForSelectedGroup(groupNames.get(0));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load event data", e);
                    Toast.makeText(getContext(), "Error loading event.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Initializes the spinner with the list of groups (Accepted, Invited, Declined).
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
                groupLabel.setText("Group: " + selectedGroup.split(" ")[0]); // Display just the main status word
                loadEntrantsForSelectedGroup(selectedGroup);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    /**
     * Retrieves the list of entrant hardware IDs for the selected group from the current Event object.
     *
     * @param selectedGroup The name of the group selected from the spinner.
     * @return The list of hardware IDs for that group.
     */
    private List<String> getHardwareIdsForGroup(String selectedGroup) {
        if (currentEvent == null) return new ArrayList<>();

        switch (selectedGroup) {
            case "Accepted Entrants (Final List)":
                // US 02.06.03: Final list of enrolled entrants
                return currentEvent.getAcceptedEntrants();
            case "Invited Entrants":
                // US 02.06.01 (implied: view all chosen entrants)
                return currentEvent.getInvitedEntrants();
            case "Declined Entrants (Cancelled)":
                // US 02.06.02: List of all cancelled entrants
                return currentEvent.getDeclinedEntrants();
            default:
                return new ArrayList<>();
        }
    }

    /**
     * Fetches Entrant objects for the selected group and updates the RecyclerView.
     *
     * @param selectedGroup The name of the group selected from the spinner.
     */
    private void loadEntrantsForSelectedGroup(String selectedGroup) {
        List<String> hardwareIds = getHardwareIdsForGroup(selectedGroup);

        if (hardwareIds.isEmpty()) {
            adapter.updateData(new ArrayList<>());
            return;
        }

        // Create tasks to fetch each Entrant object asynchronously
        List<Task<User>> lookupTasks = new ArrayList<>();
        for (String id : hardwareIds) {
            if (id != null && !id.isEmpty()) {
                lookupTasks.add(db.getUser(id));
            }
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
                    Log.d(TAG, "Loaded " + resultEntrants.size() + " entrants for group: " + selectedGroup);
                    adapter.updateData(resultEntrants);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load entrants for group: " + selectedGroup, e);
                    adapter.updateData(new ArrayList<>());
                    Toast.makeText(getContext(), "Failed to load entrant details.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Adapter used to display Entrant information in the RecyclerView.
     * Note: This is a simplified adapter structure for a demonstration.
     * You would typically use a separate file for the Adapter and ViewHolder.
     */
    private static class EntrantInfoAdapter extends RecyclerView.Adapter<EntrantInfoAdapter.EntrantInfoViewHolder> {
        private List<Entrant> entrants;

        public EntrantInfoAdapter(List<Entrant> entrants) {
            this.entrants = entrants;
        }

        public void updateData(List<Entrant> newEntrants) {
            this.entrants.clear();
            this.entrants.addAll(newEntrants);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public EntrantInfoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Using a simple text item layout, ideally you'd use a custom layout
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new EntrantInfoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull EntrantInfoViewHolder holder, int position) {
            Entrant entrant = entrants.get(position);
            holder.text1.setText(String.format("Name: %s %s", entrant.getFirstName(), entrant.getLastName()));
            holder.text2.setText(String.format("Email: %s | Phone: %s", entrant.getEmail(), entrant.getPhoneNumber()));
        }

        @Override
        public int getItemCount() {
            return entrants.size();
        }

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