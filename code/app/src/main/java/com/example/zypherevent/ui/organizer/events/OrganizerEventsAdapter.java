package com.example.zypherevent.ui.organizer.events;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.zypherevent.Event;
import com.example.zypherevent.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrganizerEventsAdapter extends RecyclerView.Adapter<OrganizerEventsAdapter.EventViewHolder> {

    private List<Event> eventList;
    private OnItemClickListener listener;

    /**
     * Interface definition for callbacks to be invoked when an item in this
     * adapter has been clicked.
     */
    public interface OnItemClickListener {
        // This is the only click we need for the text-only layout
        void onViewEntrantsClick(Event event);
    }

    /**
     * Constructs an OrganizerEventsAdapter.
     *
     * @param eventList The list of events to be displayed.
     * @param listener  A listener for handling clicks on events.
     */
    public OrganizerEventsAdapter(List<Event> eventList, OnItemClickListener listener) {
        this.eventList = eventList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_organizer_event_item, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.bind(event, listener);
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    /**
     * ViewHolder class for an event item in the RecyclerView.
     */
    public static class EventViewHolder extends RecyclerView.ViewHolder {

        // Declare the views from the new layout
        TextView tvTitle, tvMeta;
        Button btnViewEntrants;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            // Find all the views by their ID
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            btnViewEntrants = itemView.findViewById(R.id.btnViewEntrants);
        }

        /**
         * Binds data from an {@link Event} object to the views in the ViewHolder.
         *
         * @param event    The {@link Event} object containing the data to be displayed.
         * @param listener The {@link OnItemClickListener} that will handle clicks on the item view.
         */
        public void bind(final Event event, final OnItemClickListener listener) {
            tvTitle.setText(event.getEventName());

            // Format metadata: date, time, location
            StringBuilder meta = new StringBuilder();

            Date startTime = event.getStartTime();
            if (startTime != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
                SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
                meta.append(dateFormat.format(startTime));
                meta.append(" · ");
                meta.append(timeFormat.format(startTime));
            }

            String location = event.getLocation();
            if (location != null && !location.isEmpty()) {
                if (meta.length() > 0) {
                    meta.append(" · ");
                }
                meta.append(location);
            }

            tvMeta.setText(meta.toString());

            // Set the click listener for the button
            btnViewEntrants.setOnClickListener(v -> listener.onViewEntrantsClick(event));
        }
    }
}
