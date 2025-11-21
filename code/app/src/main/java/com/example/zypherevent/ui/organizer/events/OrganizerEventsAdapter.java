package com.example.zypherevent.ui.organizer.events;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.zypherevent.Event;
import com.example.zypherevent.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrganizerEventsAdapter extends RecyclerView.Adapter<OrganizerEventsAdapter.EventViewHolder> {

    private List<Event> eventList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onViewEntrantsClick(Event event);
        void onEntrantListClick(Event event);
        void onMenuClick(Event event, View anchorView);
    }

    public OrganizerEventsAdapter(List<Event> eventList, OnItemClickListener listener) {
        this.eventList = eventList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_organizer_event_item, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.bind(event, listener);
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {

        TextView tvTitle, tvMeta;
        Button btnViewEntrants;
        Button btnMenu;
        Button btnEntrantList;
        ImageView imgPoster;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            btnViewEntrants = itemView.findViewById(R.id.btnViewEntrants);
            btnEntrantList = itemView.findViewById(R.id.btnEntrantList);
            btnMenu = itemView.findViewById(R.id.btnMenu);
            imgPoster = itemView.findViewById(R.id.imgPoster);
        }

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

            // Set the placeholder image
            imgPoster.setImageResource(R.drawable.ic_images);
        }
    }
}
