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
import java.util.List;

/**
 * Adapter for displaying the list of events an Entrant has joined.
 * Implements "Leave Waitlist" (US 01.01.02) functionality.
 */
public class JoinedEventAdapter extends RecyclerView.Adapter<JoinedEventAdapter.EventViewHolder> {

    private List<Event> eventList;
    private OnLeaveClickListener listener;

    /**
     * A simple listener for only the "Leave" click.
     */
    public interface OnLeaveClickListener {
        void onLeaveClick(Event event);
    }

    public JoinedEventAdapter(List<Event> eventList, OnLeaveClickListener listener) {
        this.eventList = eventList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
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

        TextView tvTitle, tvMeta, tvWaitlistCount;
        Button btnJoinWaitlist, btnLeaveWaitlist;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            tvWaitlistCount = itemView.findViewById(R.id.tvWaitlistCount);
            btnJoinWaitlist = itemView.findViewById(R.id.btnJoinWaitlist);
            btnLeaveWaitlist = itemView.findViewById(R.id.btnLeaveWaitlist);
        }

        /**
         * Binds a single Event object to the views.
         */
        public void bind(Event event, OnLeaveClickListener listener) {
            tvTitle.setText(event.getEventName());
            tvMeta.setText(event.getLocation());

            // US 01.05.04: Show waitlist count
            int waitlistSize = event.getWaitListEntrants().size();
            tvWaitlistCount.setText("On waiting list: " + waitlistSize);

            // Since this is the "Joined Events" list,
            // we always show the "Leave" button.
            btnJoinWaitlist.setVisibility(View.GONE);
            btnLeaveWaitlist.setVisibility(View.VISIBLE);

            btnLeaveWaitlist.setOnClickListener(v -> listener.onLeaveClick(event));
        }
    }
}