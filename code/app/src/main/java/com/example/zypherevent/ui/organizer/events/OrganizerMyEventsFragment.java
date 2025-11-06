package com.example.zypherevent.ui.organizer.events;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.zypherevent.Database;
import com.example.zypherevent.Event;
import com.example.zypherevent.OrganizerActivity;
import com.example.zypherevent.R;
import com.example.zypherevent.Utils;
import com.example.zypherevent.userTypes.Entrant;
import com.example.zypherevent.userTypes.Organizer;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OrganizerMyEventsFragment extends Fragment implements OrganizerEventsAdapter.OnItemClickListener {

    private static final String TAG = "OrganizerMyEvents";

    private Database db;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyOrganizer;
    private OrganizerEventsAdapter adapter;
    private List<Event> eventList = new ArrayList<>();
    private Organizer organizerUser;

    private Button fabCreateEvent;

    public OrganizerMyEventsFragment() {
        // public no-arg constructor required
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_organizer_my_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = new Database();

        if (getActivity() instanceof OrganizerActivity) {
            organizerUser = ((OrganizerActivity) getActivity()).getOrganizerUser();
        }

        if (organizerUser == null) {
            Log.e(TAG, "Current Organizer user is NULL.");
            Toast.makeText(getContext(), "Error: Organizer information not available", Toast.LENGTH_SHORT).show();
            return;
        }

        recyclerView = view.findViewById(R.id.rvOrganizerEvents);
        swipeRefreshLayout = view.findViewById(R.id.swipeOrganizer);
        emptyOrganizer = view.findViewById(R.id.emptyOrganizer);

        adapter = new OrganizerEventsAdapter(eventList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadEvents();
        });

        // Find the Button
        fabCreateEvent = view.findViewById(R.id.fabCreateEvent);
        fabCreateEvent.setOnClickListener(v -> showCreateEventDialog());

        loadEvents();
    }

    private void loadEvents() {
        Log.d(TAG, "Attempting to query events for organizer: " + organizerUser.getHardwareID());

        db.getEventsByOrganizer(organizerUser.getHardwareID()).addOnCompleteListener(task -> {
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }

            if (task.isSuccessful()) {
                List<Event> fetchedEvents = task.getResult();
                if (fetchedEvents == null) {
                    Log.e(TAG, "Query successful but result is null");
                    return;
                }

                Log.d(TAG, "Firebase query successful. Found " + fetchedEvents.size() + " events.");

                eventList.clear();
                eventList.addAll(fetchedEvents);
                adapter.notifyDataSetChanged();

                if (eventList.isEmpty()) {
                    emptyOrganizer.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyOrganizer.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }

            } else {
                Log.e(TAG, "Error running query: ", task.getException());
                Toast.makeText(getContext(), "Error fetching events", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onViewEntrantsClick(Event event) {
        showWaitlistDialog(event);
    }

    private void showWaitlistDialog(Event event) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.popup_organizer_lottery, null);
        builder.setView(dialogView);

        TextView label = dialogView.findViewById(R.id.label1);
        label.setText("Waitlist: " + event.getEventName());

        List<Entrant> waitlistEntrants = event.getWaitListEntrants();
        if (waitlistEntrants == null) {
            waitlistEntrants = new ArrayList<>();
        }

        RecyclerView waitlistRecyclerView = dialogView.findViewById(R.id.entrant_waitlist);
        WaitlistEntrantAdapter waitlistAdapter = new WaitlistEntrantAdapter(waitlistEntrants);
        waitlistRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        waitlistRecyclerView.setAdapter(waitlistAdapter);

        Button runLotteryButton = dialogView.findViewById(R.id.run_lottery);
        runLotteryButton.setVisibility(View.GONE);

        EditText etSampleSize = dialogView.findViewById(R.id.etSampleSize);
        etSampleSize.setVisibility(View.GONE);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showCreateEventDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.popup_organizer_new_or_edit_event, null);
        builder.setView(dialogView);

        EditText editName = dialogView.findViewById(R.id.edit_name);
        EditText editTime = dialogView.findViewById(R.id.edit_time);
        EditText editLocation = dialogView.findViewById(R.id.edit_location);
        EditText editRegStart = dialogView.findViewById(R.id.edit_reg_start);
        EditText editRegEnd = dialogView.findViewById(R.id.edit_reg_end);
        EditText editDetails = dialogView.findViewById(R.id.edit_details);
        Switch switchLimit = dialogView.findViewById(R.id.switchLimit);
        EditText limitNum = dialogView.findViewById(R.id.limit_num);
        Button saveButton = dialogView.findViewById(R.id.save);

        TextView label = dialogView.findViewById(R.id.label1);
        label.setText("Create Event");

        AlertDialog dialog = builder.create();

        saveButton.setOnClickListener(v -> {
            String eventName = editName.getText().toString().trim();
            String startTimeStr = editTime.getText().toString().trim();
            String location = editLocation.getText().toString().trim();
            String regStartStr = editRegStart.getText().toString().trim();
            String regEndStr = editRegEnd.getText().toString().trim();
            String description = editDetails.getText().toString().trim();
            boolean hasLimit = switchLimit.isChecked();
            String limitStr = limitNum.getText().toString().trim();

            if (TextUtils.isEmpty(eventName)) {
                Toast.makeText(getContext(), "Event name is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(startTimeStr)) {
                Toast.makeText(getContext(), "Event start time is required (format: yyyy-MM-dd)", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(location)) {
                Toast.makeText(getContext(), "Event location is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(regStartStr)) {
                Toast.makeText(getContext(), "Registration start time is required (format: yyyy-MM-dd)", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(regEndStr)) {
                Toast.makeText(getContext(), "Registration end time is required (format: yyyy-MM-dd)", Toast.LENGTH_SHORT).show();
                return;
            }
            if (hasLimit && TextUtils.isEmpty(limitStr)) {
                Toast.makeText(getContext(), "Please enter a waitlist limit number", Toast.LENGTH_SHORT).show();
                return;
            }

            Date startTime;
            Date registrationStartTime;
            Date registrationEndTime;

            try {
                startTime = Utils.createWholeDayDate(startTimeStr);
                registrationStartTime = Utils.createWholeDayDate(regStartStr);
                registrationEndTime = Utils.createWholeDayDate(regEndStr);
            } catch (ParseException e) {
                Toast.makeText(getContext(), "Invalid date format. Please use yyyy-MM-dd", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Date parsing error", e);
                return;
            }

            if (registrationStartTime != null && registrationEndTime != null &&
                    registrationStartTime.after(registrationEndTime)) {
                Toast.makeText(getContext(), "Registration start time must be before end time", Toast.LENGTH_SHORT).show();
                return;
            }
            if (startTime != null && registrationEndTime != null &&
                    (startTime.before(registrationEndTime) || startTime.equals(registrationEndTime))) {
                Toast.makeText(getContext(), "Event start time should be after registration end time", Toast.LENGTH_SHORT).show();
                return;
            }

            Integer waitlistLimit = null;
            if (hasLimit && !TextUtils.isEmpty(limitStr)) {
                try {
                    int limit = Integer.parseInt(limitStr);
                    if (limit <= 0) {
                        Toast.makeText(getContext(), "Waitlist limit must be greater than 0", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    waitlistLimit = limit;
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Invalid waitlist limit number", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // All checks passed, now create the event
            createEvent(eventName, description, startTime, location,
                    registrationStartTime, registrationEndTime, waitlistLimit);

            // Dismiss dialog
            dialog.dismiss();
        });

        dialog.show();
    }

    private void createEvent(String eventName, String description, Date startTime, String location,
                             Date registrationStartTime, Date registrationEndTime, Integer waitlistLimit) {
        Log.d(TAG, "Creating new event: " + eventName);

        // Get unique event ID
        db.getUniqueEventID().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Error getting unique event ID", task.getException());
                Toast.makeText(getContext(), "Error creating event: Could not get event ID", Toast.LENGTH_SHORT).show();
                return;
            }

            Long eventID = task.getResult();
            if (eventID == null) {
                Log.e(TAG, "Received null event ID");
                Toast.makeText(getContext(), "Error creating event: Invalid event ID", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create Event object
            Event newEvent = new Event(
                    eventID,
                    eventName,
                    description,
                    startTime,
                    location,
                    registrationStartTime,
                    registrationEndTime,
                    organizerUser.getHardwareID()
            );

            // Set waitlist limit if provided
            if (waitlistLimit != null) {
                newEvent.setWaitlistLimit(waitlistLimit);
            }

            // Save event to database
            db.setEventData(eventID, newEvent).addOnCompleteListener(saveTask -> {
                if (saveTask.isSuccessful()) {
                    Log.d(TAG, "Event created successfully with ID: " + eventID);
                    Toast.makeText(getContext(), "Event created successfully!", Toast.LENGTH_SHORT).show();
                    // Refresh the events list
                    loadEvents();
                } else {
                    Log.e(TAG, "Error saving event to database", saveTask.getException());
                    Toast.makeText(getContext(), "Error creating event: " +
                                    (saveTask.getException() != null ? saveTask.getException().getMessage() : "Unknown error"),
                            Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private class WaitlistEntrantAdapter extends RecyclerView.Adapter<WaitlistEntrantAdapter.ViewHolder> {
        private List<Entrant> entrants;
        public WaitlistEntrantAdapter(List<Entrant> entrants) { this.entrants = entrants; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            tv.setPadding(16, 16, 16, 16);
            tv.setTextSize(16f);
            return new ViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Entrant entrant = entrants.get(position);
            ((TextView) holder.itemView).setText(entrant.getFirstName() + " " + entrant.getLastName());
        }

        @Override public int getItemCount() { return entrants.size(); }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(@NonNull View itemView) { super(itemView); }
        }
    }
}
