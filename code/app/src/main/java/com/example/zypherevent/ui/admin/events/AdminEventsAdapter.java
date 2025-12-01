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
 * An adapter for displaying a list of {@link Event} objects in a {@link RecyclerView}
 * for the admin interface.
 * <p>
 * Updates:
 * 1. Uses Glide for image loading.
 * 2. Displays Event Start Time explicitly.
 * </p>
 *
 * @author Arunavo Dutta
 * @version 2.2
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

    public AdminEventsAdapter(List<Event> eventList, OnDeleteListener deleteListener) {
        this.eventList = eventList;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_admin_item_event_card, parent, false);
        return new EventViewHolder(view);
    }

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
        holder.eventLotteryDetails.setText("Reg: " + regStart + " to " + regEnd);

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

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView eventName, eventTime, eventLotteryDetails, eventDetails;
        ImageView eventImage;
        Button deleteButton;

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