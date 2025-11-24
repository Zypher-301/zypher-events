package com.example.zypherevent.ui.organizer.events;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.zypherevent.Event;
import com.example.zypherevent.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for displaying events created by an organizer.
 * Each item shows the event title, date, time, location, and poster image,
 * along with actions to view entrants, open the entrant list, or access additional event options
 * via a menu. User interactions are forwarded through the OnItemClickListener interface.
 */
public class OrganizerEventsAdapter extends RecyclerView.Adapter<OrganizerEventsAdapter.EventViewHolder> {

    private List<Event> eventList;
    private OnItemClickListener listener;

    /**
     * Listener interface for organizer event item interactions.
     * Implementations receive callbacks when the user requests to view entrants,
     * open the entrant list, or open the overflow menu for a specific event.
     */
    public interface OnItemClickListener {

        /**
         * Called when the "View Entrants" action is selected for an event.
         *
         * @param event the event whose entrants should be displayed
         */
        void onViewEntrantsClick(Event event);

        /**
         * Called when the "Entrant List" action is selected for an event.
         *
         * @param event the event whose entrant list should be opened
         */
        void onEntrantListClick(Event event);

        /**
         * Called when the menu button is tapped for an event.
         *
         * @param event      the event associated with the menu
         * @param anchorView the view to anchor any popup menu or contextual UI
         */
        void onMenuClick(Event event, View anchorView);
    }

    /**
     * Creates a new adapter for displaying organizer events.
     *
     * @param eventList the initial list of events to display
     * @param listener  the listener to notify of item interactions
     */
    public OrganizerEventsAdapter(List<Event> eventList, OnItemClickListener listener) {
        this.eventList = eventList;
        this.listener = listener;
    }

    /**
     * Inflates the layout for an event item view and wraps it in a EventViewHolder.
     *
     * @param parent   the parent view group into which the new view will be added
     * @param viewType the view type of the new view
     * @return a new EventViewHolder instance
     */
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_organizer_event_item, parent, false);
        return new EventViewHolder(view);
    }

    /**
     * Binds an event to the given EventViewHolder at the specified position.
     *
     * @param holder   the view holder to bind data to
     * @param position the position of the item within the adapter's data set
     */
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.bind(event, listener);
    }

    /**
     * Returns the total number of events in the adapter.
     *
     * @return the number of items currently held by the adapter
     */
    @Override
    public int getItemCount() {
        return eventList.size();
    }

    /**
     * View holder responsible for displaying a single organizer event item.
     * The view holder binds event metadata, loads the poster image if available, and
     * wires up click listeners for the entrant and menu actions.
     */
    public static class EventViewHolder extends RecyclerView.ViewHolder {

        TextView tvTitle, tvMeta;
        Button btnViewEntrants;
        Button btnMenu;
        Button btnEntrantList;
        ImageView imgPoster;

        /**
         * Creates a new view holder for an organizer event item.
         *
         * @param itemView the root view of the event item layout
         */
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            btnViewEntrants = itemView.findViewById(R.id.btnViewEntrants);
            btnEntrantList = itemView.findViewById(R.id.btnEntrantList);
            btnMenu = itemView.findViewById(R.id.btnMenu);
            imgPoster = itemView.findViewById(R.id.imgPoster);
        }

        /**
         * Binds the given event's data to the item views and sets up click listeners.
         * This method formats the event's start date and time, appends the location,
         * and loads the poster image if a URL is available. It also forwards button
         * clicks to the supplied OnItemClickListener.
         *
         * @param event    the event whose data should be displayed
         * @param listener the listener to notify when actions are triggered
         */
        public void bind(final Event event, final OnItemClickListener listener) {
            tvTitle.setText(event.getEventName());

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

            // Set Button Listeners
            btnViewEntrants.setOnClickListener(v -> listener.onViewEntrantsClick(event));
            btnEntrantList.setOnClickListener(v -> listener.onEntrantListClick(event));
            btnMenu.setOnClickListener(v -> listener.onMenuClick(event, v));

            // load poster from URL if available
            String posterUrl = event.getPosterURL();
            if (!TextUtils.isEmpty(posterUrl)) {
                Glide.with(itemView.getContext())
                        .load(posterUrl)
                        .placeholder(R.drawable.ic_images)
                        .error(R.drawable.ic_images)
                        .centerCrop()
                        .into(imgPoster);
            } else {
                // show placeholder otherwise
                imgPoster.setImageResource(R.drawable.ic_images);
            }
        }
    }
}