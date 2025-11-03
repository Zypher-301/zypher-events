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
import com.example.zypherevent.userTypes.Entrant;
import java.util.List;

/**
 * @author Elliot Chrystal
 * @author Arunavo Dutta
 * @version 3.0
 * Adapter for displaying a list of events for an Entrant.
 * Binds Event data to the item_event.xml layout.
 * Implements logic for US 01.01.01 (Join), US 01.01.02 (Leave), US 01.05.04 (Waitlist).
 */
public class EntrantEventAdapter extends RecyclerView.Adapter<EntrantEventAdapter.EventViewHolder> {

    private List<Event> eventList;
    private Entrant currentUser;
    private OnItemClickListener listener;

    /**
     * Interface definition for a callback to be invoked when an item in this
     * adapter has been clicked. The hosting Fragment or Activity must implement this
     * interface to handle click events.
     */
    public interface OnItemClickListener {
        void onItemClick(Event event); // For clicking the card
        void onJoinClick(Event event); // For clicking "Join"
        void onLeaveClick(Event event); // For clicking "Leave"
    }

    /**
     * Constructs a new EntrantEventAdapter.
     *
     * @param eventList   The list of events to display.
     * @param currentUser The current entrant user, used to determine their waitlist status for events.
     * @param listener    The listener for click events on items and buttons within the adapter.
     */
    public EntrantEventAdapter(List<Event> eventList, Entrant currentUser, OnItemClickListener listener) {
        this.eventList = eventList;
        this.currentUser = currentUser;
        this.listener = listener;
    }

    /**
     * Called when RecyclerView needs a new {@link EventViewHolder} of the given type to represent
     * an item.
     * <p>
     * This new ViewHolder should be constructed with a new View that can represent the items
     * of the given type. You can either create a new View manually or inflate it from an XML
     * layout file.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return A new EventViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for each event row
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
        // Get the specific event for this row
        Event event = eventList.get(position);
        // Bind the event data to the views in that row
        holder.bind(event, currentUser, listener);
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
     * ViewHolder class for an event item in the RecyclerView.
     * This class holds references to the UI components (views) within the item_event.xml layout
     * for a single event, and binds the event data to these views. It also handles the
     * visibility of "Join" and "Leave" buttons based on the user's waitlist status.
     */
    public static class EventViewHolder extends RecyclerView.ViewHolder {

        // Declare the views from the layout
        ImageView imgPoster;
        TextView tvTitle, tvMeta, tvWaitlistCount;
        LinearLayout slotActions;
        Button btnJoinWaitlist, btnLeaveWaitlist;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            // Find all the views by their ID
            imgPoster = itemView.findViewById(R.id.imgPoster);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            tvWaitlistCount = itemView.findViewById(R.id.tvWaitlistCount);
            slotActions = itemView.findViewById(R.id.slotActions);
            btnJoinWaitlist = itemView.findViewById(R.id.btnJoinWaitlist);
            btnLeaveWaitlist = itemView.findViewById(R.id.btnLeaveWaitlist);
        }

        /**
         * Binds the data from a single {@link Event} object to the views in the ViewHolder.
         * This method sets the event's title and location. It also handles the visibility
         * and click listeners for the "Join Waitlist" and "Leave Waitlist" buttons based
         * on whether the current user is already on the event's waitlist.
         *
         * @param event       The {@link Event} object containing the data to display.
         * @param currentUser The current {@link Entrant} user, used to check waitlist status.
         * @param listener    The {@link OnItemClickListener} to handle user interactions like
         *                    joining, leaving, or clicking the event item.
         */
        public void bind(final Event event, Entrant currentUser, final OnItemClickListener listener) {
            tvTitle.setText(event.getEventName());
            tvMeta.setText(event.getLocation());

            // US 01.05.04: Show waitlist count
            int waitlistSize = event.getWaitListEntrants().size();
            tvWaitlistCount.setText("On waiting list: " + waitlistSize);

            // Make the button area visible
            slotActions.setVisibility(View.VISIBLE);

            // Check if the current user is on the waitlist.
            if (event.getWaitListEntrants().contains(currentUser)) {
                // User is ON the waitlist: Show "Leave"
                btnJoinWaitlist.setVisibility(View.GONE);
                btnLeaveWaitlist.setVisibility(View.VISIBLE);
            } else {
                // User is NOT on the waitlist: Show "Join"
                btnJoinWaitlist.setVisibility(View.VISIBLE);
                btnLeaveWaitlist.setVisibility(View.GONE);
            }

            // Set the click listeners to call the methods in the fragment
            btnJoinWaitlist.setOnClickListener(v -> listener.onJoinClick(event));
            btnLeaveWaitlist.setOnClickListener(v -> listener.onLeaveClick(event));
            itemView.setOnClickListener(v -> listener.onItemClick(event));
        }
    }
}