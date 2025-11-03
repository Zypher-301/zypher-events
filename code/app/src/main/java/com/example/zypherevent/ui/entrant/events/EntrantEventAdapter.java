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
 * Adapter for displaying a list of events for an Entrant.
 * Binds Event data to the item_event.xml layout.
 * Implements logic for US 01.01.01 (Join) and US 01.01.02 (Leave).
 */
public class EntrantEventAdapter extends RecyclerView.Adapter<EntrantEventAdapter.EventViewHolder> {

    private List<Event> eventList;
    private Entrant currentUser; // Current user
    private OnItemClickListener listener; // The fragment that will handle clicks

    /**
     * This interface defines the "contract" that the fragment must follow.
     * The fragment will provide the logic for these three methods.
     */
    public interface OnItemClickListener {
        void onItemClick(Event event); // For clicking the card
        void onJoinClick(Event event); // For clicking "Join"
        void onLeaveClick(Event event); // For clicking "Leave"
    }

    // Constructor that requires all 3 items
    public EntrantEventAdapter(List<Event> eventList, Entrant currentUser, OnItemClickListener listener) {
        this.eventList = eventList;
        this.currentUser = currentUser;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for each event row
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        // Get the specific event for this row
        Event event = eventList.get(position);
        // Bind the event data to the views in that row
        holder.bind(event, currentUser, listener);
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    /**
     * ViewHolder class that holds the references to the views
     * in the item_event.xml layout file.
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
         * Binds a single Event object to the views.
         */
        public void bind(final Event event, Entrant currentUser, final OnItemClickListener listener) {
            // Set basic event info
            tvTitle.setText(event.getEventName());
            tvMeta.setText(event.getLocation());

            // US 01.05.04: Show waitlist count
            int waitlistSize = event.getWaitListEntrants().size();
            tvWaitlistCount.setText("On waiting list: " + waitlistSize);

            // Make the button area visible
            slotActions.setVisibility(View.VISIBLE);

            // Check if the current user is on the waitlist.
            // This requires Entrant.java to have a proper .equals() method!
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