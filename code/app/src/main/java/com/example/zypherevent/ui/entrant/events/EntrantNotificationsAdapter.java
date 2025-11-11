package com.example.zypherevent.ui.entrant.events;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.zypherevent.Notification;
import com.example.zypherevent.R;

import java.util.List;

public class EntrantNotificationsAdapter extends RecyclerView.Adapter<EntrantNotificationsAdapter.NotificationViewHolder> {

    private List<Notification> notificationList;

    public EntrantNotificationsAdapter(List<Notification> notificationList) {
        this.notificationList = notificationList;
    }

    public void updateData(List<Notification> newNotifications) {
        this.notificationList = newNotifications;
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
    }

    @Override
    public int getItemCount() {
        return notificationList == null ? 0 : notificationList.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView header, body;
        CardView cardView;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            header = itemView.findViewById(R.id.notification_header);
            body = itemView.findViewById(R.id.notification_body);
            cardView = (CardView) itemView;
        }
    }
}