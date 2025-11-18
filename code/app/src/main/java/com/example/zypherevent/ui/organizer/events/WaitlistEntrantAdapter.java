package com.example.zypherevent.ui.organizer.events;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.zypherevent.Database;
import com.example.zypherevent.R;
import com.example.zypherevent.WaitlistEntry;
import com.example.zypherevent.userTypes.Entrant;
import com.example.zypherevent.userTypes.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An adapter to display a list of {@link Entrant} objects in a {@link RecyclerView}.
 * Displays entrant information including name, email, phone number, and join time.
 * Supports sorting and accepting entrants.
 */
public class WaitlistEntrantAdapter extends RecyclerView.Adapter<WaitlistEntrantAdapter.EntrantViewHolder> {

    private List<WaitlistEntry> entrantList;
    private OnAcceptClickListener acceptListener;
    private java.util.Map<String, Entrant> entrantCache = new java.util.HashMap<>();
    private Database db = new Database();

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
     * Sort by name alphabetically (using entrant info loaded from the database).
     *
     * This is asynchronous: it fetches Entrant objects for the hardware IDs first,
     * then sorts and refreshes the list when done.
     */
    public void sortByName() {
        if (entrantList == null || entrantList.isEmpty()) {
            return;
        }

        // Collect unique hardware IDs from the waitlist
        final Set<String> uniqueIdsSet = new HashSet<>();
        final java.util.List<String> idList = new java.util.ArrayList<>();

        for (WaitlistEntry entry : entrantList) {
            if (entry == null) continue;
            String id = entry.getEntrantHardwareID();
            if (id != null && !id.isEmpty() && uniqueIdsSet.add(id)) {
                // add returns true only if it was not already in the set
                idList.add(id);
            }
        }

        if (idList.isEmpty()) {
            return;
        }

        // Build lookup tasks for each unique hardware ID
        java.util.List<Task<User>> lookupTasks = new java.util.ArrayList<>();
        for (String id : idList) {
            lookupTasks.add(db.getUser(id));
        }

        // When all user lookups succeed, build a map and sort
        Tasks.whenAllSuccess(lookupTasks)
                .addOnCompleteListener(task -> {
                    Map<String, Entrant> entrantsById = new HashMap<>();

                    if (task.isSuccessful()) {
                        java.util.List<?> results = task.getResult();
                        if (results != null) {
                            // results are in the same order as lookupTasks / idList
                            for (int i = 0; i < results.size() && i < idList.size(); i++) {
                                Object obj = results.get(i);
                                if (obj instanceof Entrant) {
                                    entrantsById.put(idList.get(i), (Entrant) obj);
                                }
                            }
                        }
                    } else {
                        Log.w("WaitlistEntrantAdapter",
                                "Failed to load entrants for sortByName",
                                task.getException());
                    }

                    // Sort entrantList using the resolved names (fallback to hardware ID if needed)
                    Collections.sort(entrantList, new Comparator<WaitlistEntry>() {
                        @Override
                        public int compare(WaitlistEntry e1, WaitlistEntry e2) {
                            if (e1 == null && e2 == null) return 0;
                            if (e1 == null) return 1;
                            if (e2 == null) return -1;

                            String id1 = e1.getEntrantHardwareID();
                            String id2 = e2.getEntrantHardwareID();

                            Entrant ent1 = id1 != null ? entrantsById.get(id1) : null;
                            Entrant ent2 = id2 != null ? entrantsById.get(id2) : null;

                            String name1;
                            String name2;

                            if (ent1 != null) {
                                name1 = ent1.getFirstName() + " " + ent1.getLastName();
                            } else {
                                // Fallback: use ID if entrant couldn't be loaded
                                name1 = (id1 != null) ? id1 : "";
                            }

                            if (ent2 != null) {
                                name2 = ent2.getFirstName() + " " + ent2.getLastName();
                            } else {
                                name2 = (id2 != null) ? id2 : "";
                            }

                            return name1.compareToIgnoreCase(name2);
                        }
                    });

                    notifyDataSetChanged();
                });
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
        String hardwareId = entry.getEntrantHardwareID();

        // Try cached entrant first
        Entrant cachedEntrant = null;
        if (hardwareId != null) {
            cachedEntrant = entrantCache.get(hardwareId);
        }

        if (cachedEntrant != null) {
            // We already have the Entrant object, bind immediately
            holder.bind(cachedEntrant, entry.getTimeJoined());
        } else {
            // Show a lightweight placeholder while we load from DB
            holder.userName.setText("Name: Loading...");
            holder.userEmail.setText("Email: Loading...");
            holder.userPhone.setText("Phone: N/A");
            if (entry.getTimeJoined() != null) {
                java.text.SimpleDateFormat dateFormat =
                        new java.text.SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a",
                                java.util.Locale.getDefault());
                holder.timeJoined.setText("Joined: " + dateFormat.format(entry.getTimeJoined()));
            } else {
                holder.timeJoined.setText("Joined: N/A");
            }

            // Fetch from DB if we have a valid hardware ID
            if (hardwareId != null && !hardwareId.isEmpty()) {
                db.getUser(hardwareId)
                        .addOnSuccessListener(user -> {
                            if (!(user instanceof Entrant)) {
                                return; // not an Entrant, ignore
                            }
                            Entrant entrant = (Entrant) user;
                            entrantCache.put(hardwareId, entrant);

                            // Ensure this ViewHolder is still displaying the same item
                            int adapterPos = holder.getAdapterPosition();
                            if (adapterPos == RecyclerView.NO_POSITION) {
                                return; // view was recycled
                            }
                            WaitlistEntry currentEntry = entrantList.get(adapterPos);
                            if (!hardwareId.equals(currentEntry.getEntrantHardwareID())) {
                                return; // holder now bound to a different entry
                            }

                            // Now safely bind the loaded entrant
                            holder.bind(entrant, currentEntry.getTimeJoined());
                        })
                        .addOnFailureListener(e -> {
                            // Optional: log or show fallback
                            holder.userName.setText("Name: Unknown");
                            holder.userEmail.setText("Email: N/A");
                            holder.userPhone.setText("Phone: N/A");
                        });
            }
        }

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
        Button btnAccept;

        public EntrantViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.user_name);
            userEmail = itemView.findViewById(R.id.user_email);
            userPhone = itemView.findViewById(R.id.user_phone);
            timeJoined = itemView.findViewById(R.id.tvTimeJoined);
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
