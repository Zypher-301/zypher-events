package com.example.zypherevent.ui.admin.events;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zypherevent.Event;
import com.example.zypherevent.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * An adapter for displaying a list of {@link Event} objects in a {@link RecyclerView}
 * for the admin interface.
 * <p>
 * This class is responsible for creating and binding views for each event in the list,
 * allowing an administrator to view event details and delete them. It uses a custom
 * layout {@code fragment_admin_item_event_card.xml} to define the appearance of each
 * event item.
 * <p>
 * The adapter requires an implementation of the {@link OnDeleteListener} interface to handle
 * the deletion of events, delegating the actual data removal logic to the hosting
 * component (e.g., a Fragment or Activity).
 *
 * @author Arunavo Dutta
 * @version 2.0
 * @see Event
 * @see AdminEventsAdapter.OnDeleteListener
 * @see AdminEventsAdapter.EventViewHolder
 * @see com.example.zypherevent.R.layout#fragment_admin_item_event_card
 */
public class AdminEventsAdapter extends RecyclerView.Adapter<AdminEventsAdapter.EventViewHolder> {

    private List<Event> eventList;
    private OnDeleteListener deleteListener;

    /**
     * Interface for a callback to be invoked when an event's delete button is clicked.
     * <p>
     * Implement this interface to define the action that should be taken when the user
     * requests to delete an event from the list.
     */
    public interface OnDeleteListener {
        void onDelete(Event event);
    }

    /**
     * Constructs a new AdminEventsAdapter.
     * <p>
     * This adapter is responsible for creating views for events and binding data to them
     * for the admin interface. It requires a list of events to display and a listener
     * to handle deletion actions.
     *
     * @param eventList      The list of {@link Event} objects to be displayed in the RecyclerView.
     * @param deleteListener The listener that will be invoked when the delete button on an event is clicked.
     *                       This allows the calling component (e.g., a Fragment or Activity) to handle the
     *                       actual deletion logic.
     * @see OnDeleteListener
     */
    public AdminEventsAdapter(List<Event> eventList, OnDeleteListener deleteListener) {
        this.eventList = eventList;
        this.deleteListener = deleteListener;
    }

    /**
     * Inflates the layout for an individual event item and returns a new {@link EventViewHolder} instance.
     * <p>
     * This method is called by the RecyclerView when it needs a new ViewHolder to represent an item.
     * It inflates the {@code fragment_admin_item_event_card.xml} layout, which defines the visual
     * structure of a single event card in the admin's event list.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position. This is the RecyclerView itself.
     * @param viewType The view type of the new View. This is not used in this adapter as there is
     *                 only one type of item view.
     * @return A new {@link EventViewHolder} that holds the View for a single event item.
     */
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_admin_item_event_card, parent, false);
        return new EventViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * <p>
     * This method updates the contents of the {@link EventViewHolder#itemView} to reflect the
     * event item at the given position. It binds the event's data (name, start/end times,
     * description, location, and organizer ID) to the corresponding UI elements within the
     * {@code EventViewHolder}.
     * <p>
     * It also sets an {@link View.OnClickListener} on the delete button. When clicked, this
     * listener invokes the {@link OnDeleteListener#onDelete(Event)} callback, passing the
     * specific {@link Event} object associated with that view holder.
     *
     * @param holder The {@link EventViewHolder} which should be updated to represent the contents of the
     *               item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {

        Event event = eventList.get(position);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        holder.eventName.setText(event.getEventName());

        String regStart = (event.getRegistrationStartTime() != null) ? formatter.format(event.getRegistrationStartTime()) : "N/A";
        String regEnd = (event.getRegistrationEndTime() != null) ? formatter.format(event.getRegistrationEndTime()) : "N/A";

        holder.eventTime.setText("Registration Opens: " + regStart);
        holder.eventLotteryDetails.setText("Registration Closes: " + regEnd);


        // Description, Location, and Organizer ID
        // Combining these into the 'details' text view.
        String details = "Description: " + event.getEventDescription() +
                "\nAt: " + event.getLocation() +
                "\nOrganizer: " + event.getEventOrganizerHardwareID();
        holder.eventDetails.setText(details);



        holder.deleteButton.setOnClickListener(v -> {
            deleteListener.onDelete(event);
        });
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     * <p>
     * This method is used by the RecyclerView to determine how many items to display.
     * It is essential for the layout manager to correctly size and position the views.
     *
     * @return The total number of events in the {@code eventList}.
     */
    @Override
    public int getItemCount() {
        return eventList.size();
    }

    /**
     * A {@link RecyclerView.ViewHolder} that describes an event item view and metadata about its place
     * within the {@link RecyclerView}. This is specifically for the admin interface.
     * <p>
     * It holds the UI components for a single event card, including {@link TextView}s for the
     * event's name, start time, registration closing time (labeled as lottery details), and other
     * combined details (description, location, organizer). It also includes a {@link Button}
     * for deleting the event.
     *
     * @see AdminEventsAdapter
     * @see res/layout/fragment_admin_item_event_card.xml
     */
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