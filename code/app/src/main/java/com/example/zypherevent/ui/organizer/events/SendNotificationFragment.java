package com.example.zypherevent.ui.organizer.events;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.zypherevent.Database;
import com.example.zypherevent.Event;
import com.example.zypherevent.Notification;
import com.example.zypherevent.R;
import com.example.zypherevent.userTypes.Entrant;
import com.example.zypherevent.userTypes.Organizer;

import java.util.ArrayList;

/**
 * @author Tom Yang
 * @version 1.0
 *
 * Activity for sending notifications to entrants based on their event status.
 * Allows organizers to select a status category (Waitlisted, Accepted, or Denied)
 * and send appropriate notifications to all entrants in that category.
 * @see Notification
 * @see Database
 * @see Organizer
 * @see Entrant
 * @see Event
 */
public class SendNotificationFragment extends AppCompatActivity {
    /** Spinner for selecting the target status group (Waitlisted, Accepted, or Denied) */
    private Spinner statusSpinner;

    /** Button to trigger sending notifications to the selected status group */
    private Button sendButton;

    /** Firestore database instance for querying entrants and storing notifications */
    private Database database;

    /** The currently selected status from the spinner */
    private String selectedStatus;

    /** The organizer who is sending the notification */
    private Organizer organizer;

    /** The event ID for which notifications are being sent to */
    private Long eventID;

