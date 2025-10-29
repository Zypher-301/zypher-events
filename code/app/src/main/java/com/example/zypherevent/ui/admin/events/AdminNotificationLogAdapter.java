package com.example.zypherevent.ui.admin.events;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zypherevent.R;
import com.example.zypherevent.model.AdminNotificationLog;
import java.util.List;
/**
 * @author Arunavo Dutta
 * @version 1.0
 * @see AdminNotificationLog
 * @see res/layout/fragment_admin_item_notification_log.xml
 */

public class AdminNotificationLogAdapter extends RecyclerView.Adapter<AdminNotificationLogAdapter.LogViewHolder> {

    private List<AdminNotificationLog> logList;

    public AdminNotificationLogAdapter(List<AdminNotificationLog> logList) {
        this.logList = logList;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_admin_item_notification_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        AdminNotificationLog log = logList.get(position);
        holder.message.setText(log.getMessage());
        holder.event.setText("Event: " + log.getEvent());
        holder.group.setText("Group: " + log.getGroup());
        holder.timestamp.setText(log.getTimestamp());
        holder.sender.setText(log.getSender());
    }

    @Override
    public int getItemCount() {
        return logList.size();
    }

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