package com.example.zypherevent.ui.admin.events;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zypherevent.Notification;
import com.example.zypherevent.R;
import java.util.List;

/**
 * Adapter class for displaying a list of notification logs in a RecyclerView.
 * This adapter is responsible for creating ViewHolders for each item, binding notification data
 * to the views within each ViewHolder, and managing the overall list. It is specifically
 * used in the admin section to view logs of sent notifications.
 *
 * @author Arunavo Dutta
 * @version 2.0
 * @see Notification
 * @see RecyclerView.Adapter
 * @see AdminNotificationLogAdapter.LogViewHolder
 * @see com.example.zypherevent.R.layout#fragment_admin_item_notification_log
 */
public class AdminNotificationLogAdapter extends RecyclerView.Adapter<AdminNotificationLogAdapter.LogViewHolder> {


    private List<Notification> logList;

    public AdminNotificationLogAdapter(List<Notification> logList) {
        this.logList = logList;
    }

    /**
     * Called when RecyclerView needs a new {@link LogViewHolder} of the given type to represent
     * an item.
     * <p>
     * This new ViewHolder should be constructed with a new View that can represent the items
     * of the given type. You can either create a new View manually or inflate it from an XML
     * layout file.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return A new LogViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_admin_item_notification_log, parent, false);
        return new LogViewHolder(view);
    }

    /**
     * Binds the data from the {@link Notification} object at a specific position in the list
     * to the views within the {@link LogViewHolder}.
     * <p>
     * This method is called by the RecyclerView to display the data at the specified position.
     * It retrieves the Notification object and populates the TextViews in the ViewHolder with
     * the notification's header, body, sender ID, and unique ID. The 'group' TextView is hidden
     * as it is not used.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        Notification log = logList.get(position);

        // Map notificationBody to notification_message
        holder.message.setText("\"" + log.getNotificationBody() + "\"");

        // Map sendingUserHardwareID to notification_sender
        holder.sender.setText("From: " + log.getSendingUserHardwareID());

        // Re-use notification_event to show the header
        holder.event.setText("Title: " + log.getNotificationHeader());

        // Re-use notification_timestamp to show the ID
        holder.timestamp.setText("ID: " + log.getUniqueNotificationID().toString());

        // Hide the 'group' field, as we don't have data for it
        holder.group.setVisibility(View.GONE);
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of notifications in the list.
     */
    @Override
    public int getItemCount() {
        return logList.size();
    }

    /**
     * ViewHolder class for displaying individual notification log items in the RecyclerView.
     * It holds and initializes the UI components for a single item, such as TextViews
     * for the message, event, group, timestamp, and sender.
     */
    public static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView message, event, group, timestamp, sender;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.notification_message);
            event = itemView.findViewById(R.id.notification_event);
            group = itemView.findViewById(R.id.notification_group);
            timestamp = itemView.findViewById(R.id.notification_timestamp);
            sender = itemView.findViewById(R.id.notification_sender);
        }
    }
}