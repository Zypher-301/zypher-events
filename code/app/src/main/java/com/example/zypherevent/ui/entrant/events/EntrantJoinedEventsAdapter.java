package com.example.zypherevent.ui.entrant.events;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.zypherevent.Event;
import com.example.zypherevent.R;
import com.example.zypherevent.userTypes.Entrant;
import com.google.android.material.chip.Chip;

import java.util.List;

/**
 * Adapter for displaying joined events in a RecyclerView.
 * Shows event details and the user's status for each event.
 * (formatted from AdminEventsAdapter)
 *
 * @author Tom Yang
 * @version 1.0
 * @see Event
 * @see Entrant
 */
public class EntrantJoinedEventsAdapter extends RecyclerView.Adapter<EntrantJoinedEventsAdapter.EventViewHolder> {
    private List<Event> eventList;
    private Entrant currentUser;

    /**
     * Constructs an EntrantJoinedEventsAdapter.
     *
     * @param eventList The list of {@link Event} objects to be displayed.
     * @param currentUser The current {@link Entrant} user.
     */
    public EntrantJoinedEventsAdapter(List<Event> eventList, Entrant currentUser) {
        this.eventList = eventList;
        this.currentUser = currentUser;
    }

    /**
     * Called when RecyclerView needs a new {@link EventViewHolder} of the given type to represent
     * an item.
     *
     * @param parent The ViewGroup into which the new View will be added after it is bound to
     *               an adapter position.
     * @param viewType The view type of the new View.
     * @return A new EventViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_joined_preview, parent, false);
        return new EventViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * This method updates the contents of the {@link EventViewHolder#itemView} to reflect the
     * event item at the given position in the data set.
     *
     * @param holder The ViewHolder which should be updated to represent the contents of the
     *               item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);

        // Bind data from the Event object
        holder.tvTitle.setText(event.getEventName());

        // Format event metadata (date, time, location)
        String meta = formatEventMeta(event);
        holder.tvMeta.setText(meta);

        // Determine and display event status
        String status = determineEventStatus(event, currentUser);
        holder.chipStatus.setText(status);

        // Load event poster image if available
        // TODO: add event poster if available
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of events in the eventList.
     */
    @Override
    public int getItemCount() {
        return eventList.size();
    }

    /**
     * Formats event metadata as a readable string.
     *
     * @param event the event to format
     * @return formatted string with time and location
     */
    private String formatEventMeta(Event event) {
        StringBuilder meta = new StringBuilder();

        // Add start time if available
        String startTime = event.getStartTime();
        if (startTime != null && !startTime.isEmpty()) {
            meta.append(startTime);
        }

        // Add location
        String location = event.getLocation();
        if (location != null && !location.isEmpty()) {
            if (meta.length() > 0) {
                meta.append(" · ");
            }
            meta.append(location);
        }

        return meta.toString();
    }

    /**
     * Determines the user's status for this event.
     *
     * @param event the event to check
     * @param user the entrant user
     * @return status string (e.g., "Accepted", "Waitlisted", "Declined")
     */
    private String determineEventStatus(Event event, Entrant user) {
        if (event.getAcceptedEntrants() != null && event.getAcceptedEntrants().contains(user)) {
            return "Accepted";
        } else if (event.getDeclinedEntrants() != null && event.getDeclinedEntrants().contains(user)) {
            return "Declined";
        } else if (event.getWaitListEntrants() != null && event.getWaitListEntrants().contains(user)) {
            return "Waitlisted";
        }
        return "Joined";
    }

    /**
     * ViewHolder class for displaying individual event items in a RecyclerView.
     * Holds and initializes the UI components for an event card.
     */
    public static class EventViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPoster;
        TextView tvTitle;
        TextView tvMeta;
        Chip chipStatus;
        Button btnLeave;

        /**
         * Constructs an EventViewHolder.
         *
         * @param itemView The view for the event card.
         */
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPoster = itemView.findViewById(R.id.imgPoster);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMeta = itemView.findViewById(R.id.tvMeta);

            // Views from include_actions_joined
            View actionsJoined = itemView.findViewById(R.id.actionsJoined);
            chipStatus = actionsJoined.findViewById(R.id.chipAccepted);
            btnLeave = actionsJoined.findViewById(R.id.btnLeave);
        }
    }
}