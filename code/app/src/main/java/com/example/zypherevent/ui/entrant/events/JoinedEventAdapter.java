package com.example.zypherevent.ui.entrant.events;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.zypherevent.Event;
import com.example.zypherevent.R;
import java.util.List;

/**
 * @author Arunavo Dutta
 * @version 3.0
 * Adapter for displaying a list of joined events for an Entrant.
 * Binds Event data to the item_event.xml layout.
 * Implements logic for US 01.01.02 (Leave), US 01.05.04 (Waitlist).
 */
public class JoinedEventAdapter extends RecyclerView.Adapter<JoinedEventAdapter.EventViewHolder> {

    private List<Event> eventList;
    private OnLeaveClickListener listener;

    /**
     * A simple listener for only the "Leave" click.
     */
    public interface OnLeaveClickListener {
        void onLeaveClick(Event event);
    }

    /**
     * Constructs a new JoinedEventAdapter.
     *
     * @param eventList The list of events that the user has joined.
     * @param listener The listener for handling leave event actions.
     */
    public JoinedEventAdapter(List<Event> eventList, OnLeaveClickListener listener) {
        this.eventList = eventList;
        this.listener = listener;
    }

    /**
     * Called when RecyclerView needs a new {@link EventViewHolder} of the given type to represent
     * an item.
     * <p>
     * This new ViewHolder is constructed with a new View that is inflated from the
     * {@code R.layout.item_event} XML layout file.
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
                .inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * This method updates the contents of the {@link EventViewHolder#itemView} to reflect the
     * event at the given position in the list.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.bind(event, listener);
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of events in the list.
     */
    @Override
    public int getItemCount() {
        return eventList.size();
    }

    /**
     * ViewHolder class for an individual event item in the RecyclerView.
     * This class holds and manages the UI components for a single event entry,
     * such as the event title, metadata, and action buttons.
     */
    public static class EventViewHolder extends RecyclerView.ViewHolder {

        TextView tvTitle, tvMeta, tvWaitlistCount;
        Button btnJoinWaitlist, btnLeaveWaitlist;

        /**
         * Constructs an EventViewHolder.
         *
         * @param itemView The view that will be used to display an item in the RecyclerView.
         */
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            tvWaitlistCount = itemView.findViewById(R.id.tvWaitlistCount);
            btnJoinWaitlist = itemView.findViewById(R.id.btnJoinWaitlist);
            btnLeaveWaitlist = itemView.findViewById(R.id.btnLeaveWaitlist);
        }

        /**
         * Binds the data from an {@link Event} object to the views in the ViewHolder.
         * This method sets the event title and location. It also manages the visibility
         * of UI elements based on the context of the "Joined Events" screen.
         * Specifically, it shows the "Leave" button and hides the "Join" button.
         * It also displays the current number of entrants on the waitlist for the event.
         * An OnClickListener is set on the "Leave" button to handle leave actions.
         *
         * @param event    The {@link Event} object containing the data to display.
         * @param listener The {@link OnLeaveClickListener} to be invoked when the "Leave" button is clicked.
         */
        public void bind(Event event, OnLeaveClickListener listener) {
            tvTitle.setText(event.getEventName());
            tvMeta.setText(event.getLocation());

            // US 01.05.04: Show waitlist count
            int waitlistSize = event.getWaitListEntrants().size();
            tvWaitlistCount.setText("On waiting list: " + waitlistSize);

            // Since this is the "Joined Events" list,
            // we always show the "Leave" button.
            btnJoinWaitlist.setVisibility(View.GONE);
            btnLeaveWaitlist.setVisibility(View.VISIBLE);

            btnLeaveWaitlist.setOnClickListener(v -> listener.onLeaveClick(event));
        }
    }
}