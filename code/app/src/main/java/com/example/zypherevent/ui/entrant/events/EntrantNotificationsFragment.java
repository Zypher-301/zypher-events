package com.example.zypherevent.ui.entrant.events;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.zypherevent.Database;
import com.example.zypherevent.EntrantActivity;
import com.example.zypherevent.Notification;
import com.example.zypherevent.R;
import com.example.zypherevent.userTypes.Entrant;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Elliot Chrystal
 * @author Tom Yang (Added adapter to notification recycler view)
 *
 * @version 2.0
 */
public class EntrantNotificationsFragment extends Fragment {
    private RecyclerView recyclerView;
    private EntrantNotificationsAdapter adapter;
    private List<Notification> notifications;
    private Button refreshButton;

    private Database db;
    private String currentUserHardwareID;

    public EntrantNotificationsFragment() {
        // public no-arg constructor required
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_entrant_notifications, container, false);

        // Initialize Database
        db = new Database();

        // Get current user's hardware ID from arguments
        Entrant currentUser = ((EntrantActivity) getActivity()).getEntrantUser();

        if (currentUser != null) {
            currentUserHardwareID = currentUser.getHardwareID();
            Log.d("EntrantNotificationsFragment", "Current user hardware ID: " + currentUserHardwareID);
        }

        // Initialize views
        recyclerView = view.findViewById(R.id.recycler_view);
        refreshButton = view.findViewById(R.id.refresh_button);

        // Set up RecyclerView
        notifications = new ArrayList<>();
        adapter = new EntrantNotificationsAdapter(notifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Load notifications
        loadNotifications();

        // Set up refresh button
        refreshButton.setOnClickListener(v -> loadNotifications());

        return view;
    }

    /**
     * Fetches all notifications from the database and filters them for the current
     * user.
     * Only shows notifications where receivingUserHardwareID matches the current
     * user.
     */
    private void loadNotifications() {
        if (currentUserHardwareID == null) {
            Toast.makeText(getContext(), "User ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable refresh button while loading
        refreshButton.setEnabled(false);

        db.getAllNotifications()
                .addOnSuccessListener(allNotifications -> {
                    notifications.clear();
                    List<Long> eventIDs = new ArrayList<>();

                    // Filter notifications for current user
                    for (Notification notification : allNotifications) {
                        if (notification.getReceivingUserHardwareID() != null &&
                                notification.getReceivingUserHardwareID().equals(currentUserHardwareID)) {
                            notifications.add(notification);
                            if (notification.getEventID() != null) {
                                eventIDs.add(notification.getEventID());
                            }
                        }
                    }

                    // Sort by notification ID (most recent first, assuming higher ID = newer)
                    notifications
                            .sort((n1, n2) -> Long.compare(n2.getUniqueNotificationID(), n1.getUniqueNotificationID()));

                    // Fetch events for the notifications
                    fetchEventsForNotifications(eventIDs);

                })
                .addOnFailureListener(e -> {
                    // Re-enable refresh button
                    refreshButton.setEnabled(true);
                    refreshButton.setText("Refresh");

                    Toast.makeText(getContext(),
                            "Error loading notifications: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Retrieves Event objects for a list of event IDs to display details in
     * notifications.
     * Used to determine if a notification is an invitation that needs
     * Accept/Decline buttons.
     *
     * @param eventIDs List of event IDs to fetch
     */
    private void fetchEventsForNotifications(List<Long> eventIDs) {
        if (eventIDs.isEmpty()) {
            adapter.updateData(notifications, new java.util.HashMap<>(), currentUserHardwareID);
            refreshButton.setEnabled(true);
            refreshButton.setText("Refresh");
            if (notifications.isEmpty()) {
                Toast.makeText(getContext(), "No notifications", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        java.util.Map<Long, com.example.zypherevent.Event> eventMap = new java.util.HashMap<>();
        java.util.concurrent.atomic.AtomicInteger pendingRequests = new java.util.concurrent.atomic.AtomicInteger(
                eventIDs.size());

        for (Long eventID : eventIDs) {
            db.getEvent(eventID).addOnSuccessListener(event -> {
                if (event != null) {
                    eventMap.put(eventID, event);
                }
                if (pendingRequests.decrementAndGet() == 0) {
                    adapter.updateData(notifications, eventMap, currentUserHardwareID);
                    refreshButton.setEnabled(true);
                    refreshButton.setText("Refresh");
                }
            }).addOnFailureListener(e -> {
                Log.e("EntrantNotifications", "Failed to load event: " + eventID, e);
                if (pendingRequests.decrementAndGet() == 0) {
                    adapter.updateData(notifications, eventMap, currentUserHardwareID);
                    refreshButton.setEnabled(true);
                    refreshButton.setText("Refresh");
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload notifications when fragment becomes visible
        loadNotifications();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter.setOnNotificationActionListener(new EntrantNotificationsAdapter.OnNotificationActionListener() {
            @Override
            public void onAccept(Notification notification) {
                handleAcceptInvitation(notification);
            }

            @Override
            public void onDecline(Notification notification) {
                handleDeclineInvitation(notification);
            }
        });
    }

    /**
     * Updates the event status to "Accepted" for the current user.
     * Moves the user from the invited list to the accepted list in Firestore.
     *
     * @param notification The notification associated with the event invitation
     */
    private void handleAcceptInvitation(Notification notification) {
        Long eventID = notification.getEventID();
        if (eventID == null)
            return;

        db.getEvent(eventID).addOnSuccessListener(event -> {
            if (event == null)
                return;

            // Move from invited to accepted
            event.removeEntrantFromInvitedList(currentUserHardwareID);
            event.addEntrantToAcceptedList(currentUserHardwareID);

            db.setEventData(eventID, event).addOnSuccessListener(v -> {
                Toast.makeText(getContext(), "Invitation Accepted!", Toast.LENGTH_SHORT).show();
                loadNotifications(); // Refresh UI
            }).addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Failed to accept invitation", Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Failed to load event", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Updates the event status to "Declined" for the current user.
     * Moves the user from the invited list to the declined list in Firestore.
     *
     * @param notification The notification associated with the event invitation
     */
    private void handleDeclineInvitation(Notification notification) {
        Long eventID = notification.getEventID();
        if (eventID == null)
            return;

        db.getEvent(eventID).addOnSuccessListener(event -> {
            if (event == null)
                return;

            // Move from invited to declined
            event.removeEntrantFromInvitedList(currentUserHardwareID);
            event.addEntrantToDeclinedList(currentUserHardwareID);

            db.setEventData(eventID, event).addOnSuccessListener(v -> {
                Toast.makeText(getContext(), "Invitation Declined", Toast.LENGTH_SHORT).show();
                loadNotifications(); // Refresh UI
            }).addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Failed to decline invitation", Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Failed to load event", Toast.LENGTH_SHORT).show();
        });
    }
}