    /** The event name stored for notification */
    private String eventName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.popup_organizer_notifications_to_entrant);

        // Initialize Database
        database = new Database();

        // Get organizer and event ID from intent
        organizer = (Organizer) getIntent().getSerializableExtra("organizer");
        eventID = getIntent().getLongExtra("eventID", -1L);

        if (organizer == null || eventID == -1L) {
            Toast.makeText(this, "Error: missing organizer or event information", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        //Initialize ui
        statusSpinner = findViewById(R.id.dropdown);
        sendButton = findViewById(R.id.send_button);

        // Setup spinner with status options
        setupSpinner();

        // Setup send button click listener
        sendButton.setOnClickListener(v -> sendNotification());
    }

    /**
     * Configures the status spinner with available options (Waitlisted, Accepted, Denied)
     * Sets up the adapter and selection listener to track the currently selected status
     */
    private void setupSpinner() {
        // Create array of status options
        String[] statusOptions = {"Waitlisted", "Accepted", "Denied"};

        // Create adapter for spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statusOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Set adapter to spinner
        statusSpinner.setAdapter(adapter);

        // Set prompt before selection
        statusSpinner.setPrompt("Select...");

        // Set the listener for spinner selection
        statusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            /**
             * Called when an item in the spinner in selected.
             * Updates the selected status based on the position in the array.
             *
             * @param parent The AdapterView where the selection happened
             * @param view The view within the AdapterView that was clicked
             * @param position The position of the view in the adapter
             * @param id The row id of the item that is selected
             */
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedStatus = statusOptions[position];
            }

            /**
             * Called when nothing has been selected int he spinner.
             * Clears the selected status
             *
             * @param parent The AdapterView that now contains no selected item.
             */
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedStatus = null;
            }
        });
    }

    /**
     * Queries FireBase for all entrants with the selected status for the current event and sends a notification to each one.
     * Validates that a status has been selected and shows an AlertDialog if not.
     * The notification content is automatically generated based on status.
     * Uses the Database class to generate unique notification IDs and store notification (for future user story)
     * Provides user feedback through toast messages for success or failure.
     * Disables the send button during processing to prevent duplicate sends.
     */
    private void sendNotification() {
        // Validate that a status has been selected
        if (selectedStatus == null || selectedStatus.isEmpty()) {
            showSelectionRequireDialog();
            return;
        }

        // Disable button to prevent multiple clicks
        sendButton.setEnabled(false);

        // Query the event to get entrants with selected status
        database.getEvent(eventID).addOnSuccessListener(event -> {
            if (event == null) {
                Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                sendButton.setEnabled(true);
                return;
            }

            eventName = event.getEventName();
            // Get notification content based on status and event name
            String header = getNotificationHeader(selectedStatus, eventName);
            String body = getNotificationBody(selectedStatus);

            ArrayList<Entrant> targetEntrants = getEntrantListByStatus(event, selectedStatus);

            if (targetEntrants == null || targetEntrants.isEmpty()) {
                showNoEntrantDialog(selectedStatus);
                sendButton.setEnabled(true);
                return;
            }

            //Send notification to all entrants to the target list
            sendNotificationToEntrants(targetEntrants, header, body);
        })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to retrieve event " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    sendButton.setEnabled(true);
        });
    }

    /**
     * Displays an AlertDialog informing the organizer they must select a group from the dropdown
     * before sending notification.
     */
    private void showSelectionRequireDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("No Group Selected")
                .setMessage("Please select a group from the dropdown menu before sending notifications")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Displays an AlertDialog informing the organizer that there are no entrants in the selected group
     *
     * @param selectedStatus The status group that has no entrants (waitlisted, accepted, denied)
     */
    private void showNoEntrantDialog(String selectedStatus) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("No Entrant Found")
                .setMessage("There are currently no " + selectedStatus + " entrants for this event")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Retrieves the appropriate list of entrant Hardware Ids based on the selected status
     *
     * @param event The event containing the entrant lists
     * @param selectedStatus The selected status (waitlisted, accepted, denied)
     * @return The list of entrant Hardware IDs matching the selectedStatus, or null is status is invalid
     */
    private ArrayList<Entrant> getEntrantListByStatus(Event event, String selectedStatus) {
        switch (selectedStatus) {
            case "Waitlisted":
                return event.getWaitListEntrants();
            case "Accepted":
                return event.getAcceptedEntrants();
            case "Denied":
                return event.getDeclinedEntrants();
            default:
                return null;
        }
    }

    /**
     * Sends notifications to a list of entrants.
     * For each entrant, generate a unique notification ID using the Database class and stores notification in Firebase
     *
     * @param entrants The list of entrant objects to notify
     * @param header The notification header text
     * @param body The notification body text
     */
    private void sendNotificationToEntrants(ArrayList<Entrant> entrants, String header, String body) {
        int totalEntrants = entrants.size();
        final int[] successCount = {0};
        final int[] failureCount = {0};

        for (Entrant entrant : entrants) {
            // Get hardware ID from the Entrant object
            String entrantHardwareID = entrant.getHardwareID();

            // Get unique notification ID from the database
            database.getUniqueNotificationID()
                    .addOnSuccessListener(notificationID -> {
                        // Create notification
                        Notification notification = new Notification(notificationID, organizer.getHardwareID(),
                                entrantHardwareID, header, body);

                        // Store notification to database
                        database.setNotificationData(notificationID, notification)
                                .addOnSuccessListener(v -> {
                                    successCount[0]++;
                                    checkCompletionAndNotify(successCount[0], failureCount[0], totalEntrants);
                                })
                                .addOnFailureListener(e -> {
                                    failureCount[0]++;
                                    checkCompletionAndNotify(successCount[0], failureCount[0], totalEntrants);
                                });
                    })
                    .addOnFailureListener(e -> {
                        failureCount[0]++;
                        checkCompletionAndNotify(successCount[0], failureCount[0], totalEntrants);
                    });
        }
    }

    /**
     * Checks if all notification have been processed and displays the appropriate message.
     * Re-enables the send button once all operations are finished.
     *
     * @param successCount The number of notifications sent successfully
     * @param failureCount The number of notifications that failed to send
     * @param totalEntrants The total number of notification-sends attempted
     */
    private void checkCompletionAndNotify(int successCount, int failureCount, int totalEntrants) {
        if (successCount + failureCount == totalEntrants) {
            String message;
            if (failureCount == 0) {
                message = "Successfully sent " + successCount + " notification(s) to " + selectedStatus + " entrants ";
            } else {
                message = "Send " + successCount + " notification(s), " + failureCount + " failed";
            }

            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            sendButton.setEnabled(true);
        }
    }

    /**
     * Generates a notification header based on the entrant's status
     *
     * @param selectedStatus The selected status (waitlisted, accepted, denied)
     * @param eventName The name of the event
     * @return A formatted header string appropriate for the given status
     */
    private String getNotificationHeader(String selectedStatus, String eventName) {
        switch (selectedStatus){
            case "Accepted":
                return "Accepted: " + eventName;
            case "Waitlisted":
                return "Waitlisted: " + eventName;
            case "Denied":
                return "Update: " + eventName;
            default:
                return eventName + " Notification";
        }
    }

    /**
     * Generates a notification body based on the entrant's status
     *
     * @param selectedStatus The selected status (waitlisted, accepted, denied)
     * @return A formatted body message appropriate for the given status
     */
    private String getNotificationBody(String selectedStatus) {
        switch (selectedStatus){
            case "Accepted":
                return "Great news! You have been accepted. Please check the event details for next steps.";
            case "Waitlisted":
                return "You are currently on the waitlist for this event. We will notify you for status updates.";
            case "Denied":
                return "Unfortunately, you were not selected at this time. We will notify you for status updates.";
            default:
                return "Your event status has been updated. Please check the event for more details.";
        }
    }
}
