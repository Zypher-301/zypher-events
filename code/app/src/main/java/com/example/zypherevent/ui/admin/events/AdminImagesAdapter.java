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
 * @author Arunavo Dutta
 * @version 2.0
 * @see Event
 * @see AdminImagesFragment
 * @see res/layout/fragment_admin_item_image_card.xml
 */
public class AdminImagesAdapter extends RecyclerView.Adapter<AdminImagesAdapter.ImageViewHolder> {

    private List<Event> eventList;
    private OnDeleteListener deleteListener;

    /**
     * Interface definition for a callback to be invoked when an image's delete button is clicked.
     * The listener provides the specific {@link Event} object and its position in the adapter,
     * allowing the calling fragment to handle the deletion logic.
     */
    public interface OnDeleteListener {
        void onDelete(Event event, int position);
    }

    /**
     * Constructs a new {@code AdminImagesAdapter}.
     *
     * @param eventList      The initial list of {@link Event} objects that have posters to display.
     * @param deleteListener The listener to be invoked when an image's delete button is clicked.
     */
    public AdminImagesAdapter(List<Event> eventList, OnDeleteListener deleteListener) {
        this.eventList = eventList;
        this.deleteListener = deleteListener;
    }

    /**
     * Called when RecyclerView needs a new {@link ImageViewHolder} of the given type to represent
     * an item.
     * <p>
     * This new ViewHolder is constructed with a new View that is inflated from the
     * {@code R.layout.fragment_admin_item_image_card} layout resource.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return A new {@link ImageViewHolder} that holds a View for a single image item.
     */
    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_admin_item_image_card, parent, false);
        return new ImageViewHolder(view);
    }

    /**
     * Binds the data from an {@link Event} object to the views within an {@link ImageViewHolder}.
     * <p>
     * This method is called by the RecyclerView to display the data at a specified position.
     * It sets the event's name and description. It uses the {@link Glide} library to load the
     * event's poster image from its URL into the {@code imagePreview} ImageView. A placeholder
     * and an error drawable are set for a better user experience during loading or if the image
     * fails to load.
     * <p>
     * It also attaches a click listener to the delete button. When clicked, this listener
     * invokes the {@link OnDeleteListener#onDelete(Event, int)} callback, passing the specific
     * event and its adapter position, allowing the hosting fragment to handle the deletion logic.
     *
     * @param holder   The {@link ImageViewHolder} which should be updated to represent the contents
     *                 of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Event event = eventList.get(position);

        holder.eventName.setText(event.getEventName());
        holder.eventDescription.setText(event.getEventDescription());

        // Use Glide to load the image
        if (!TextUtils.isEmpty(event.getPosterURL())) {
            Glide.with(holder.itemView.getContext())
                    .load(event.getPosterURL())
                    .placeholder(R.drawable.ic_images) // A generic placeholder
                    .error(R.drawable.ic_delete) // Error placeholder
                    .into(holder.imagePreview);
        } else {
            holder.imagePreview.setImageResource(R.drawable.ic_images);
        }

        // Set the click listener for the delete button
        holder.deleteButton.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(event, holder.getAdapterPosition());
            }
        });
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of events with images to be displayed.
     */
    @Override
    public int getItemCount() {
        return eventList.size();
    }

    /**
     * Updates the data set with a new list of events.
     * <p>
     * This method clears the existing list and replaces it with the new one,
     * then notifies the adapter that the entire data set has changed. This is
     * typically used when the underlying data source is completely refreshed.
     *
     * @param newEventList The new list of {@link Event} objects to display.
     */
    public void updateData(List<Event> newEventList) {
        this.eventList.clear();
        this.eventList.addAll(newEventList);
        notifyDataSetChanged();
    }

    /**
     * Removes an item from the adapter's data set at a specific position.
     * <p>
     * This method provides an efficient way to update the UI after an item has been
     * deleted, as it avoids a full reload of the data. It safely checks if the
     * position is valid before attempting to remove the item.
     *
     * @param position The adapter position of the item to remove.
     */
    public void removeItem(int position) {
        if (position >= 0 && position < eventList.size()) {
            eventList.remove(position);
            notifyItemRemoved(position);
        }
    }

    /**
     * A {@link RecyclerView.ViewHolder} that describes an image item view and its metadata.
     * <p>
     * This ViewHolder holds the UI components for a single image card in the admin image browser.
     * It includes an {@link ImageView} for the event poster, another {@link ImageView} that acts as a
     * delete button, and {@link TextView}s to display the associated event's name and description.
     * This provides administrators with the necessary context to manage images effectively.
     */
    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imagePreview, deleteButton;
        TextView eventName, eventDescription;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imagePreview = itemView.findViewById(R.id.image_preview);
            eventName = itemView.findViewById(R.id.image_uploader);
            eventDescription = itemView.findViewById(R.id.image_upload_date);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
}