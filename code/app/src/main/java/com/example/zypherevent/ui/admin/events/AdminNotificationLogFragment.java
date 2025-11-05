package com.example.zypherevent.ui.admin.events;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.zypherevent.Database;
import com.example.zypherevent.Notification;
import com.example.zypherevent.R;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Arunavo Dutta
 * @version 2.0
 * @see AdminBaseListFragment
 * @see Notification
 * @see AdminNotificationLogAdapter
 * @see "res/navigation/admin_navigation.xml"
 */

public class AdminNotificationLogFragment extends AdminBaseListFragment {

    private static final String TAG = "AdminNotifLogFragment";

    private Database db;
    private AdminNotificationLogAdapter adapter;
    private List<Notification> notificationList = new ArrayList<>();
    private Button refreshButton;

    /**
     * Called immediately after {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, Bundle)}
     * has returned, but before any saved state has been restored in to the view.
     * This method initializes the database, sets up the RecyclerView adapter for displaying
     * notification logs, and configures a refresh button to reload the logs. It also
     * performs an initial load of the notification logs from the database.
     *
     * @param view The View returned by {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState); // Sets up recyclerView

        // Initialize Database
        db = new Database();

        // Initialize Adapter with the empty list
        adapter = new AdminNotificationLogAdapter(notificationList);
        recyclerView.setAdapter(adapter);

        // --- REFRESH BUTTON LOGIC ---
        refreshButton = view.findViewById(R.id.refresh_button);
        refreshButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Refreshing list...", Toast.LENGTH_SHORT).show();
            loadLogs();
        });
        // --- END REFRESH LOGIC ---

        // Load the logs for the first time
        loadLogs();
    }

    /**
     * Fetches all notification logs from the Firestore database.
     * <p>
     * This method initiates an asynchronous query to the 'notifications' collection using
     * the {@link Database#getAllNotifications()} method.
     * On successful completion, it clears the existing {@code notificationList}, populates it
     * with the fetched logs, and notifies the {@code adapter} to refresh the RecyclerView.
     * If the task fails or returns a null result, an error message is logged and displayed
     * as a Toast.
     */
    private void loadLogs() {
        Log.d(TAG, "Attempting to query 'notifications' collection...");

        db.getAllNotifications().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<Notification> fetchedLogs = task.getResult();

                if (fetchedLogs == null) {
                    Log.e(TAG, "Failed to get notification list (task result was null).");
                    Toast.makeText(getContext(), "Error: Failed to get list.", Toast.LENGTH_SHORT).show();
                    return;
                }

                Log.d(TAG, "Firebase query successful. Found " + fetchedLogs.size() + " logs.");

                // Clear the old list and add the new ones
                notificationList.clear();
                notificationList.addAll(fetchedLogs);

                // Tell the adapter to update the UI
                adapter.notifyDataSetChanged();

            } else {
                Log.e(TAG, "Error running query: ", task.getException());
                Toast.makeText(getContext(), "Error fetching logs", Toast.LENGTH_SHORT).show();
            }
        });
    }
}