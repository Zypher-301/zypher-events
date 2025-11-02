// package com.example.zypherevent.ui.admin.events;
package com.example.zypherevent.ui.admin.events; // Use your actual package name

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

    private List<User> profileList; // Use real User model
    private OnDeleteListener deleteListener;

    public interface OnDeleteListener {
        void onDelete(User profile); // Use real User model
    }

    public AdminProfilesAdapter(List<User> profileList, OnDeleteListener deleteListener) {
        this.profileList = profileList;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_admin_item_profile_card, parent, false);
        return new ProfileViewHolder(view);
    }

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
            holder.profilePhone.setText("Phone: N/A"); // Organizer model doesn't have phone/email
            holder.profileEmail.setText("Email: N/A");
            holder.profileEventDetails.setText("Created Events: " + organizer.getCreatedEvents().size());
            holder.profileEventDetails.setVisibility(View.VISIBLE);

        } else { // Administrator
            holder.profilePhone.setText("Phone: N/A");
            holder.profileEmail.setText("Email: N/A");
            holder.profileEventDetails.setVisibility(View.GONE);
        }

        // Set the click listener for the delete button
        holder.deleteButton.setOnClickListener(v -> {
            deleteListener.onDelete(profile);
        });
    }

    @Override
    public int getItemCount() {
        return profileList.size();
    }

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