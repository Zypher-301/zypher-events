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
import com.example.zypherevent.userTypes.User;

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
     * Fetches all notifications from the database and filters them for the current user.
     * Only shows notifications where receivingUserHardwareID matches the current user.
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

                    // Filter notifications for current user
                    for (Notification notification : allNotifications) {
                        if (notification.getReceivingUserHardwareID() != null &&
                                notification.getReceivingUserHardwareID().equals(currentUserHardwareID)) {
                            notifications.add(notification);
                        }
                    }

                    // Sort by notification ID (most recent first, assuming higher ID = newer)
                    notifications.sort((n1, n2) ->
                            Long.compare(n2.getUniqueNotificationID(), n1.getUniqueNotificationID()));

                    adapter.updateData(notifications);

                    // Re-enable refresh button
                    refreshButton.setEnabled(true);
                    refreshButton.setText("Refresh");

                    if (notifications.isEmpty()) {
                        Toast.makeText(getContext(), "No notifications", Toast.LENGTH_SHORT).show();
                    }
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

    @Override
    public void onResume() {
        super.onResume();
        // Reload notifications when fragment becomes visible
        loadNotifications();
    }
}