package com.example.zypherevent.ui.admin.events; // Use your actual package name

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zypherevent.Event; // <-- IMPORTING REAL EVENT
import com.example.zypherevent.R;
import java.util.List;

/**
 * @author Arunavo Dutta
 * @version 2.0
 * @see Event
 * @see res/layout/fragment_admin_item_event_card.xml
 */
public class AdminEventsAdapter extends RecyclerView.Adapter<AdminEventsAdapter.EventViewHolder> {

    private List<Event> eventList; // <-- USING REAL EVENT
    private OnDeleteListener deleteListener;

    public interface OnDeleteListener {
        void onDelete(Event event); // <-- USING REAL EVENT
    }

    public AdminEventsAdapter(List<Event> eventList, OnDeleteListener deleteListener) {
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
        Event event = eventList.get(position); // <-- USING REAL EVENT

        // Bind data from the real Event object
        holder.eventName.setText(event.getEventName());
        holder.eventTime.setText(event.getStartTime());
        holder.eventDetails.setText(event.getEventDescription());
        holder.eventLotteryDetails.setText("Lottery ends: " + event.getRegistrationEndTime());

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
        Button deleteButton;

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