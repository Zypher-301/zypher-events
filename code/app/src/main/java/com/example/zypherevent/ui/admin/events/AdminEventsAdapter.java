package com.example.zypherevent.ui.admin.events;

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
import java.util.List;
import java.util.Locale;


/**
 * Adapter for populating a {@link RecyclerView} with a list of {@link Event} objects
 * for the admin interface. This adapter is responsible for creating views for each event,
 * binding event data to those views, and handling interactions such as event deletion.
 * <p>
 * It displays key event details including the event name, start time, registration window,
 * description, location, and organizer. It uses the Glide library for efficiently loading
 * event posters from a URL. A delete button is provided for each event, which triggers a
 * callback to a listener.
 *
 * @author Arunavo Dutta
 * @version 2.2
 * @see RecyclerView.Adapter
 * @see Event
 * @see AdminEventsAdapter.EventViewHolder
 * @see OnDeleteListener
 */
public class AdminEventsAdapter extends RecyclerView.Adapter<AdminEventsAdapter.EventViewHolder> {

    private List<Event> eventList;
    private OnDeleteListener deleteListener;

    /**
     * Interface for a callback to be invoked when an event's delete button is clicked.
     */
    public interface OnDeleteListener {
        void onDelete(Event event);
    }

    /**
     * Constructs a new AdminEventsAdapter.
     *
     * @param eventList      A list of {@link Event} objects to be displayed.
     * @param deleteListener A listener to handle delete button clicks for each event.
     */
    public AdminEventsAdapter(List<Event> eventList, OnDeleteListener deleteListener) {
        this.eventList = eventList;
        this.deleteListener = deleteListener;
    }

    /**
     * Called when RecyclerView needs a new {@link EventViewHolder} of the given type to represent
     * an item.
     * <p>
     * This new ViewHolder is constructed with a new View that is inflated from the
     * {@code R.layout.fragment_admin_item_event_card} XML layout file.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return A new EventViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_admin_item_event_card, parent, false);
        return new EventViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position. This method
     * updates the contents of the {@link EventViewHolder#itemView} to reflect the event
     * at the given position in the list.
     * <p>
     * This method performs the following actions:
     * <ol>
     *     <li>Retrieves the {@link Event} object for the current position.</li>
     *     <li>Sets the event name.</li>
     *     <li>Formats and displays the event start date and registration window dates. If dates are null, "TBD" or "N/A" is shown.</li>
     *     <li>Concatenates and displays detailed event information, including description, location, and organizer ID.</li>
     *     <li>Loads the event poster image using Glide. If the URL is empty or invalid, a placeholder drawable is displayed.</li>
     *     <li>Sets an OnClickListener on the delete button to trigger the {@link OnDeleteListener#onDelete(Event)} callback when clicked.</li>
     * </ol>
     *
     * @param holder   The {@link EventViewHolder} which should be updated to represent the
     *                 contents of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // 1. Basic Info
        holder.eventName.setText(event.getEventName());

        // 2. Date Formatting (Retrieve and format the Event Start Time)
        String eventStart = (event.getStartTime() != null) ? formatter.format(event.getStartTime()) : "TBD";

        // Retrieve and format Registration Window
        String regStart = (event.getRegistrationStartTime() != null) ? formatter.format(event.getRegistrationStartTime()) : "N/A";
        String regEnd = (event.getRegistrationEndTime() != null) ? formatter.format(event.getRegistrationEndTime()) : "N/A";

        // 3. Set Text Fields
        // Primary Time: The actual event date
        holder.eventTime.setText("Event Start: " + eventStart);

        // Secondary Detail: The registration window
        holder.eventLotteryDetails.setText("Registration: " + regStart + " to " + regEnd);

        // 4. Detailed Description
        String details = "Description: " + event.getEventDescription() +
                "\nAt: " + event.getLocation() +
                "\nOrganizer: " + event.getEventOrganizerHardwareID();
        holder.eventDetails.setText(details);

        // 5. Image Loading
        if (!TextUtils.isEmpty(event.getPosterURL())) {
            Glide.with(holder.itemView.getContext())
                    .load(event.getPosterURL())
                    .placeholder(R.drawable.ic_images)
                    .error(R.drawable.ic_images)
                    .centerCrop()
                    .into(holder.eventImage);
        } else {
            holder.eventImage.setImageResource(R.drawable.ic_images);
        }

        // 6. Delete Action
        holder.deleteButton.setOnClickListener(v -> {
            deleteListener.onDelete(event);
        });
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
     * A {@link RecyclerView.ViewHolder} that describes an event item view and metadata about its place
     * within the {@link RecyclerView}. This holder is used in the admin interface to display
     * event details and provide an option to delete the event.
     * <p>
     * It holds references to the UI components (like {@link TextView}, {@link ImageView}, and {@link Button})
     * for each individual event card in the list.
     * </p>
     */
    public static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView eventName, eventTime, eventLotteryDetails, eventDetails;
        ImageView eventImage;
        Button deleteButton;

        /**
         * Constructs an {@code EventViewHolder}.
         * Initializes the UI components of the event card by finding their respective views
         * within the item layout.
         *
         * @param itemView The view of the individual list item.
         */
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventImage = itemView.findViewById(R.id.event_image);
            eventName = itemView.findViewById(R.id.event_name);
            eventTime = itemView.findViewById(R.id.event_time);
            eventLotteryDetails = itemView.findViewById(R.id.event_lottery_details);
            eventDetails = itemView.findViewById(R.id.event_details);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
}