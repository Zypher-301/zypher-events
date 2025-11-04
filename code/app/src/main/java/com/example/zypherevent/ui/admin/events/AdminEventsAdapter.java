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
import java.util.List;

/**
 * @author Arunavo Dutta
 * @version 2.0
 * @see Event
 * @see res/layout/fragment_admin_item_event_card.xml
 */
public class AdminEventsAdapter extends RecyclerView.Adapter<AdminEventsAdapter.EventViewHolder> {

    private List<Event> eventList;
    private OnDeleteListener deleteListener;

    /**
     * Interface for handling delete actions on events.
     * This listener is triggered when a user clicks the delete button on an event card.
     */
    public interface OnDeleteListener {
        void onDelete(Event event);
    }

    /**
     * Constructs an AdminEventsAdapter.
     *
     * @param eventList The list of {@link Event} objects to be displayed.
     * @param deleteListener The listener for delete button click events.
     */
    public AdminEventsAdapter(List<Event> eventList, OnDeleteListener deleteListener) {
        this.eventList = eventList;
        this.deleteListener = deleteListener;
    }

    /**
     * Called when RecyclerView needs a new {@link EventViewHolder} of the given type to represent
     * an item.
     * <p>
     * This new ViewHolder should be constructed with a new View that can represent the items
     * of the given type. You can either create a new View manually or inflate it from an XML
     * layout file.
     *
     * @param parent The ViewGroup into which the new View will be added after it is bound to
     * an adapter position.
     * @param viewType The view type of the new View.
     * @return A new EventViewHolder that holds a View of the given view type.
     * @see #onBindViewHolder(EventViewHolder, int)
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
     * This method updates the contents of the {@link EventViewHolder#itemView} to reflect the
     * event item at the given position in the data set. It binds the event's name, start time,
     * description, and lottery details to the corresponding TextViews. It also sets up an
     * OnClickListener for the delete button, which triggers the {@link OnDeleteListener#onDelete(Event)}
     * callback when clicked.
     *
     * @param holder The ViewHolder which should be updated to represent the contents of the
     * item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position); // <-- USING REAL EVENT

        // --- Start: Bind all requested key details ---

        // 1. Event Name
        holder.eventName.setText(event.getEventName());

        // 2. Registration Start Time
        holder.eventTime.setText("Registration Opens: " + event.getRegistrationStartTime());

        // 3. Registration End Time
        holder.eventLotteryDetails.setText("Registration Closes: " + event.getRegistrationEndTime());

        // 4. Description, Location, and Organizer ID
        // We are combining these into the 'details' text view.
        String details = "Description: " + event.getEventDescription() +
                "\nAt: " + event.getLocation() +
                "\nOrganizer: " + event.getEventOrganizerHardwareID();
        holder.eventDetails.setText(details);

        // --- End ---


        holder.deleteButton.setOnClickListener(v -> {
            deleteListener.onDelete(event);
        });
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     * This is used by the RecyclerView to determine how many items to display.
     *
     * @return The total number of events in the eventList.
     */
    @Override
    public int getItemCount() {
        return eventList.size();
    }

    /**
     * ViewHolder class for displaying individual event items in a RecyclerView for the admin interface.
     * It holds and initializes the UI components for an event card, such as the event name,
     * time, details, and a delete button.
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