package com.example.zypherevent.ui.admin.events;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.zypherevent.Event;
import com.example.zypherevent.R;

import java.util.List;

/**
 * An adapter for displaying a list of {@link Event} objects that have images
 * for the admin interface.
 * <p>
 * This adapter is responsible for creating and binding views for each event's poster,
 * allowing an administrator to view the image, see which event it belongs to, and
 * delete the image (by nullifying its posterURL).
 * <p>
 * This fulfills parts of:
 * <ul>
 * <li><b>US 03.06.01:</b> As an administrator, I want to be able to browse images.</li>
 * <li><b>US 03.03.01:</b> As an administrator, I want to be able to remove images.</li>
 * </ul>
 *
 * @author Arunavo Dutta (Refactored)
 * @version 2.0
 * @see Event
 * @see AdminImagesFragment
 * @see res/layout/fragment_admin_item_image_card.xml
 */
public class AdminImagesAdapter extends RecyclerView.Adapter<AdminImagesAdapter.ImageViewHolder> {

    private List<Event> eventList;
    private OnDeleteListener deleteListener;

    /**
     * Interface for a callback to be invoked when an event's delete image button is clicked.
     */
    public interface OnDeleteListener {
        void onDelete(Event event, int position);
    }

    /**
     * Constructs a new AdminImagesAdapter.
     *
     * @param eventList      The list of {@link Event} objects (with posters) to display.
     * @param deleteListener The listener that will be invoked when the delete button is clicked.
     */
    public AdminImagesAdapter(List<Event> eventList, OnDeleteListener deleteListener) {
        this.eventList = eventList;
        this.deleteListener = deleteListener;
    }

    /**
     * Inflates the layout for an individual image item and returns a new {@link ImageViewHolder} instance.
     *
     * @param parent   The ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View.
     * @return A new {@link ImageViewHolder} that holds the View for a single image item.
     */
    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_admin_item_image_card, parent, false);
        return new ImageViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * <p>
     * This method updates the contents of the {@link ImageViewHolder#itemView} to reflect the
     * event item at the given position. It binds the event's name and description, and uses
     * {@link Glide} to load the {@code posterURL} into the {@link ImageView}.
     * <p>
     * It also sets an {@link View.OnClickListener} on the delete button, which invokes the
     * {@link OnDeleteListener#onDelete(Event, int)} callback.
     *
     * @param holder   The {@link ImageViewHolder} which should be updated.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Event event = eventList.get(position);

        // We re-use the text views from the layout to show the event name and description
        holder.eventName.setText(event.getEventName());
        holder.eventDescription.setText(event.getEventDescription());

        // Use Glide to load the image
        if (!TextUtils.isEmpty(event.getPosterURL())) {
            Glide.with(holder.itemView.getContext())
                    .load(event.getPosterURL())
                    .placeholder(R.drawable.ic_images) // A generic placeholder
                    .error(R.drawable.ic_delete)     // An error drawable
                    .into(holder.imagePreview);
        } else {
            // Should not happen if fragment filters correctly, but good to have a fallback
            holder.imagePreview.setImageResource(R.drawable.ic_images);
        }

        // Set the click listener for the delete button
        holder.deleteButton.setOnClickListener(v -> {
            if (deleteListener != null) {
                // Pass both the event and its position for efficient removal
                deleteListener.onDelete(event, holder.getAdapterPosition());
            }
        });
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of events in the {@code eventList}.
     */
    @Override
    public int getItemCount() {
        return eventList.size();
    }

    /**
     * Updates the data set with a new list of events and notifies the adapter.
     *
     * @param newEventList The new list of events to display.
     */
    public void updateData(List<Event> newEventList) {
        this.eventList.clear();
        this.eventList.addAll(newEventList);
        notifyDataSetChanged();
    }

    /**
     * Removes an item from the list at a specific position.
     * This is more efficient than reloading the entire list from the database.
     *
     * @param position The position of the item to remove.
     */
    public void removeItem(int position) {
        if (position >= 0 && position < eventList.size()) {
            eventList.remove(position);
            notifyItemRemoved(position);
        }
    }

    /**
     * A {@link RecyclerView.ViewHolder} that describes an image item view.
     * <p>
     * It holds the UI components for a single image card, including {@link ImageView}s
     * for the preview and delete button, and {@link TextView}s for the event's name
     * and description.
     */
    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imagePreview, deleteButton;
        TextView eventName, eventDescription;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imagePreview = itemView.findViewById(R.id.image_preview);
            // Re-using the layout's TextViews to show event context
            eventName = itemView.findViewById(R.id.image_uploader);
            eventDescription = itemView.findViewById(R.id.image_upload_date);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
}