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
import com.example.zypherevent.userTypes.User; // Using your real User model
import com.example.zypherevent.userTypes.UserType;
import java.util.List;

/**
 * @author Arunavo Dutta
 * @version 2.0
 * @see User
 * @see Entrant
 * @see Organizer
 * @see res/layout/fragment_admin_item_profile_card.xml
 */
public class AdminProfilesAdapter extends RecyclerView.Adapter<AdminProfilesAdapter.ProfileViewHolder> {

    private List<User> profileList;
    private OnDeleteListener deleteListener;

    /**
     * Defines a callback to be invoked when a user profile is selected for deletion.
     * <p>
     * This interface must be implemented by the hosting component (such as a Fragment or Activity)
     * that uses {@link AdminProfilesAdapter}. The implementation is responsible for handling the
     * actual deletion logic, which may include removing the user from the data source,
     * updating the database, and refreshing the user interface.
     */
    public interface OnDeleteListener {
        void onDelete(User profile);
    }

    /**
     * Constructs an AdminProfilesAdapter.
     * <p>
     * Initializes the adapter with the list of user profiles to be displayed
     * and a listener to handle delete actions.
     *
     * @param profileList    A list of {@link User} objects to be displayed in the RecyclerView.
     * @param deleteListener A listener that defines the callback method {@link OnDeleteListener#onDelete(User)}
     *                       to be invoked when the delete button for a profile is clicked.
     */
    public AdminProfilesAdapter(List<User> profileList, OnDeleteListener deleteListener) {
        this.profileList = profileList;
        this.deleteListener = deleteListener;
    }

    /**
     * Inflates the item layout and creates the ViewHolder.
     * <p>
     * This method is called by the RecyclerView when it needs a new {@link ProfileViewHolder}
     * to represent an item. It inflates the XML layout file for a single profile card
     * ({@code R.layout.fragment_admin_item_profile_card}) and returns a new instance
     * of {@link ProfileViewHolder} holding the inflated view.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position. This is the RecyclerView itself.
     * @param viewType The view type of the new View. This is not used in this adapter as
     *                 all items share the same layout.
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
     * Binds the data from a {@link User} object at a given position to a {@link ProfileViewHolder}.
     * <p>
     * This method is called by the RecyclerView to display the data for a specific item. It retrieves
     * the {@code User} object from the data set and populates the views within the
     * {@code ProfileViewHolder}. The method handles different user types (e.g., {@code ENTRANT},
     * {@code ORGANIZER}) by casting the {@code User} object and displaying type-specific details.
     * It also sets up a click listener on the delete button, which triggers the
     * {@link OnDeleteListener#onDelete(User)} callback when activated.
     *
     * @param holder   The {@link ProfileViewHolder} that should be updated to represent the
     *                 contents of the item at the given position.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ProfileViewHolder holder, int position) {
        User profile = profileList.get(position); // Use real User model

        // Bind common data from the User object
        holder.profileName.setText(profile.getFirstName() + " " + profile.getLastName());
        holder.profileRole.setText(profile.getUserType().toString());

        // Set specific fields based on the user type
        if (profile.getUserType() == UserType.ENTRANT) {
            Entrant entrant = (Entrant) profile;
            holder.profilePhone.setText("Phone: " + entrant.getPhoneNumber());
            holder.profileEmail.setText("Email: " + entrant.getEmail());
            holder.profileEventDetails.setVisibility(View.GONE); // Or show event history count

        } else if (profile.getUserType() == UserType.ORGANIZER) {
            Organizer organizer = (Organizer) profile;
            holder.profilePhone.setText("Phone: N/A");
            holder.profileEmail.setText("Email: N/A");
            holder.profileEventDetails.setText("Created Events: " + organizer.getCreatedEvents().size());
            holder.profileEventDetails.setVisibility(View.VISIBLE);

        } else { // Administrator
            holder.profilePhone.setText("Phone: N/A"); // Administrator model doesn't have phone/email
            holder.profileEmail.setText("Email: N/A");
            holder.profileEventDetails.setVisibility(View.GONE);
        }

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
        return profileList.size();
    }

    /**
     * A ViewHolder that describes an item view and metadata about its place within the RecyclerView.
     * <p>
     * This class holds the UI components for a single profile item in the list, such as the user's name,
     * role, contact information, and a delete button. It is responsible for finding and
     * caching the views from the layout file ({@code fragment_admin_item_profile_card.xml}) to avoid
     * repeated and costly {@code findViewById()} calls, improving performance.
     *
     * @see RecyclerView.ViewHolder
     * @see R.layout#fragment_admin_item_profile_card
     */
    public static class ProfileViewHolder extends RecyclerView.ViewHolder {
        TextView profileName, profileRole, profilePhone, profileEmail, profileEventDetails;
        ImageView deleteButton;

        /**
         * Constructor for the ProfileViewHolder.
         * <p>
         * Initializes the views for a profile card item within the RecyclerView. It finds and
         * holds references to the UI components (TextViews and ImageView) in the item's layout,
         * which is inflated from {@code R.layout.fragment_admin_item_profile_card}. This avoids
         * repeated {@code findViewById()} calls, improving performance.
         *
         * @param itemView The View for a single item in the RecyclerView, representing a user profile card.
         * @see R.id#profile_name
         * @see R.id#profile_role
         * @see R.id#profile_phone
         * @see R.id#profile_email
         * @see R.id#profile_event_details
         * @see R.id#delete_button
         */
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