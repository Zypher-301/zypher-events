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
import com.example.zypherevent.WaitlistEntry;
import com.example.zypherevent.userTypes.Entrant;

import java.util.List;

/**
 * BUG: DOUBLE CLICKING NEEDED FOR LEAVING THE WAITLIST
 * An adapter to display a list of {@link Event} objects for an {@link Entrant} in a {@link RecyclerView}.
 */
public class EntrantEventAdapter extends RecyclerView.Adapter<EntrantEventAdapter.EventViewHolder> {

    private List<Event> eventList;
    private Entrant currentUser;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Event event);
        void onJoinClick(Event event);
        void onLeaveClick(Event event);
    }

    public EntrantEventAdapter(List<Event> eventList, Entrant currentUser, OnItemClickListener listener) {
        this.eventList = eventList;
        this.currentUser = currentUser;
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
        holder.bind(event, currentUser, listener);
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {

        ImageView imgPoster;
        TextView tvTitle, tvMeta, tvWaitlistCount;
        TextView tvLotteryCriteriaLabel, tvLotteryCriteria;
        LinearLayout slotActions;
        Button btnJoinWaitlist, btnLeaveWaitlist;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
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

            boolean registrationOpen = event.isRegistrationOpen();
            String registrationStatus = event.getRegistrationStatus();

            boolean isOnWaitlist = false;
            for (WaitlistEntry entry : event.getWaitListEntrants()) {
                if (entry.getEntrant().equals(currentUser)) {
                    isOnWaitlist = true;
                    break;
                }
            }

            if (isOnWaitlist) {
                btnJoinWaitlist.setVisibility(View.GONE);
                btnLeaveWaitlist.setVisibility(View.VISIBLE);
                btnLeaveWaitlist.setEnabled(registrationOpen);
            } else {
                btnLeaveWaitlist.setVisibility(View.GONE);
                btnJoinWaitlist.setVisibility(View.VISIBLE);

                if (registrationOpen) {
                    btnJoinWaitlist.setEnabled(true);
                    btnJoinWaitlist.setText("Join Waitlist");
                } else {
                    btnJoinWaitlist.setEnabled(false);
                    btnJoinWaitlist.setText(registrationStatus);
                }
            }

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
