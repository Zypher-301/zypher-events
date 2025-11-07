package com.example.zypherevent.ui.organizer.events;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.zypherevent.R;
import com.example.zypherevent.WaitlistEntry;
import com.example.zypherevent.userTypes.Entrant;

import java.util.List;

/**
 * An adapter to display a list of {@link Entrant} objects in a {@link RecyclerView}.
 * <p>
 * This adapter is responsible for creating views for each entrant on a waitlist, binding
 * entrant data to those views. It displays entrant information including name, email, and phone number.
 *
 * @see Entrant
 * @see WaitlistEntrantAdapter.EntrantViewHolder
 */
public class WaitlistEntrantAdapter extends RecyclerView.Adapter<WaitlistEntrantAdapter.EntrantViewHolder> {

    private List<WaitlistEntry> entrantList;

    /**
     * Constructs a WaitlistEntrantAdapter.
     *
     * @param entrantList The list of entrants to be displayed.
     */
    public WaitlistEntrantAdapter(List<WaitlistEntry> entrantList) {
        this.entrantList = entrantList;
    }

    /**
     * Called when RecyclerView needs a new {@link EntrantViewHolder} to represent an entrant item.
     * <p>
     * This method inflates a new view from the {@code R.layout.item_entrant_profile} XML layout file,
     * which defines the UI for a single item in the list. It then creates and returns a new
     * {@code EntrantViewHolder} instance holding this new view.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position. This is the RecyclerView itself.
     * @param viewType The view type of the new View. This is not used in this adapter as there
     *                 is only one type of item view.
     * @return A new {@code EntrantViewHolder} that holds the View for a single entrant item.
     */
    @NonNull
    @Override
    public EntrantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for each entrant row
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_entrant_profile, parent, false);
        return new EntrantViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * This method updates the contents of the {@link EntrantViewHolder#itemView} to reflect the
     * entrant at the given position in the list.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull EntrantViewHolder holder, int position) {
        // Get the specific waitlist entry for this row
        WaitlistEntry entry = entrantList.get(position);
        // Extract the entrant from the entry
        Entrant entrant = entry.getEntrant();
        // Bind the entrant data and timestamp to the views in that row
        holder.bind(entrant, entry.getTimeJoined());
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of entrants in this adapter's list.
     */
    @Override
    public int getItemCount() {
        return entrantList.size();
    }

    /**
     * ViewHolder class for an entrant item in the RecyclerView.
     * This class holds references to the UI components (views) within the item_entrant_profile.xml layout
     * for a single entrant, and binds the entrant data to these views.
     */
    public static class EntrantViewHolder extends RecyclerView.ViewHolder {

        // Declare the views from the layout
        TextView userName, userEmail, userPhone, timeJoined;

        /**
         * Constructs an instance of the {@code EntrantViewHolder}.
         * This constructor finds and initializes the UI components from the item layout
         * ({@code item_entrant_profile.xml}) that are used to display the entrant data.
         *
         * @param itemView The view for a single item in the RecyclerView, inflated from
         *                 the {@code item_entrant_profile.xml} layout. This view contains all the
         *                 UI elements for one entrant.
         */
        public EntrantViewHolder(@NonNull View itemView) {
            super(itemView);
            // Find all the views by their ID
            userName = itemView.findViewById(R.id.user_name);
            userEmail = itemView.findViewById(R.id.user_email);
            userPhone = itemView.findViewById(R.id.user_phone);
            timeJoined = itemView.findViewById(R.id.tvTimeJoined);
        }

        /**
         * Binds data from an {@link Entrant} object and join timestamp to the views in the ViewHolder.
         * <p>
         * This method populates the entrant's name, email, phone number, and the time they joined the waitlist.
         *
         * @param entrant The {@link Entrant} object containing the data to be displayed.
         * @param joinTime The {@link java.util.Date} when the entrant joined the waitlist, or null if not available.
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


