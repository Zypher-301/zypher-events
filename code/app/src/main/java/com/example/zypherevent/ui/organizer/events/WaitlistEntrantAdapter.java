package com.example.zypherevent.ui.organizer.events;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.zypherevent.R;
import com.example.zypherevent.WaitlistEntry;
import com.example.zypherevent.userTypes.Entrant;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * An adapter to display a list of {@link Entrant} objects in a {@link RecyclerView}.
 * Displays entrant information including name, email, phone number, and join time.
 * Supports sorting and accepting entrants.
 */
public class WaitlistEntrantAdapter extends RecyclerView.Adapter<WaitlistEntrantAdapter.EntrantViewHolder> {

    private List<WaitlistEntry> entrantList;
    private OnAcceptClickListener acceptListener;

    /**
     * Interface for handling accept button clicks
     */
    public interface OnAcceptClickListener {
        void onAcceptClick(WaitlistEntry entry, int position);
    }

    /**
     * Constructs a WaitlistEntrantAdapter.
     *
     * @param entrantList The list of entrants to be displayed.
     */
    public WaitlistEntrantAdapter(List<WaitlistEntry> entrantList) {
        this.entrantList = entrantList;
    }

    /**
     * Constructs a WaitlistEntrantAdapter with accept listener.
     *
     * @param entrantList The list of entrants to be displayed.
     * @param acceptListener Listener for accept button clicks
     */
    public WaitlistEntrantAdapter(List<WaitlistEntry> entrantList, OnAcceptClickListener acceptListener) {
        this.entrantList = entrantList;
        this.acceptListener = acceptListener;
    }

    /**
     * Sort by newest first (most recent join time)
     */
    public void sortByNewest() {
        Collections.sort(entrantList, new Comparator<WaitlistEntry>() {
            @Override
            public int compare(WaitlistEntry e1, WaitlistEntry e2) {
                if (e1.getTimeJoined() == null) return 1;
                if (e2.getTimeJoined() == null) return -1;
                return e2.getTimeJoined().compareTo(e1.getTimeJoined());
            }
        });
        notifyDataSetChanged();
    }

    /**
     * Sort by oldest first (earliest join time)
     */
    public void sortByOldest() {
        Collections.sort(entrantList, new Comparator<WaitlistEntry>() {
            @Override
            public int compare(WaitlistEntry e1, WaitlistEntry e2) {
                if (e1.getTimeJoined() == null) return 1;
                if (e2.getTimeJoined() == null) return -1;
                return e1.getTimeJoined().compareTo(e2.getTimeJoined());
            }
        });
        notifyDataSetChanged();
    }

    /**
     * Sort by name alphabetically
     */
    public void sortByName() {
        Collections.sort(entrantList, new Comparator<WaitlistEntry>() {
            @Override
            public int compare(WaitlistEntry e1, WaitlistEntry e2) {
                String name1 = e1.getEntrant().getFirstName() + " " + e1.getEntrant().getLastName();
                String name2 = e2.getEntrant().getFirstName() + " " + e2.getEntrant().getLastName();
                return name1.compareToIgnoreCase(name2);
            }
        });
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EntrantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_entrant_profile, parent, false);
        return new EntrantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EntrantViewHolder holder, int position) {
        WaitlistEntry entry = entrantList.get(position);
        Entrant entrant = entry.getEntrant();
        holder.bind(entrant, entry.getTimeJoined());

        // Handle accept button click
        holder.btnAccept.setOnClickListener(v -> {
            if (acceptListener != null) {
                acceptListener.onAcceptClick(entry, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return entrantList.size();
    }

    /**
     * ViewHolder class for an entrant item in the RecyclerView.
     */
    public static class EntrantViewHolder extends RecyclerView.ViewHolder {

        TextView userName, userEmail, userPhone, timeJoined;
        ImageView profileImage;
        Button btnAccept;

        public EntrantViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.user_name);
            userEmail = itemView.findViewById(R.id.user_email);
            userPhone = itemView.findViewById(R.id.user_phone);
            timeJoined = itemView.findViewById(R.id.tvTimeJoined);
            profileImage = itemView.findViewById(R.id.profile_image);
            btnAccept = itemView.findViewById(R.id.btnMoveToAccepted);
        }

        /**
         * Binds data from an Entrant object and join timestamp to the views.
         *
         * @param entrant The Entrant object containing the data to be displayed.
         * @param joinTime The Date when the entrant joined the waitlist, or null if not available.
         */
        public void bind(Entrant entrant, java.util.Date joinTime) {
            // Set name
            String fullName = entrant.getFirstName() + " " + entrant.getLastName();
            userName.setText("Name: " + fullName);

            // Set email
            String email = entrant.getEmail();
            if (email != null && !email.isEmpty()) {
                userEmail.setText("Email: " + email);
            } else {
                userEmail.setText("Email: N/A");
            }

            // Set phone
            String phone = entrant.getPhoneNumber();
            if (phone != null && !phone.isEmpty() && !phone.equals("N/A")) {
                userPhone.setText("Phone: " + phone);
            } else {
                userPhone.setText("Phone: N/A");
            }

            // Set join time
            if (joinTime != null) {
                java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", java.util.Locale.getDefault());
                timeJoined.setText("Joined: " + dateFormat.format(joinTime));
            } else {
                timeJoined.setText("Joined: N/A");
            }
        }
    }
}
