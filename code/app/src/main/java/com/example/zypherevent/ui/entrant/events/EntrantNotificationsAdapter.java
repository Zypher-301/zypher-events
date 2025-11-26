package com.example.zypherevent.ui.entrant.events;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.zypherevent.Event;
import com.example.zypherevent.Notification;
import com.example.zypherevent.R;

import java.util.List;
import java.util.Map;

public class EntrantNotificationsAdapter
        extends RecyclerView.Adapter<EntrantNotificationsAdapter.NotificationViewHolder> {

    private List<Notification> notificationList;
    private Map<Long, Event> eventMap;
    private String currentUserId;
    private OnNotificationActionListener actionListener;

    /**
     * Listener for handling actions on notifications, specifically Accept and
     * Decline.
     */
    public interface OnNotificationActionListener {
        void onAccept(Notification notification);

        void onDecline(Notification notification);
    }

    /**
     * Sets the listener for notification actions.
     * 
     * @param listener The listener to handle actions
     */
    public void setOnNotificationActionListener(OnNotificationActionListener listener) {
        this.actionListener = listener;
    }

    public EntrantNotificationsAdapter(List<Notification> notificationList) {
        this.notificationList = notificationList;
    }

    public void updateData(List<Notification> newNotifications) {
        this.notificationList = newNotifications;
        notifyDataSetChanged();
    }

    /**
     * Updates the list of notifications and provides context for handling
     * invitations.
     *
     * @param newNotifications List of new notifications to display
     * @param eventMap         Map of Event objects corresponding to notifications
     *                         (for checking invitation status)
     * @param currentUserId    The hardware ID of the current user
     */
    public void updateData(List<Notification> newNotifications, Map<Long, Event> eventMap, String currentUserId) {
        this.notificationList = newNotifications;
        this.eventMap = eventMap;
        this.currentUserId = currentUserId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notificationList.get(position);

        holder.header.setText(notification.getNotificationHeader());
        holder.body.setText(notification.getNotificationBody());

        // Check if this is an invitation and if we should show buttons
        boolean showButtons = false;
        if (eventMap != null && currentUserId != null && notification.getEventID() != null) {
            Event event = eventMap.get(notification.getEventID());
            if (event != null && event.getInvitedEntrants() != null &&
                    event.getInvitedEntrants().contains(currentUserId)) {
                showButtons = true;
            }
        }

        if (showButtons) {
            holder.acceptButton.setVisibility(View.VISIBLE);
            holder.declineButton.setVisibility(View.VISIBLE);
            holder.acceptButton.setOnClickListener(v -> {
                if (actionListener != null)
                    actionListener.onAccept(notification);
            });
            holder.declineButton.setOnClickListener(v -> {
                if (actionListener != null)
                    actionListener.onDecline(notification);
            });
        } else {
            holder.acceptButton.setVisibility(View.GONE);
            holder.declineButton.setVisibility(View.GONE);
            holder.acceptButton.setOnClickListener(null);
            holder.declineButton.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return notificationList == null ? 0 : notificationList.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView header, body;
        Button acceptButton, declineButton;
        CardView cardView;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            header = itemView.findViewById(R.id.notification_header);
            body = itemView.findViewById(R.id.notification_body);
            acceptButton = itemView.findViewById(R.id.accept_button);
            declineButton = itemView.findViewById(R.id.decline_button);
            cardView = (CardView) itemView;
        }
    }
}