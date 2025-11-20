package com.example.zypherevent.ui.entrant.events;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.zypherevent.Event;
import com.example.zypherevent.R;
import com.example.zypherevent.WaitlistEntry;
import com.example.zypherevent.userTypes.Entrant;
import java.util.List;

/**
 * BUG: DOUBLE CLICKING NEEDED FOR LEAVING THE WAITLIST
 * An adapter to display a list of {@link Event} objects for an {@link Entrant} in a {@link RecyclerView}.
 * <p>
 * This adapter is responsible for creating views for each event, binding event data to those views,
 * and handling user interactions. It manages the UI for an entrant to join or leave an event's waitlist,
 * delegating these actions to the hosting component through the {@link OnItemClickListener}.
 * <p>
 * This implementation addresses the following user stories:
 * <ul>
 * <li>US 01.01.01: Allows an entrant to join an event's waitlist.</li>
 * <li>US 01.01.02: Allows an entrant to leave an event's waitlist.</li>
 * <li>US 01.05.04: Displays the current count of users on the waitlist for an event.</li>
 * </ul>
 *
 * @see Event
 * @see Entrant
 * @see EntrantEventAdapter.EventViewHolder
 * @author Arunavo Dutta
 * @version 3.0
 */
public class EntrantEventAdapter extends RecyclerView.Adapter<EntrantEventAdapter.EventViewHolder> {

    private List<Event> eventList;
    private Entrant currentUser;
    private OnItemClickListener listener;

    /**
     * Interface definition for a callback to be invoked when an item in this
     * adapter has been clicked.
     * <p>
     * The hosting Fragment or Activity must implement this interface to handle
     * click events on the event item itself, as well as actions like joining or
     * leaving an event's waitlist.
     */
    public interface OnItemClickListener {
        void onItemClick(Event event); // For clicking the card
        void onJoinClick(Event event); // For clicking "Join"
        void onLeaveClick(Event event); // For clicking "Leave"
    }

    /**
     * Constructs an EntrantEventAdapter.
     *
     * @param eventList   The list of events to be displayed.
     * @param currentUser The current entrant, used to determine their status (e.g., on a waitlist) for each event.
     * @param listener    A listener for handling clicks on events and associated actions like joining or leaving a waitlist.
     */
    public EntrantEventAdapter(List<Event> eventList, Entrant currentUser, OnItemClickListener listener) {
        this.eventList = eventList;
        this.currentUser = currentUser;
        this.listener = listener;
    }

