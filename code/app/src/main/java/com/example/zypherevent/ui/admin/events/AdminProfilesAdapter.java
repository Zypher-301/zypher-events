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
     * Interface for a callback to be invoked when a user profile is to be deleted.
     * This listener is implemented by the hosting fragment or activity to handle the deletion logic,
     * such as removing the user from the data source and updating the UI.
     */
    public interface OnDeleteListener {
        void onDelete(User profile);
    }

    /**
     * Constructs an AdminProfilesAdapter.
     *
     * @param profileList    A list of {@link User} objects to be displayed.
     * @param deleteListener A listener for handling delete actions on profiles.
     */
    public AdminProfilesAdapter(List<User> profileList, OnDeleteListener deleteListener) {
        this.profileList = profileList;
        this.deleteListener = deleteListener;
    }

    /**
     * Called when RecyclerView needs a new {@link ProfileViewHolder} of the given type to represent
     * an item.
     * <p>
     * This new ViewHolder should be constructed with a new View that can represent the items
     * of the given type. You can either create a new View manually or inflate it from an XML
     * layout file.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return A new ProfileViewHolder that holds a View of the given view type.
     * @see #onBindViewHolder(ProfileViewHolder, int)
     */
    @NonNull
    @Override
    public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_admin_item_profile_card, parent, false);
        return new ProfileViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position. This method
     * updates the contents of the {@link ProfileViewHolder#itemView} to reflect the item at the
     * given position.
     * <p>
     * It retrieves the {@link User} object from the list based on the position and binds its
     * data to the corresponding views in the ViewHolder. The method handles different user types
     * ({@code ENTRANT}, {@code ORGANIZER}, {@code ADMINISTRATOR}) and displays their specific
     * details accordingly. It also sets up a click listener for the delete button, which
     * triggers the {@link OnDeleteListener#onDelete(User)} callback when clicked.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     * @see #onCreateViewHolder(ViewGroup, int)
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
     * A ViewHolder class that describes an item view and metadata about its place within the RecyclerView.
     * It holds the UI components for a single profile item in the list, such as the user's name,
     * role, contact information, and a delete button. This class is responsible for finding and
     * caching the views from the layout file {@code fragment_admin_item_profile_card.xml} to avoid
     * repeated and costly {@code findViewById()} calls.
     */
    public static class ProfileViewHolder extends RecyclerView.ViewHolder {
        TextView profileName, profileRole, profilePhone, profileEmail, profileEventDetails;
        ImageView deleteButton;

        /**
         * Constructor for the ProfileViewHolder.
         * Initializes the views for a profile card item within the RecyclerView. It finds and
         * holds references to the UI components (TextViews and ImageView) in the item's layout.
         *
         * @param itemView The View for a single item in the RecyclerView, representing a user profile card.
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