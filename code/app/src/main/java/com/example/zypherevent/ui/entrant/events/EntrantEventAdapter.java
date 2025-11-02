package com.example.zypherevent.ui.entrant.events;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
 * Manages and displays a list of {@link Event} objects in a RecyclerView for an entrant.
 * This adapter is responsible for creating view holders and binding event data to the views
 * defined in the {@code item_event.xml} layout file. It also handles item click events
 * to allow for interaction with individual events in the list.
 */
public class EntrantEventAdapter extends RecyclerView.Adapter<EntrantEventAdapter.EventViewHolder> {

    private List<Event> eventList;
    private OnItemClickListener listener;

    /**
     * A listener interface for receiving item click events from the event list.
     * The containing Fragment or Activity must implement this interface to handle
     * user interactions with individual events.
     */
    public interface OnItemClickListener {
        void onItemClick(Event event);
    }

    /**
     * Constructs a new EntrantEventAdapter.
     *
     * @param eventList The list of events to be displayed.
     * @param listener The listener that will handle item click events.
     */
    public EntrantEventAdapter(List<Event> eventList, OnItemClickListener listener) {
        this.eventList = eventList;
        this.listener = listener;
    }

    /**
     * Called when the RecyclerView needs a new {@link EventViewHolder} of the given type to represent
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
        // Inflate the item_event.xml layout for each row
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
        // Get the event for this position
        Event event = eventList.get(position);

        // Bind the event data to the ViewHolder
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
     * A ViewHolder that describes an event item view and metadata about its place
     * within the RecyclerView. It holds the UI components for a single event item
     * in the list, such as the event poster, title, and other details.
     */
    public static class EventViewHolder extends RecyclerView.ViewHolder {

        // Views from item_event.xml
        ImageView imgPoster;
        TextView tvTitle, tvMeta, tvWaitlistCount;
        LinearLayout slotActions;

        /**
         * Constructs a new EventViewHolder.
         *
         * @param itemView The view that represents a single item in the RecyclerView. This view
         *                 is inflated from the {@code item_event.xml} layout file.
         */
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);

            // Find all the views in the layout
            imgPoster = itemView.findViewById(R.id.imgPoster);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            tvWaitlistCount = itemView.findViewById(R.id.tvWaitlistCount);
            slotActions = itemView.findViewById(R.id.slotActions);
        }

        /**
         * Binds an {@link Event} object's data to the corresponding views in the item layout.
         * This method sets the event's title, location, and the current waitlist count.
         * It also attaches a click listener to the entire item view, which triggers the
         * {@link OnItemClickListener#onItemClick(Event)} callback when the item is tapped.
         *
         * @param event    The {@link Event} object containing the data to display.
         * @param listener The listener that will handle the click event for this item.
         */
        public void bind(Event event, OnItemClickListener listener) {
            // Set the event name
            tvTitle.setText(event.getEventName());

            // Set the event metadata (e.g., location)
            tvMeta.setText(event.getLocation());

            // --- THIS IS THE LOGIC FOR US 01.05.04 ---
            // Get the size of the waitlist from the Event object
            int waitlistSize = event.getWaitListEntrants().size();
            tvWaitlistCount.setText("On waiting list: " + waitlistSize);
            // ------------------------------------

            // TODO: Load the event poster image using Glide or Picasso
            // imgPoster.setImageResource(R.drawable.placeholder);

            // TODO: Add logic for the slotActions (Join, Leave, Accept buttons)
            // For now, we can just hide it to keep it simple
            slotActions.setVisibility(View.GONE);

            // Set the click listener for the whole item
            itemView.setOnClickListener(v -> listener.onItemClick(event));
        }
    }
}