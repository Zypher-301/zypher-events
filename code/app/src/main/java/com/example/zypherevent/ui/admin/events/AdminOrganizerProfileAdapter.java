package com.example.zypherevent.ui.admin.events;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zypherevent.R;
import com.example.zypherevent.userTypes.Entrant;
import com.example.zypherevent.userTypes.Organizer;
import com.example.zypherevent.userTypes.User;
import com.example.zypherevent.userTypes.UserType;
import java.util.List;

/**
 * An adapter for displaying a list of ONLY ORGANIZER profiles in a {@link RecyclerView}.
 *
 * <p>This adapter is designed for an administrator's view, providing the functionality
 * to display organizer profiles and handle their deletion.
 * It populates a {@code RecyclerView} with data from a list of {@link User} objects,
 * mapping each organizer's data to a card-style layout defined in
 * {@code R.layout.fragment_admin_item_profile_card}.</p>
 *
 * <p>It uses an
 * {@link OnDeleteListener} interface to delegate the deletion action to the hosting
 * fragment, which contains the cascading delete logic.</p>
 *
 * @author Arunavo Dutta
 * @version 1.0
 * @see User
 * @see Organizer
 * @see res/layout/fragment_admin_item_profile_card.xml
 */
public class AdminOrganizerProfileAdapter extends RecyclerView.Adapter<AdminOrganizerProfileAdapter.ProfileViewHolder> {

    private List<User> organizerList;
    private OnDeleteListener deleteListener;

    /**
     * Defines a callback to be invoked when an organizer profile is selected for deletion.
     */
    public interface OnDeleteListener {
        void onDelete(User profile);
    }

    /**
     * Constructs an AdminOrganizerProfileAdapter.
     *
     * @param organizerList    A list of {@link User} objects (filtered to only Organizers)
     * to be displayed in the RecyclerView.
     * @param deleteListener A listener to handle delete actions.
     */
    public AdminOrganizerProfileAdapter(List<User> organizerList, OnDeleteListener deleteListener) {
        this.organizerList = organizerList;
        this.deleteListener = deleteListener;
    }

    /**
     * Inflates the item layout and creates the ViewHolder.
     *
     * @param parent   The ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View.
     * @return A new {@link ProfileViewHolder} that holds the View for a single profile item.
     */
    @NonNull
    @Override
    public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_admin_item_profile_card, parent, false);
        return new ProfileViewHolder(view);
    }

    /**
     * Binds the data from an {@link Organizer} object at a given position to a {@link ProfileViewHolder}.
     *
     * @param holder   The {@link ProfileViewHolder} that should be updated.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ProfileViewHolder holder, int position) {
        User profile = organizerList.get(position);

        // Bind common data from the User object
        holder.profileName.setText(profile.getFirstName() + " " + profile.getLastName());
        holder.profileRole.setText(profile.getUserType().toString()); // Will always be ORGANIZER

        // Set specific fields for the Organizer
        Organizer organizer = (Organizer) profile;
        holder.profilePhone.setText("Phone: N/A");
        holder.profileEmail.setText("Email: N/A");
        holder.profileEventDetails.setText("Created Events: " + organizer.getCreatedEvents().size());
        holder.profileEventDetails.setVisibility(View.VISIBLE);


        // Set the click listener for the delete button
        holder.deleteButton.setOnClickListener(v -> {
            deleteListener.onDelete(profile);
        });
    }

    /**
     * Returns the total number of profiles in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    @Override
    public int getItemCount() {
        return organizerList.size();
    }

    /**
     * A ViewHolder that describes an item view and metadata about its place within the RecyclerView.
     *
     * @see RecyclerView.ViewHolder
     * @see R.layout#fragment_admin_item_profile_card
     */
    public static class ProfileViewHolder extends RecyclerView.ViewHolder {
        TextView profileName, profileRole, profilePhone, profileEmail, profileEventDetails;
        ImageView deleteButton;

        public ProfileViewHolder(@NonNull View itemView) {
            super(itemView);
            profileName = itemView.findViewById(R.id.profile_name);
            profileRole = itemView.findViewById(R.id.profile_role);
            profilePhone = itemView.findViewById(R.id.profile_phone);
            profileEmail = itemView.findViewById(R.id.profile_email);
            profileEventDetails = itemView.findViewById(R.id.profile_event_details);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
}