    /**
     * Called when RecyclerView needs a new {@link EventViewHolder} to represent an event item.
     * <p>
     * This method inflates the a new view from the {@code R.layout.item_event} XML layout file,
     * which defines the UI for a single item in the list. It then creates and returns a new
     * {@code EventViewHolder} instance holding this new view. This process is managed by the
     * LayoutManager.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     * an adapter position. This is the RecyclerView itself.
     * @param viewType The view type of the new View. This is not used in this adapter as there
     * is only one type of item view.
     * @return A new {@code EventViewHolder} that holds the View for a single event item.
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
     * event at the given position in the list. It retrieves the specific {@link Event} object
     * for the given position and calls the {@link EventViewHolder#bind(Event, Entrant, OnItemClickListener)}
     * method to populate the views with the event's data.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     * item at the given position in the data set.
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
     * @return The total number of events in this adapter's list.
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
        TextView tvLotteryCriteriaLabel, tvLotteryCriteria;
        LinearLayout slotActions;
        Button btnJoinWaitlist, btnLeaveWaitlist;

        /**
         * Constructs an instance of the {@code EventViewHolder}.
         * This constructor finds and initializes the UI components from the item layout
         * ({@code item_event.xml}) that are used to display the event data.
         *
         * @param itemView The view for a single item in the RecyclerView, inflated from
         * the {@code item_event.xml} layout. This view contains all the
         * UI elements for one event.
         */
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            // Find all the views by their ID
            imgPoster = itemView.findViewById(R.id.imgPoster);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            tvWaitlistCount = itemView.findViewById(R.id.tvWaitlistCount);
            tvLotteryCriteriaLabel = itemView.findViewById(R.id.tvLotteryCriteriaLabel);
            tvLotteryCriteria = itemView.findViewById(R.id.tvLotteryCriteria);
            slotActions = itemView.findViewById(R.id.slotActions);
            btnJoinWaitlist = itemView.findViewById(R.id.btnJoinWaitlist);
            btnLeaveWaitlist = itemView.findViewById(R.id.btnLeaveWaitlist);
        }

        /**
         * Binds data from an {@link Event} object to the views in the ViewHolder.
         * <p>
         * This method populates the event's title, location, and waitlist count. It dynamically
         * controls the visibility of the "Join Waitlist" and "Leave Waitlist" buttons based on
         * whether the {@code currentUser} is already on the event's waitlist. It also sets up
         * click listeners for the entire item view and the action buttons, delegating the
         * handling of these events to the provided {@link OnItemClickListener}.
         *
         * @param event       The {@link Event} object containing the data to be displayed.
         * @param currentUser The current {@link Entrant} user, used to determine their waitlist status.
         * @param listener    The {@link OnItemClickListener} that will handle clicks on the item view,
         * join button, and leave button.
         */
        public void bind(final Event event, Entrant currentUser, final OnItemClickListener listener) {
            tvTitle.setText(event.getEventName());
            tvMeta.setText(event.getLocation());

            int waitlistSize = event.getWaitListEntrants().size();
            tvWaitlistCount.setText("On Waiting List: " + waitlistSize);

            // per-event lottery criteria
            String criteria = event.getLotteryCriteria();
            if (criteria == null || criteria.trim().isEmpty()) {
                tvLotteryCriteriaLabel.setVisibility(View.GONE);
                tvLotteryCriteria.setVisibility(View.GONE);
            } else {
                tvLotteryCriteriaLabel.setVisibility(View.VISIBLE);
                tvLotteryCriteria.setVisibility(View.VISIBLE);
                tvLotteryCriteria.setText(criteria.trim());
            }

            slotActions.setVisibility(View.VISIBLE);

            // Check registration window status
            boolean registrationOpen = event.isRegistrationOpen();
            String registrationStatus = event.getRegistrationStatus();

            // Check if the current user is on the waitlist.
            boolean isOnWaitlist = false;
            String myId = currentUser.getHardwareID();

            for (WaitlistEntry entry : event.getWaitListEntrants()) {
                String entryId = entry.getEntrantHardwareID();
                if (entryId != null && entryId.equals(myId)) {
                    isOnWaitlist = true;
                    break;
                }
            }

            if (isOnWaitlist) {
                // User is ON the waitlist: Show "Leave" (only enabled during registration window)
                btnJoinWaitlist.setVisibility(View.GONE);
                btnLeaveWaitlist.setVisibility(View.VISIBLE);
                btnLeaveWaitlist.setEnabled(registrationOpen);
            } else {
                // User is NOT on the waitlist
                btnLeaveWaitlist.setVisibility(View.GONE);
                btnJoinWaitlist.setVisibility(View.VISIBLE);

                if (registrationOpen) {
                    // Registration is open: Show enabled "Join" button
                    btnJoinWaitlist.setEnabled(true);
                    btnJoinWaitlist.setText("Join Waitlist");
                } else {
                    // Registration is closed/not started: Show disabled button with status
                    btnJoinWaitlist.setEnabled(false);
                    btnJoinWaitlist.setText(registrationStatus);
                }
            }

            // Set the click listeners to call the methods in the fragment
            btnJoinWaitlist.setOnClickListener(v -> {
                if (registrationOpen) {
                    listener.onJoinClick(event);
                }
            });

            btnLeaveWaitlist.setOnClickListener(v -> {
                if (registrationOpen) {
                    listener.onLeaveClick(event);
                }
            });

            itemView.setOnClickListener(v -> listener.onItemClick(event));
        }
    }
}
