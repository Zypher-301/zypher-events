package com.example.zypherevent.ui.admin.events;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zypherevent.R;
import com.example.zypherevent.model.AdminNotificationLog;
import com.example.zypherevent.model.AdminProfile;
import java.util.List;
/**
 * @author Arunavo Dutta
 * @version 1.0
 * @see AdminProfile
 * @see res/layout/fragment_admin_item_profile_card.xml
 */
public class AdminProfilesAdapter extends RecyclerView.Adapter<AdminProfilesAdapter.ProfileViewHolder> {

    private List<AdminProfile> profileList;
    private OnDeleteListener deleteListener;

    // Interface for handling delete clicks
    public interface OnDeleteListener {
        void onDelete(AdminProfile profile);
    }

    public AdminProfilesAdapter(List<AdminProfile> profileList, OnDeleteListener deleteListener) {
        this.profileList = profileList;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the card layout you provided
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_admin_item_profile_card, parent, false);
        return new ProfileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileViewHolder holder, int position) {
        AdminProfile profile = profileList.get(position);

        // Bind data to the views
        holder.profileName.setText(profile.getName());
        holder.profileRole.setText(profile.getRole());
        holder.profilePhone.setText("Phone: " + profile.getPhone());
        holder.profileEmail.setText("Email: " + profile.getEmail());

        // Hide event details for now, or set if available
        holder.profileEventDetails.setVisibility(View.GONE);

        // Set the click listener for the delete button
        holder.deleteButton.setOnClickListener(v -> {
            deleteListener.onDelete(profile);
        });
    }

    @Override
    public int getItemCount() {
        return profileList.size();
    }

    // The ViewHolder class to hold the views
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