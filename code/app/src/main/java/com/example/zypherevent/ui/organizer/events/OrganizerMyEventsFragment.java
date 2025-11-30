package com.example.zypherevent.ui.organizer.events;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
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
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Fragment that displays and manages events created by the current Organizer.
 * shows a scrollable list of the organizer's events and provides actions to
 * create new events, edit existing ones,
 * export accepted entrants as CSV, run waitlist lotteries, and generate or
 * share QR codes for
 * event check-in or promotion.
 * Also allows the Organizer to send notifications to certain status group
 * entrants notifications
 * It serves as the main event management screen for organizer users.
 */
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

    /**
     * Default no-argument constructor required by the Fragment framework.
     */
    public OrganizerMyEventsFragment() {
    }

    /**
     * Inflates the layout for this fragment.
     * This method inflates and returns the root view.
     * Further view lookups and initialization are performed in onViewCreated(View,
     * Bundle)
     *
     * @param inflater           the LayoutInflater used to inflate the layout XML
     * @param container          the parent view that the fragment's UI should
     *                           attach to, or null
     * @param savedInstanceState previously saved state, or null if none
     * @return the root view for this fragment's UI
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_organizer_my_events, container, false);
    }

    /**
     * Called after the fragment's view has been created.
     * This method initializes the Database instance, resolves the current
     * Organizer from the hosting OrganizerActivity, wires up the RecyclerView,
     * adapter,
     * empty-state view and the "Create Event" floating action button. If organizer
     * information is unavailable,
     * an error is logged and shown to the user and initialization is aborted.
     * Finally,
     * it triggers the initial load of organizer events via loadEvents()
     *
     * @param view               the fragment's root view
     * @param savedInstanceState previously saved instance state, or null if none
     */
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

        swipeRefreshLayout.setOnRefreshListener(this::loadEvents);

        fabCreateEvent = view.findViewById(R.id.fabCreateEvent);
        fabCreateEvent.setOnClickListener(v -> showCreateEventDialog());

        loadEvents();
    }

    /**
     * Loads events created by the current organizer from the database.
     * This method queries the database for all {@link Event} instances associated
     * with the
     * organizer's hardware ID, updates the event list and its adapter with the
     * fetched results,
     * and toggles the empty state view depending on whether any events are
     * returned. The swipe
     * refresh indicator is stopped when the query completes, and any errors are
     * logged and shown
     * to the user via a toast message.
     */
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

    /**
     * Handles the "View Waitlist" click action for a given event.
     * When the organizer chooses to view entrants for an event, this method opens
     * the
     * waitlist dialog by delegating to showWaitlistDialog(Event)
     *
     * @param event the event whose waitlisted entrants should be displayed
     */
    @Override
    public void onViewEntrantsClick(Event event) {
        showWaitlistDialog(event);
    }

    /**
     * Handles the "Entrant List" button click for a given event.
     * This method navigates to a dedicated fragment that displays accepted and
     * declined
     * entrants for the selected event. The event's unique ID is passed in a Bundle
     * so the destination fragment can load the correct data.
     *
     * @param event the event whose entrant lists should be viewed
     */
    @Override
    public void onEntrantListClick(Event event) {
        if (event.getUniqueEventID() == null) {
            Toast.makeText(getContext(), "Error: Event ID is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putLong("eventId", event.getUniqueEventID());

        Navigation.findNavController(requireView()).navigate(R.id.nav_view_entrants_list, bundle);
    }

    /**
     * Handles overflow menu actions for a specific event row.
     * This method shows a PopupMenu anchored to the given view, and responds to
     * menu selections by displaying notifications, showing the event QR code,
     * exporting
     * accepted entrants as CSV, or opening the edit-event dialog.
     *
     * @param event      the event associated with the clicked menu
     * @param anchorView the view used as an anchor for the popup menu
     */
    @Override
    public void onMenuClick(Event event, View anchorView) {
        PopupMenu popup = new PopupMenu(getContext(), anchorView);
        popup.getMenuInflater().inflate(R.menu.fragment_organanizer_event_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_notifications) {
                showSendNotificationDialog(event);
                return true;
            } else if (id == R.id.action_view_qr) {
                showQRCodeDialog(event);
                return true;
            } else if (id == R.id.action_export_csv) {
                showCSVDialog(event);
                return true;
            } else if (id == R.id.action_edit_event) {
                showEditEventDialog(event);
                return true;
            }
            return false;
        });

        popup.show();
    }

    /**
     * Shows a dialog for sending customizable notifications to entrants based on
     * their status.
     *
     * @param event the event for which to send notifications
     */
    private void showSendNotificationDialog(Event event) {
        if (event.getUniqueEventID() == null) {
            Toast.makeText(getContext(), "Error: Event ID is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (organizerUser == null) {
            Toast.makeText(getContext(), "Error: Organizer information not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_notifications, null);
        builder.setView(dialogView);

        Spinner statusSpinner = dialogView.findViewById(R.id.dropdown);
        EditText editHeader = dialogView.findViewById(R.id.edit_notification_header);
        EditText editBody = dialogView.findViewById(R.id.edit_notification_body);
        TextView charCounter = dialogView.findViewById(R.id.character_counter);
        Button useTemplateButton = dialogView.findViewById(R.id.use_template_button);
        Button sendButton = dialogView.findViewById(R.id.send_button);

        String[] statusOptions = { "Waitlisted", "Accepted", "Declined" };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                statusOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(adapter);

        final String[] selectedStatus = { null };

        editBody.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int after) {
                int length = s.length();
                charCounter.setText(length + "/500");
                if (length > 500) {
                    charCounter.setTextColor(Color.RED);
                } else {
                    charCounter.setTextColor(Color.GRAY);
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });

        statusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedStatus[0] = statusOptions[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedStatus[0] = null;
            }
        });

        useTemplateButton.setOnClickListener(v -> {
            if (selectedStatus[0] != null) {
                populateTemplate(editHeader, editBody, selectedStatus[0], event.getEventName());
                Toast.makeText(getContext(), "Template loaded", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Please select a status group first", Toast.LENGTH_SHORT).show();
            }
        });

        AlertDialog dialog = builder.create();

        sendButton.setOnClickListener(v -> {
            if (selectedStatus[0] == null || selectedStatus[0].isEmpty()) {
                new AlertDialog.Builder(getContext())
                        .setTitle("No Group Selected")
                        .setMessage("Please select a group from the dropdown menu before sending notifications.")
                        .setPositiveButton("OK", null)
                        .show();
                return;
            }

            String customHeader = editHeader.getText().toString().trim();
            String customBody = editBody.getText().toString().trim();

            if (TextUtils.isEmpty(customHeader)) {
                Toast.makeText(getContext(), "Please enter a notification title", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(customBody)) {
                Toast.makeText(getContext(), "Please enter a notification message", Toast.LENGTH_SHORT).show();
                return;
            }
            if (customBody.length() > 500) {
                Toast.makeText(getContext(), "Message too long (max 500 characters)", Toast.LENGTH_SHORT).show();
                return;
            }

            sendButton.setEnabled(false);
            sendNotificationsToStatus(event, selectedStatus[0], customHeader, customBody, sendButton, dialog);
        });

        dialog.show();
    }

    /**
     * Populate the notification fields with a template based on the selected status
     *
     * @param headerField The EditText for the notification header
     * @param bodyField   The EditText for the notification body
     * @param status      The selected status group
     * @param eventName   The name of the event
     */
    private void populateTemplate(EditText headerField, EditText bodyField, String status, String eventName) {
        String header = "";
        String body = "";

        switch (status) {
            case "Accepted":
                header = "Event Starting Soon: " + eventName;
                body = "This is a reminder that you have been selected for this event. " +
                        "Please check the event details and prepare accordingly. We look forward to seeing you!";
                break;

            case "Waitlisted":
                header = "Event Update: " + eventName;
                body = "You are currently on the waitlist for this event. " +
                        "We will notify you if your status changes.";
                break;

            case "Declined":
                header = "Thank you: " + eventName;
                body = "Thank you for your interest in this event." +
                        "Unfortunately, all available spots have been filled. " +
                        "Please watch for future events.";
                break;
        }

        headerField.setText(header);
        bodyField.setText(body);
    }

    /**
     * Sends custom notifications to entrants based on their status for the given
     * event.
     *
     * @param event          The event to send notifications for
     * @param selectedStatus The status group to notify ("Waitlisted", "Accepted",
     *                       "Denied")
     * @param customHeader   The custom notification title
     * @param customBody     The custom notification message
     * @param sendButton     The send button to re-enable after completion
     * @param dialog         The dialog to dismiss after sending
     */
    private void sendNotificationsToStatus(Event event, String selectedStatus, String customHeader, String customBody,
            Button sendButton, AlertDialog dialog) {
        List<String> entrantIds = new ArrayList<>();

        switch (selectedStatus) {
            case "Waitlisted":
                if (event.getWaitListEntrants() != null) {
                    for (WaitlistEntry entry : event.getWaitListEntrants()) {
                        if (entry != null && entry.getEntrantHardwareID() != null) {
                            entrantIds.add(entry.getEntrantHardwareID());
                        }
                    }
                }
                break;

            case "Accepted":
                if (event.getInvitedEntrants() != null) {
                    entrantIds.addAll(event.getInvitedEntrants());
                }
                if (event.getAcceptedEntrants() != null) {
                    entrantIds.addAll(event.getAcceptedEntrants());
                }
                break;

            case "Declined":
                if (event.getDeclinedEntrants() != null) {
                    entrantIds.addAll(event.getDeclinedEntrants());
                }
                break;
        }

        if (entrantIds.isEmpty()) {
            new AlertDialog.Builder(getContext())
                    .setTitle("No Entrant Found")
                    .setMessage("There are currently no " + selectedStatus + " entrants for this event.")
                    .setPositiveButton("OK", null)
                    .show();
            sendButton.setEnabled(true);
            return;
        }

        List<String> uniqueIds = new ArrayList<>(new java.util.HashSet<>(entrantIds));

        new AlertDialog.Builder(getContext())
                .setTitle("Confirm Notification Send")
                .setMessage("Send notifications to " + uniqueIds.size() + " " + selectedStatus.toLowerCase()
                        + " entrant(s)?")
                .setPositiveButton("Send", (confirmDialog, which) -> {
                    sendBulkNotificationsViaDatabase(uniqueIds, customHeader, customBody, () -> {
                        Toast.makeText(getContext(), "Sent notifications to " + uniqueIds.size() + " entrants.",
                                Toast.LENGTH_LONG).show();
                        sendButton.setEnabled(true);
                        dialog.dismiss();
                    }, () -> {
                        Toast.makeText(getContext(), "Failed to send some notifications.", Toast.LENGTH_SHORT).show();
                        sendButton.setEnabled(true);
                    });
                })
                .setNegativeButton("Cancel", (confirmDialog, which) -> {
                    sendButton.setEnabled(true);
                })
                .show();
    }

    /**
     * Sends bulk notifications using Database directly.
     */
    private void sendBulkNotificationsViaDatabase(List<String> entrantIds, String header, String body,
            Runnable onSuccess, Runnable onFailure) {
        int totalEntrants = entrantIds.size();
        final int[] successCount = { 0 };
        final int[] failureCount = { 0 };

        for (String entrantId : entrantIds) {
            db.getUniqueNotificationID()
                    .addOnSuccessListener(notificationID -> {
                        com.example.zypherevent.Notification notification = new com.example.zypherevent.Notification(
                                notificationID,
                                organizerUser.getHardwareID(),
                                entrantId,
                                header,
                                body,
                                null,
                                false);

                        db.setNotificationData(notificationID, notification)
                                .addOnSuccessListener(v -> {
                                    successCount[0]++;
                                    Log.d(TAG, "Notification sent to: " + entrantId);
                                    if (successCount[0] + failureCount[0] == totalEntrants) {
                                        onSuccess.run();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    failureCount[0]++;
                                    Log.e(TAG, "Failed to send notification to: " + entrantId, e);
                                    if (successCount[0] + failureCount[0] == totalEntrants) {
                                        if (successCount[0] > 0) {
                                            onSuccess.run();
                                        } else {
                                            onFailure.run();
                                        }
                                    }
                                });
                    })
                    .addOnFailureListener(e -> {
                        failureCount[0]++;
                        Log.e(TAG, "Failed to get notification ID for: " + entrantId, e);
                        if (successCount[0] + failureCount[0] == totalEntrants) {
                            if (successCount[0] > 0) {
                                onSuccess.run();
                            } else {
                                onFailure.run();
                            }
                        }
                    });
        }
    }

    /**
     * Builds and displays a dialog containing the CSV export for the given event.
     * This method calls exportCSV(Event) to generate a comma-separated list
     * of accepted entrant names, displays the result in a dialog, optionally shows
     * a
     * registration warning if the event is still open, and provides a button to
     * save
     * the CSV to the device's Downloads folder.
     *
     * @param event the event whose accepted entrants should be exported
     */
    private void showCSVDialog(Event event) {
        exportCSV(event).addOnSuccessListener(csvString -> {
            Log.d("CSV", csvString);

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

            View dialogView = getLayoutInflater().inflate(R.layout.dialog_csv, null);

            TextView registrationWarning = dialogView.findViewById(R.id.tvCSVWarning);

            Log.d("OrganizerMyEvents", event.getEventName() + " isRegistrationOpen: " + event.isRegistrationOpen());
            if (event.isRegistrationOpen()) {
                registrationWarning.setVisibility(View.VISIBLE);
            } else {
                registrationWarning.setVisibility(View.GONE);
            }

            TextView csvContent = dialogView.findViewById(R.id.tvCopyPaste);
            Button downloadButton = dialogView.findViewById(R.id.btnDownloadCSV);

            csvContent.setText(csvString);

            downloadButton.setOnClickListener(v -> saveCSVToDownloads(csvString, event.getEventName()));

            builder.setView(dialogView);
            builder.setPositiveButton("Close", null);
            builder.create().show();
        }).addOnFailureListener(e -> {
            Log.e("CSV", "Failed to export CSV", e);
        });
    }

    /**
     * Saves a CSV string to the system Downloads directory using the MediaStore
     * API.
     * The file name is derived from the event name and the current timestamp, and
     * the
     * CSV content is written using UTF-8 encoding. A toast message indicates
     * success
     * or failure.
     *
     * @param csvString the CSV content to be written to disk
     * @param eventName the event name used to construct the output file name
     */
    private void saveCSVToDownloads(String csvString, String eventName) {
        try {
            String fileName = eventName.replaceAll("[^a-zA-Z0-9]", "_") + "_" + System.currentTimeMillis() + ".csv";

            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri uri = requireContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

            if (uri != null) {
                OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri);

                if (outputStream != null) {
                    outputStream.write(csvString.getBytes(StandardCharsets.UTF_8));
                    outputStream.close();

                    Toast.makeText(getContext(), "CSV saved to Downloads", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving CSV", e);
            Toast.makeText(getContext(), "Failed to save CSV", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Exports accepted entrants for an event as a CSV-formatted string.
     * This method resolves all accepted entrant IDs to Entrant instances, then
     * constructs a comma-separated list of their full names. If there are no
     * accepted
     * entrants, an empty string is returned.
     *
     * @param event the event whose accepted entrants should be exported
     * @return a Task that completes with a CSV string of accepted entrant names
     */
    private Task<String> exportCSV(Event event) {
        ArrayList<String> acceptedList = event.getAcceptedEntrants();
        if (acceptedList == null || acceptedList.isEmpty()) {
            return Tasks.forResult("");
        }

        List<Task<Entrant>> userTasks = new ArrayList<>();

        for (String entrantId : acceptedList) {
            Task<Entrant> t = db.getUser(entrantId).continueWith(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                return (Entrant) task.getResult();
            });
            userTasks.add(t);
        }

        return Tasks.whenAllSuccess(userTasks).continueWith(task -> {
            @SuppressWarnings("unchecked")
            List<Entrant> entrants = (List<Entrant>) (List<?>) task.getResult();

            List<String> names = new ArrayList<>();
            for (Entrant e : entrants) {
                names.add(e.getFirstName() + " " + e.getLastName());
            }
            return String.join(", ", names);
        });
    }

    /**
     * Displays a dialog showing the current waitlist and lottery tools for an
     * event.
     * The dialog lists all waitlisted entrants, provides sorting options, and
     * allows the organizer
     * to run a random lottery that moves a chosen sample of entrants into the
     * invited list.
     * Entrants can also be individually accepted. After running
     * a lottery, the event list is refreshed to keep the UI in sync.
     *
     * @param event the event whose waitlist should be displayed and managed
     */
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

        WaitlistEntrantAdapter[] waitlistAdapterHolder = new WaitlistEntrantAdapter[1];
        waitlistAdapterHolder[0] = new WaitlistEntrantAdapter(waitlistEntrants,
                (entry, position) -> handleAcceptEntrant(event, entry, position));

        WaitlistEntrantAdapter waitlistAdapter = waitlistAdapterHolder[0];

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
        Button btnDrawReplacement = dialogView.findViewById(R.id.btn_draw_replacement);

        if (btnDrawReplacement != null) {
            btnDrawReplacement.setOnClickListener(v -> drawReplacementFromPool(event));
        }

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

                int invitedCount = 0;

                for (WaitlistEntry entry : selected) {

                    String hardwareId = entry.getEntrantHardwareID();
                    if (hardwareId == null || hardwareId.isEmpty())
                        continue;

                    Entrant stubEntrant = new Entrant(hardwareId, "", "", "");

                    db.moveEntrantToInvited(event.getUniqueEventID().toString(), stubEntrant)
                            .addOnSuccessListener(unused -> {
                            }).addOnFailureListener(
                                    err -> Log.e(TAG, "Failed to move entrant to invited: " + err.getMessage()));

                    invitedCount++;
                }

                Toast.makeText(getContext(), "Selected and invited " + invitedCount + " entrant(s).", Toast.LENGTH_LONG)
                        .show();

                loadEvents();
            });
        }

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Handles accepting a single entrant from the waitlist.
     * This method looks up the entrant by hardware ID, shows a confirmation dialog
     * that
     * includes the entrant's name, and moves the entrant to the accepted list if
     * the
     * organizer confirms. On success, the event list is reloaded; on failure, an
     * error
     * toast is shown.
     *
     * @param event    the event for which the entrant is being accepted
     * @param entry    the waitlist entry representing the entrant
     * @param position the adapter position of the entry (currently unused)
     */
    private void handleAcceptEntrant(Event event, WaitlistEntry entry, int position) {
        if (getContext() == null)
            return;

        String hardwareId = entry.getEntrantHardwareID();
        if (hardwareId == null || hardwareId.isEmpty()) {
            Toast.makeText(getContext(), "Invalid entrant", Toast.LENGTH_SHORT).show();
            return;
        }

        db.getUser(hardwareId).addOnSuccessListener(user -> {
            if (!(user instanceof Entrant)) {
                Toast.makeText(getContext(), "User is not an entrant or could not be loaded", Toast.LENGTH_SHORT)
                        .show();
                return;
            }

            Entrant entrant = (Entrant) user;
            String first = entrant.getFirstName() != null ? entrant.getFirstName() : "";
            String last = entrant.getLastName() != null ? entrant.getLastName() : "";
            String fullName = (first + " " + last).trim();
            if (fullName.isEmpty()) {
                fullName = "this entrant";
            }

            AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(getContext());
            confirmBuilder.setTitle("Accept Entrant");
            confirmBuilder.setMessage("Accept " + fullName + " for this event?");

            String finalFullName = fullName;
            confirmBuilder.setPositiveButton("Accept", (dialog, which) -> {
                db.moveEntrantToAccepted(event.getUniqueEventID().toString(), entrant).addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(), finalFullName + " accepted!", Toast.LENGTH_SHORT).show();
                    loadEvents();
                }).addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to accept entrant: " + e.getMessage(), Toast.LENGTH_SHORT)
                            .show();
                });
            });

            confirmBuilder.setNegativeButton("Cancel", null);
            confirmBuilder.show();
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Failed to load entrant: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Draws one replacement applicant from the events waitlist "pool"
     * moves them to into the invited list
     *
     * Used when a previously selected applicant cancels or rejects invitation,
     * organizer can pull a new entrant from the pool
     *
     * @param event the event to draw a replacement entrant
     */

    private void drawReplacementFromPool(Event event) {
        if (event == null || event.getUniqueEventID() == null) {
            return;
        }

        ArrayList<WaitlistEntry> waitlist = event.getWaitListEntrants();
        if (waitlist == null || waitlist.isEmpty()) {
            Toast.makeText(getContext(), "No entrants left in the waitlist pool.", Toast.LENGTH_SHORT).show();
            return;

        }

        ArrayList<String> invited = event.getInvitedEntrants();
        ArrayList<String> accepted = event.getAcceptedEntrants();
        ArrayList<String> declined = event.getDeclinedEntrants();
        ArrayList<String> cancelled = event.getCancelledEntrants();



        if (invited == null)
            invited = new ArrayList<>();
        if (accepted == null)
            accepted = new ArrayList<>();
        if (declined == null)
            declined = new ArrayList<>();
        if (cancelled == null)
            cancelled = new ArrayList<>();


        ArrayList<WaitlistEntry> eligible = new ArrayList<>();
        for (WaitlistEntry entry : waitlist) {
            if (entry == null)
                continue;
            String id = entry.getEntrantHardwareID();
            if (id == null || id.isEmpty())
                continue;

            if (invited.contains(id) || accepted.contains(id) || declined.contains(id) || cancelled.contains(id)) {
                continue;
            }
            eligible.add(entry);
        }
        if (eligible.isEmpty()) {
            Toast.makeText(getContext(),
                    "No eligible entrants left in the pool",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        Collections.shuffle(eligible);
        WaitlistEntry chosen = eligible.get(0);
        String chosenId = chosen.getEntrantHardwareID();

        Entrant stubEntrant = new Entrant(chosenId, "", "", "");

        db.moveEntrantToInvited(event.getUniqueEventID().toString(), stubEntrant)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(),
                            "Replacement entrant invited from the pool.",
                            Toast.LENGTH_SHORT).show();
                    loadEvents();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to move replacement to invited: " + e.getMessage(), e);
                    Toast.makeText(getContext(),
                            "Error inviting replacement entrant.",
                            Toast.LENGTH_SHORT).show();
                });

    }

    /**
     * Shows a dialog for creating a new event.
     * The dialog collects basic event information, registration window dates,
     * optional
     * waitlist limits, lottery criteria, and whether geolocation is required. All
     * inputs
     * are validated (including date ordering and numeric limits), and on success
     * createEvent is invoked to persist the new event.
     */
    private void showCreateEventDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.popup_organizer_new_or_edit_event, null);
        builder.setView(dialogView);

        EditText editName = dialogView.findViewById(R.id.edit_name);
        Button btnEventDate = dialogView.findViewById(R.id.btn_event_date);
        EditText editLocation = dialogView.findViewById(R.id.edit_location);
        Button btnRegStart = dialogView.findViewById(R.id.btn_reg_start);
        Button btnRegEnd = dialogView.findViewById(R.id.btn_reg_end);
        EditText editLotteryCriteria = dialogView.findViewById(R.id.edit_lottery_criteria);
        EditText editDescription = dialogView.findViewById(R.id.edit_description);
        EditText posterUrlInput = dialogView.findViewById(R.id.editPosterUrl);
        Switch switchLimit = dialogView.findViewById(R.id.switchLimit);
        EditText limitNum = dialogView.findViewById(R.id.limit_num);
        Switch requireGeolocation = dialogView.findViewById(R.id.require_geolocation);
        Button saveButton = dialogView.findViewById(R.id.save);

        TextView label = dialogView.findViewById(R.id.label1);
        label.setText("Create Event");

        final Date[] eventDate = { null };
        final Date[] regStartDate = { null };
        final Date[] regEndDate = { null };
        SimpleDateFormat displayFormat = new SimpleDateFormat("MMM d, yyyy");

        btnEventDate.setOnClickListener(v -> showDatePickerDialog(date -> {
            eventDate[0] = date;
            btnEventDate.setText(displayFormat.format(date));
            btnEventDate.setTextColor(getResources().getColor(android.R.color.primary_text_light, null));
        }));

        btnRegStart.setOnClickListener(v -> showDatePickerDialog(date -> {
            regStartDate[0] = date;
            btnRegStart.setText(displayFormat.format(date));
            btnRegStart.setTextColor(getResources().getColor(android.R.color.primary_text_light, null));
        }));

        btnRegEnd.setOnClickListener(v -> showDatePickerDialog(date -> {
            regEndDate[0] = date;
            btnRegEnd.setText(displayFormat.format(date));
            btnRegEnd.setTextColor(getResources().getColor(android.R.color.primary_text_light, null));
        }));

        limitNum.setVisibility(switchLimit.isChecked() ? View.VISIBLE : View.GONE);

        switchLimit.setOnCheckedChangeListener((buttonView, isChecked) -> {
            limitNum.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                limitNum.setText("");
            }
        });

        AlertDialog dialog = builder.create();

        saveButton.setOnClickListener(v -> {
            String eventName = editName.getText().toString().trim();
            String location = editLocation.getText().toString().trim();
            String lotteryCriteria = editLotteryCriteria.getText().toString().trim();
            String eventDescription = editDescription.getText().toString().trim();
            String posterUrl = posterUrlInput.getText().toString().trim();
            boolean hasLimit = switchLimit.isChecked();
            String limitStr = limitNum.getText().toString().trim();
            boolean requiresGeo = requireGeolocation.isChecked();

            if (TextUtils.isEmpty(eventName)) {
                Toast.makeText(getContext(), "Event name is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (eventDate[0] == null) {
                Toast.makeText(getContext(), "Please select an event date", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(location)) {
                Toast.makeText(getContext(), "Event location is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (regStartDate[0] == null) {
                Toast.makeText(getContext(), "Please select registration start date", Toast.LENGTH_SHORT).show();
                return;
            }
            if (regEndDate[0] == null) {
                Toast.makeText(getContext(), "Please select registration end date", Toast.LENGTH_SHORT).show();
                return;
            }
            if (hasLimit && TextUtils.isEmpty(limitStr)) {
                Toast.makeText(getContext(), "Please enter a waitlist limit number", Toast.LENGTH_SHORT).show();
                return;
            }

            Date startTime = eventDate[0];
            Date registrationStartTime = regStartDate[0];
            Date registrationEndTime = regEndDate[0];

            if (registrationStartTime != null && registrationEndTime != null
                    && registrationStartTime.after(registrationEndTime)) {
                Toast.makeText(getContext(), "Registration start time must be before end time", Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            if (startTime != null && registrationEndTime != null
                    && (startTime.before(registrationEndTime) || startTime.equals(registrationEndTime))) {
                Toast.makeText(getContext(), "Event start time should be after registration end time",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Integer waitlistLimit = null;
            if (hasLimit && !TextUtils.isEmpty(limitStr)) {
                try {
                    int limit = Integer.parseInt(limitStr);
                    if (limit <= 0) {
                        Toast.makeText(getContext(), "Waitlist limit must be greater than 0", Toast.LENGTH_SHORT)
                                .show();
                        return;
                    }
                    waitlistLimit = limit;
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Invalid waitlist limit number", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            createEvent(eventName, eventDescription, startTime, location,
                    registrationStartTime, registrationEndTime,
                    waitlistLimit, lotteryCriteria, posterUrl, requiresGeo);

            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Creates a new event and saves it to the database.
     * This method obtains a unique event ID, constructs an Event using the supplied
     * details, applies any optional waitlist limit and lottery criteria, and
     * updates the
     * organizer's list of created events. Both the organizer record and event
     * record are
     * written to the database, and the event list is refreshed on success.
     *
     * @param eventName             the name of the event
     * @param eventDescription      a brief description of the event
     * @param startTime             the date when the event occurs
     * @param location              the physical or virtual location of the event
     * @param registrationStartTime the date when registration opens
     * @param registrationEndTime   the date when registration closes
     * @param waitlistLimit         the maximum allowed waitlist size, or null for
     *                              no limit
     * @param lotteryCriteria       optional textual description of lottery criteria
     * @param posterUrl             the Firebase path to the event's optional
     *                              promotional poster
     * @param requiresGeolocation   whether geolocation is required for entrant
     *                              registration
     */
    private void createEvent(String eventName,
            String eventDescription,
            Date startTime,
            String location,
            Date registrationStartTime,
            Date registrationEndTime,
            Integer waitlistLimit,
            String lotteryCriteria,
            String posterUrl,
            boolean requiresGeolocation) {

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
                    eventDescription,
                    startTime,
                    location,
                    registrationStartTime,
                    registrationEndTime,
                    organizerUser.getHardwareID(),
                    posterUrl,
                    requiresGeolocation);

            if (waitlistLimit != null) {
                newEvent.setWaitlistLimit(waitlistLimit);
            }

            newEvent.setLotteryCriteria(lotteryCriteria);

            organizerUser.addCreatedEvent(eventID);

            db.setUserData(organizerUser.getHardwareID(), organizerUser);

            db.setEventData(eventID, newEvent).addOnCompleteListener(saveTask -> {
                if (saveTask.isSuccessful()) {
                    Log.d(TAG, "Event created successfully with ID: " + eventID);
                    Toast.makeText(getContext(), "Event created successfully!", Toast.LENGTH_SHORT).show();
                    loadEvents();
                } else {
                    Log.e(TAG, "Error saving event to database", saveTask.getException());
                    Toast.makeText(getContext(),
                            "Error creating event: " +
                                    (saveTask.getException() != null
                                            ? saveTask.getException().getMessage()
                                            : "Unknown error"),
                            Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /**
     * Shows a dialog for editing an existing event.
     * The dialog is pre-populated with the event's current values and allows the
     * organizer
     * to modify basic details, registration window dates, waitlist limit, lottery
     * criteria,
     * and geolocation requirement. After validating inputs, this method calls
     * updateEvent to persist the changes.
     *
     * @param event the event to be edited
     */
    private void showEditEventDialog(Event event) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.popup_organizer_new_or_edit_event, null);
        builder.setView(dialogView);

        EditText editName = dialogView.findViewById(R.id.edit_name);
        Button btnEventDate = dialogView.findViewById(R.id.btn_event_date);
        EditText editLocation = dialogView.findViewById(R.id.edit_location);
        Button btnRegStart = dialogView.findViewById(R.id.btn_reg_start);
        Button btnRegEnd = dialogView.findViewById(R.id.btn_reg_end);
        EditText editLotteryCriteria = dialogView.findViewById(R.id.edit_lottery_criteria);
        EditText editDescription = dialogView.findViewById(R.id.edit_description);
        EditText posterUrlInput = dialogView.findViewById(R.id.editPosterUrl);
        Switch switchLimit = dialogView.findViewById(R.id.switchLimit);
        EditText limitNum = dialogView.findViewById(R.id.limit_num);
        Switch requireGeolocation = dialogView.findViewById(R.id.require_geolocation);
        Button saveButton = dialogView.findViewById(R.id.save);

        TextView label = dialogView.findViewById(R.id.label1);
        label.setText("Edit Event");

        final Date[] eventDate = { event.getStartTime() };
        final Date[] regStartDate = { event.getRegistrationStartTime() };
        final Date[] regEndDate = { event.getRegistrationEndTime() };
        SimpleDateFormat displayFormat = new SimpleDateFormat("MMM d, yyyy");

        editName.setText(event.getEventName());
        editLocation.setText(event.getLocation());
        editDescription.setText(event.getDescription());

        // fill lottery criteria and poster URL
        String existingCriteria = event.getLotteryCriteria();
        if (existingCriteria != null) {
            editLotteryCriteria.setText(existingCriteria);
        }

        String existingPosterUrl = event.getPosterURL();
        if (existingPosterUrl != null) {
            posterUrlInput.setText(existingPosterUrl);
        }

        if (event.getStartTime() != null) {
            btnEventDate.setText(displayFormat.format(event.getStartTime()));
            btnEventDate.setTextColor(getResources().getColor(android.R.color.primary_text_light, null));
        }
        if (event.getRegistrationStartTime() != null) {
            btnRegStart.setText(displayFormat.format(event.getRegistrationStartTime()));
            btnRegStart.setTextColor(getResources().getColor(android.R.color.primary_text_light, null));
        }
        if (event.getRegistrationEndTime() != null) {
            btnRegEnd.setText(displayFormat.format(event.getRegistrationEndTime()));
            btnRegEnd.setTextColor(getResources().getColor(android.R.color.primary_text_light, null));
        }

        btnEventDate.setOnClickListener(v -> showDatePickerDialog(date -> {
            eventDate[0] = date;
            btnEventDate.setText(displayFormat.format(date));
            btnEventDate.setTextColor(getResources().getColor(android.R.color.primary_text_light, null));
        }));

        btnRegStart.setOnClickListener(v -> showDatePickerDialog(date -> {
            regStartDate[0] = date;
            btnRegStart.setText(displayFormat.format(date));
            btnRegStart.setTextColor(getResources().getColor(android.R.color.primary_text_light, null));
        }));

        btnRegEnd.setOnClickListener(v -> showDatePickerDialog(date -> {
            regEndDate[0] = date;
            btnRegEnd.setText(displayFormat.format(date));
            btnRegEnd.setTextColor(getResources().getColor(android.R.color.primary_text_light, null));
        }));

        Integer currentLimit = event.getWaitlistLimit();
        if (currentLimit != null) {
            switchLimit.setChecked(true);
            limitNum.setText(String.valueOf(currentLimit));
            limitNum.setVisibility(View.VISIBLE);
        } else {
            switchLimit.setChecked(false);
            limitNum.setVisibility(View.GONE);
        }

        requireGeolocation.setChecked(event.getRequiresGeolocation());

        switchLimit.setOnCheckedChangeListener((buttonView, isChecked) -> {
            limitNum.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                limitNum.setText("");
            }
        });

        AlertDialog dialog = builder.create();

        saveButton.setOnClickListener(v -> {
            String eventName = editName.getText().toString().trim();
            String location = editLocation.getText().toString().trim();
            String lotteryCriteria = editLotteryCriteria.getText().toString().trim();
            String eventDescription = editDescription.getText().toString().trim();
            String posterUrl = posterUrlInput.getText().toString().trim();
            boolean hasLimit = switchLimit.isChecked();
            String limitStr = limitNum.getText().toString().trim();
            boolean requiresGeo = requireGeolocation.isChecked();

            if (TextUtils.isEmpty(eventName)) {
                Toast.makeText(getContext(), "Event name is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (eventDate[0] == null) {
                Toast.makeText(getContext(), "Please select an event date", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(location)) {
                Toast.makeText(getContext(), "Event location is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (regStartDate[0] == null) {
                Toast.makeText(getContext(), "Please select registration start date", Toast.LENGTH_SHORT).show();
                return;
            }
            if (regEndDate[0] == null) {
                Toast.makeText(getContext(), "Please select registration end date", Toast.LENGTH_SHORT).show();
                return;
            }
            if (hasLimit && TextUtils.isEmpty(limitStr)) {
                Toast.makeText(getContext(), "Please enter a waitlist limit number", Toast.LENGTH_SHORT).show();
                return;
            }

            Date startTime = eventDate[0];
            Date registrationStartTime = regStartDate[0];
            Date registrationEndTime = regEndDate[0];

            if (registrationStartTime != null && registrationEndTime != null
                    && registrationStartTime.after(registrationEndTime)) {
                Toast.makeText(getContext(), "Registration start time must be before end time", Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            if (startTime != null && registrationEndTime != null
                    && (startTime.before(registrationEndTime) || startTime.equals(registrationEndTime))) {
                Toast.makeText(getContext(), "Event start time should be after registration end time",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Integer waitlistLimit = null;
            if (hasLimit && !TextUtils.isEmpty(limitStr)) {
                try {
                    int limit = Integer.parseInt(limitStr);
                    if (limit <= 0) {
                        Toast.makeText(getContext(), "Waitlist limit must be greater than 0", Toast.LENGTH_SHORT)
                                .show();
                        return;
                    }
                    int currentWaitlistSize = event.getWaitListEntrants() != null
                            ? event.getWaitListEntrants().size()
                            : 0;
                    if (limit < currentWaitlistSize) {
                        Toast.makeText(getContext(),
                                "Waitlist limit cannot be less than current waitlist size (" + currentWaitlistSize
                                        + ")",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    waitlistLimit = limit;
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Invalid waitlist limit number", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            updateEvent(
                    event.getUniqueEventID(),
                    eventName,
                    eventDescription,
                    startTime,
                    location,
                    registrationStartTime,
                    registrationEndTime,
                    waitlistLimit,
                    lotteryCriteria,
                    posterUrl,
                    requiresGeo);

            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Updates an existing event with the provided details.
     * A new Event instance is constructed with the updated fields and written to
     * the database under the same event ID. On success, the organizer's event list
     * is
     * reloaded to reflect the changes. on failure, an error is logged and displayed
     * to
     * the user via a toast.
     *
     * @param eventId               the unique identifier of the event being updated
     * @param eventName             the updated event name
     * @param eventDescription      the updated event description
     * @param startTime             the updated event start time
     * @param location              the updated event location
     * @param registrationStartTime the updated registration start time
     * @param registrationEndTime   the updated registration end time
     * @param waitlistLimit         the updated waitlist limit, or null for no limit
     * @param lotteryCriteria       the updated lottery criteria text
     * @param posterUrl             the updated poster URL
     * @param requiresGeolocation   whether geolocation is required for registration
     */
    private void updateEvent(Long eventId,
            String eventName,
            String eventDescription,
            Date startTime,
            String location,
            Date registrationStartTime,
            Date registrationEndTime,
            Integer waitlistLimit,
            String lotteryCriteria,
            String posterUrl,
            boolean requiresGeolocation) {
        Log.d(TAG, "Updating event: " + eventName);

        Event updatedEvent = new Event(
                eventId,
                eventName,
                eventDescription,
                startTime,
                location,
                registrationStartTime,
                registrationEndTime,
                organizerUser.getHardwareID(),
                posterUrl,
                requiresGeolocation);

        updatedEvent.setWaitlistLimit(waitlistLimit);
        updatedEvent.setLotteryCriteria(lotteryCriteria);

        db.setEventData(eventId, updatedEvent).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Event updated successfully with ID: " + eventId);
                Toast.makeText(getContext(), "Event updated successfully!", Toast.LENGTH_SHORT).show();
                loadEvents();
            } else {
                Log.e(TAG, "Error updating event in database", task.getException());
                Toast.makeText(
                        getContext(),
                        "Error updating event: " +
                                (task.getException() != null
                                        ? task.getException().getMessage()
                                        : "Unknown error"),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Saves a QR code bitmap to the system Downloads directory.
     * The file name is derived from the event name and a timestamp, and the image
     * is stored
     * as a PNG using the MediaStore Downloads collection. A toast message indicates
     * whether
     * the operation succeeded or failed.
     *
     * @param bitmap    the QR code bitmap to save
     * @param eventName the event name used to generate the output file name
     */
    private void saveQRCodeToDownloads(Bitmap bitmap, String eventName) {
        try {
            String fileName = "QR_" + eventName.replaceAll("[^a-zA-Z0-9]", "_") + "_" + System.currentTimeMillis()
                    + ".png";

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
     * Displays a dialog containing the QR code for a specific event.
     * The dialog shows the event name, event ID, a generated QR code bitmap, and
     * buttons
     * to download or share the QR code. If QR generation fails then an error toast
     * is shown
     * and the dialog is not displayed.
     *
     * @param event the event for which a QR code should be generated and displayed
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
     * Shares a QR code bitmap for an event using an intent.
     * The bitmap is first written to the system Pictures directory using
     * MediaStore, and
     * then a chooser is launched so the organizer can select an app to share the QR
     * code
     * image along with a short descriptive message.
     *
     * @param bitmap    the QR code bitmap to share
     * @param eventName the event name included in the shared message and file name
     */
    private void shareQRCode(Bitmap bitmap, String eventName) {
        try {
            String fileName = "QR_" + eventName.replaceAll("[^a-zA-Z0-9]", "_") + ".png";

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

            Uri uri = requireContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values);
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

    private interface DateSelectedCallback {
        void onDateSelected(Date date);
    }

    private void showDatePickerDialog(DateSelectedCallback callback) {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(selectedYear, selectedMonth, selectedDay, 0, 0, 0);
                    selected.set(Calendar.MILLISECOND, 0);
                    callback.onDateSelected(selected.getTime());
                }, year, month, day);
        dialog.show();
    }
}