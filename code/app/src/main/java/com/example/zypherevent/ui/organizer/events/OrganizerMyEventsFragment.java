package com.example.zypherevent.ui.organizer.events;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
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
import com.example.zypherevent.WaitlistEntry;
import com.example.zypherevent.userTypes.Entrant;
import com.example.zypherevent.userTypes.Organizer;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.OutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
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
    private FloatingActionButton fabCreateEvent;

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

        fabCreateEvent = view.findViewById(R.id.fabCreateEvent);
        fabCreateEvent.setOnClickListener(v -> showCreateEventDialog());

        loadEvents();
    }

    private void loadEvents() {
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

    @Override
    public void onMenuClick(Event event, View anchorView) {
        PopupMenu popup = new PopupMenu(getContext(), anchorView);

        // Inflate your menu file
        popup.getMenuInflater().inflate(R.menu.fragment_organanizer_event_menu, popup.getMenu());

        // Set a listener for menu item clicks
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_notifications) {
                Toast.makeText(getContext(), "Notifications for " + event.getEventName(), Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.action_view_qr) {
                showQRCodeDialog(event);
                return true;
            } else if (id == R.id.action_export_csv) {
                Toast.makeText(getContext(), "Export CSV for " + event.getEventName(), Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.action_edit_event) {
                showEditEventDialog(event);
                return true;
            }
            return false;
        });

        // Show the menu
        popup.show();
    }

    private void showWaitlistDialog(Event event) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.popup_organizer_lottery, null);
        builder.setView(dialogView);

        TextView label = dialogView.findViewById(R.id.label1);
        label.setText("Waitlist: " + event.getEventName());

        List<WaitlistEntry> waitlistEntrants = event.getWaitListEntrants();
        if (waitlistEntrants == null) {
            waitlistEntrants = new ArrayList<>();
        }

        RecyclerView waitlistRecyclerView = dialogView.findViewById(R.id.entrant_waitlist);

        WaitlistEntrantAdapter waitlistAdapter = new WaitlistEntrantAdapter(
                waitlistEntrants,
                (entry, position) -> handleAcceptEntrant(event, entry, position)
        );

        waitlistRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        waitlistRecyclerView.setAdapter(waitlistAdapter);

        Button btnSortNewest = dialogView.findViewById(R.id.btnSortNewest);
        Button btnSortOldest = dialogView.findViewById(R.id.btnSortOldest);
        Button btnSortName = dialogView.findViewById(R.id.btnSortName);

        if (btnSortNewest != null) {
            btnSortNewest.setOnClickListener(v -> waitlistAdapter.sortByNewest());
        }
        if (btnSortOldest != null) {
            btnSortOldest.setOnClickListener(v -> waitlistAdapter.sortByOldest());
        }
        if (btnSortName != null) {
            btnSortName.setOnClickListener(v -> waitlistAdapter.sortByName());
        }

        waitlistAdapter.sortByNewest();

        Button runLotteryButton = dialogView.findViewById(R.id.run_lottery);
        EditText etSampleSize = dialogView.findViewById(R.id.etSampleSize);

        List<WaitlistEntry> finalWaitlistEntrants = waitlistEntrants;

        if (runLotteryButton != null && etSampleSize != null) {
            runLotteryButton.setOnClickListener(v -> {
                String input = etSampleSize.getText().toString().trim();
                if (input.isEmpty()) {
                    Toast.makeText(getContext(), "Please enter a number to sample", Toast.LENGTH_SHORT).show();
                    return;
                }

                int sampleSize;
                try {
                    sampleSize = Integer.parseInt(input);
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Invalid number", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (sampleSize <= 0) {
                    Toast.makeText(getContext(), "Sample size must be greater than 0", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (finalWaitlistEntrants.isEmpty()) {
                    Toast.makeText(getContext(), "No entrants in waitlist", Toast.LENGTH_SHORT).show();
                    return;
                }

                int n = Math.min(sampleSize, finalWaitlistEntrants.size());

                List<WaitlistEntry> shuffled = new ArrayList<>(finalWaitlistEntrants);
                Collections.shuffle(shuffled);

                List<WaitlistEntry> selected = shuffled.subList(0, n);

                StringBuilder sb = new StringBuilder();
                for (WaitlistEntry entry : selected) {
                    Entrant e = entry.getEntrant();
                    String name;
                    if (e != null) {
                        String first = e.getFirstName() != null ? e.getFirstName() : "";
                        String last = e.getLastName() != null ? e.getLastName() : "";
                        name = (first + " " + last).trim();
                        if (name.isEmpty()) {
                            name = "Unnamed entrant";
                        }
                    } else {
                        name = "Unknown entrant";
                    }

                    if (sb.length() > 0) sb.append(", ");
                    sb.append(name);
                }

                Toast.makeText(
                        getContext(),
                        "Selected " + n + " entrant(s): " + sb.toString(),
                        Toast.LENGTH_LONG
                ).show();
            });
        }

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Handle accepting an entrant from the waitlist
     */
    private void handleAcceptEntrant(Event event, WaitlistEntry entry, int position) {
        AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(getContext());
        confirmBuilder.setTitle("Accept Entrant");
        confirmBuilder.setMessage("Accept " + entry.getEntrant().getFirstName() + " " +
                entry.getEntrant().getLastName() + " for this event?");

        confirmBuilder.setPositiveButton("Accept", (dialog, which) -> {
            db.moveEntrantToAccepted(event.getUniqueEventID().toString(), entry.getEntrant());
            Toast.makeText(getContext(),
                    entry.getEntrant().getFirstName() + " accepted!",
                    Toast.LENGTH_SHORT).show();

            // Refresh the waitlist
            loadEvents();
        });

        confirmBuilder.setNegativeButton("Cancel", null);
        confirmBuilder.show();
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

        // Initially hide limit_num if switch is off
        limitNum.setVisibility(switchLimit.isChecked() ? View.VISIBLE : View.GONE);

        // Add listener to show/hide limit field based on switch
        switchLimit.setOnCheckedChangeListener((buttonView, isChecked) -> {
            limitNum.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                limitNum.setText(""); // Clear the field when unchecked
            }
        });

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
            Switch switchGeolocation = dialogView.findViewById(R.id.require_geolocation);

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

            boolean requiresGeolocation = switchGeolocation.isChecked();

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

            createEvent(eventName, description, startTime, location,
                    registrationStartTime, registrationEndTime, waitlistLimit, requiresGeolocation);

            dialog.dismiss();
        });

        dialog.show();
    }

    private void createEvent(String eventName, String description, Date startTime, String location,
                             Date registrationStartTime, Date registrationEndTime, Integer waitlistLimit, boolean requiresGeolocation) {

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

            Event newEvent = new Event(
                    eventID,
                    eventName,
                    description,
                    startTime,
                    location,
                    registrationStartTime,
                    registrationEndTime,
                    organizerUser.getHardwareID(),
                    requiresGeolocation
            );

            if (waitlistLimit != null) {
                newEvent.setWaitlistLimit(waitlistLimit);
            }

            db.setEventData(eventID, newEvent).addOnCompleteListener(saveTask -> {
                if (saveTask.isSuccessful()) {
                    Log.d(TAG, "Event created successfully with ID: " + eventID);
                    Toast.makeText(getContext(), "Event created successfully!", Toast.LENGTH_SHORT).show();
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

    private void showEditEventDialog(Event event) {
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
        Switch switchGeolocation = dialogView.findViewById(R.id.require_geolocation);
        Button saveButton = dialogView.findViewById(R.id.save);

        TextView label = dialogView.findViewById(R.id.label1);
        label.setText("Edit Event");

        // Populate fields with existing event data
        editName.setText(event.getEventName());
        if (event.getStartTime() != null) {
            editTime.setText(Utils.formatDateForDisplay(event.getStartTime()));
        }
        editLocation.setText(event.getLocation());
        if (event.getRegistrationStartTime() != null) {
            editRegStart.setText(Utils.formatDateForDisplay(event.getRegistrationStartTime()));
        }
        if (event.getRegistrationEndTime() != null) {
            editRegEnd.setText(Utils.formatDateForDisplay(event.getRegistrationEndTime()));
        }
        if (event.getRequiresGeolocation()) {
            switchGeolocation.setChecked(true);
        }
        editDetails.setText(event.getEventDescription());

        // Set waitlist limit fields
        Integer currentLimit = event.getWaitlistLimit();
        if (currentLimit != null) {
            switchLimit.setChecked(true);
            limitNum.setText(String.valueOf(currentLimit));
            limitNum.setVisibility(View.VISIBLE);
        } else {
            switchLimit.setChecked(false);
            limitNum.setVisibility(View.GONE);
        }

        // Add listener to show/hide limit field based on switch
        switchLimit.setOnCheckedChangeListener((buttonView, isChecked) -> {
            limitNum.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                limitNum.setText(""); // Clear the field when unchecked
            }
        });

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
            boolean requiresGeolocation = switchGeolocation.isChecked();

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
                    // Check if new limit is less than current waitlist size
                    int currentWaitlistSize = event.getWaitListEntrants() != null ? event.getWaitListEntrants().size() : 0;
                    if (limit < currentWaitlistSize) {
                        Toast.makeText(getContext(), "Waitlist limit cannot be less than current waitlist size (" + currentWaitlistSize + ")", Toast.LENGTH_LONG).show();
                        return;
                    }
                    waitlistLimit = limit;
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Invalid waitlist limit number", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            updateEvent(event.getUniqueEventID(), eventName, description, startTime, location,
                    registrationStartTime, registrationEndTime, waitlistLimit, requiresGeolocation);

            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateEvent(Long eventId, String eventName, String description, Date startTime, String location,
                             Date registrationStartTime, Date registrationEndTime, Integer waitlistLimit, boolean requiresGeolocation) {
        Log.d(TAG, "Updating event: " + eventName);

        Event updatedEvent = new Event(
                eventId,
                eventName,
                description,
                startTime,
                location,
                registrationStartTime,
                registrationEndTime,
                organizerUser.getHardwareID(),
                requiresGeolocation
        );

        updatedEvent.setWaitlistLimit(waitlistLimit);

        db.setEventData(eventId, updatedEvent).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Event updated successfully with ID: " + eventId);
                Toast.makeText(getContext(), "Event updated successfully!", Toast.LENGTH_SHORT).show();
                loadEvents();
            } else {
                Log.e(TAG, "Error updating event in database", task.getException());
                Toast.makeText(getContext(), "Error updating event: " +
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * Saves the QR code bitmap to device Downloads folder.
     * Uses MediaStore API for Android 10+ compatibility.
     *
     * @param bitmap    The QR code bitmap to save
     * @param eventName The event name used for the filename
     */
    private void saveQRCodeToDownloads(Bitmap bitmap, String eventName) {
        try {
            String fileName = "QR_" + eventName.replaceAll("[^a-zA-Z0-9]", "_") + "_" + System.currentTimeMillis() + ".png";

            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "image/png");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri uri = requireContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    outputStream.close();
                    Toast.makeText(getContext(), "QR code saved to Downloads", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving QR code", e);
            Toast.makeText(getContext(), "Failed to save QR code", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Displays a dialog showing the event's QR code with download and share options.
     * QR code encodes the event ID in format "EVENT:{id}" for scanning.
     * <p>
     * Flow:
     * 1. Generates QR code bitmap using Utils.generateQRCode()
     * 2. Displays in dialog with event name and ID
     * 3. Provides Download and Share buttons
     *
     * @param event The event to generate a QR code for
     */
    private void showQRCodeDialog(Event event) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_qr_code, null);

        TextView title = dialogView.findViewById(R.id.tvQRTitle);
        ImageView qrImageView = dialogView.findViewById(R.id.ivQRCode);
        TextView eventIdText = dialogView.findViewById(R.id.tvEventId);
        Button downloadButton = dialogView.findViewById(R.id.btnDownloadQR);
        Button shareButton = dialogView.findViewById(R.id.btnShareQR);

        title.setText("QR Code: " + event.getEventName());
        eventIdText.setText("Event ID: " + event.getUniqueEventID());

        // Generate QR code bitmap (512x512 pixels)
        Bitmap qrBitmap = Utils.generateQRCode(event.getUniqueEventID(), 512, 512);
        if (qrBitmap != null) {
            qrImageView.setImageBitmap(qrBitmap);
        } else {
            Toast.makeText(getContext(), "Failed to generate QR code", Toast.LENGTH_SHORT).show();
            return;
        }

        downloadButton.setOnClickListener(v -> saveQRCodeToDownloads(qrBitmap, event.getEventName()));
        shareButton.setOnClickListener(v -> shareQRCode(qrBitmap, event.getEventName()));

        builder.setView(dialogView);
        builder.setPositiveButton("Close", null);
        builder.create().show();
    }

    /**
     * Opens Android share sheet to share QR code via any app (email, messaging, social media).
     * Saves QR code to Pictures folder temporarily, then creates share intent.
     *
     * @param bitmap    The QR code bitmap to share
     * @param eventName The event name used for the filename and share text
     */
    private void shareQRCode(Bitmap bitmap, String eventName) {
        try {
            String fileName = "QR_" + eventName.replaceAll("[^a-zA-Z0-9]", "_") + ".png";

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

            Uri uri = requireContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    outputStream.close();

                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("image/png");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    shareIntent.putExtra(Intent.EXTRA_TEXT, "QR Code for event: " + eventName);
                    startActivity(Intent.createChooser(shareIntent, "Share QR Code"));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sharing QR code", e);
            Toast.makeText(getContext(), "Failed to share QR code", Toast.LENGTH_SHORT).show();
        }
    }
}
