package com.example.zypherevent.ui.admin.events;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.zypherevent.model.AdminProfile;
import java.util.ArrayList;
import java.util.List;

public class AdminProfileFragment extends AdminBaseListFragment {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState); // Sets up the RecyclerView

        // 1. Create placeholder data
        List<AdminProfile> profiles = new ArrayList<>();
        profiles.add(new AdminProfile("Amy McDonald", "Organizer", "(780) 123-4567", "amy.mcd@email.com"));
        profiles.add(new AdminProfile("Bob Smith", "Entrant", "(780) 321-7654", "bob.smith@email.com"));
        profiles.add(new AdminProfile("Charles Lee", "Entrant", "(587) 555-1212", "c.lee@email.com"));

        // 2. Create the adapter
        AdminProfilesAdapter adapter = new AdminProfilesAdapter(profiles, new AdminProfilesAdapter.OnDeleteListener() {
            @Override
            public void onDelete(AdminProfile profile) {
                // This is where you would call Firebase to delete the profile
                Toast.makeText(getContext(), "Deleting " + profile.getName(), Toast.LENGTH_SHORT).show();

                // TODO: Remove the item from the list and notify the adapter
                // profiles.remove(profile);
                // adapter.notifyDataSetChanged();
            }
        });

        // 3. Set the adapter on the RecyclerView
        recyclerView.setAdapter(adapter);
    }
}