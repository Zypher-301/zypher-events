package com.example.zypherevent.ui.admin.events;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zypherevent.R;
import com.example.zypherevent.model.AdminEvent;
import java.util.List;

/**
 * @author Arunavo Dutta
 * @version 1.0
 * @see AdminEvent
 * @see res/layout/fragment_admin_item_event_card.xml
 */

public class AdminEventsAdapter extends RecyclerView.Adapter<AdminEventsAdapter.EventViewHolder> {

    private List<AdminEvent> eventList;
    private OnDeleteListener deleteListener;

    public interface OnDeleteListener {
        void onDelete(AdminEvent event);
    }

    public AdminEventsAdapter(List<AdminEvent> eventList, OnDeleteListener deleteListener) {
        this.eventList = eventList;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_admin_item_event_card, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        AdminEvent event = eventList.get(position);
        holder.eventName.setText(event.getName());
        holder.eventTime.setText(event.getTime());
        holder.eventDetails.setText(event.getDetails());

        // We are ignoring lottery_details for this example
        holder.eventLotteryDetails.setVisibility(View.GONE);

        holder.deleteButton.setOnClickListener(v -> {
            deleteListener.onDelete(event);
        });
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView eventName, eventTime, eventLotteryDetails, eventDetails;
        Button deleteButton; // Note: This is a Button in your XML

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventName = itemView.findViewById(R.id.event_name);
            eventTime = itemView.findViewById(R.id.event_time);
            eventLotteryDetails = itemView.findViewById(R.id.event_lottery_details);
            eventDetails = itemView.findViewById(R.id.event_details);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
}