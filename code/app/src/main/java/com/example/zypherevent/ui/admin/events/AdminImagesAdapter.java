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
 * @author Arunavo Dutta
 * @version 1.2
 * @see Event
 * @see res/layout/fragment_admin_item_image_card.xml
 */

public class AdminImagesAdapter extends RecyclerView.Adapter<AdminImagesAdapter.ImageViewHolder> {

    private List<Event> eventList;
    private OnDeleteListener deleteListener;

    public interface OnDeleteListener {
        void onDelete(Event event);
    }

    public AdminImagesAdapter(List<Event> eventList, OnDeleteListener deleteListener) {
        this.eventList = eventList;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_admin_item_image_card, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Event event = eventList.get(position);

        holder.eventName.setText(event.getEventName());
        holder.eventDescription.setText(event.getEventDescription());

        if (!TextUtils.isEmpty(event.getPosterURL())) {
            Glide.with(holder.itemView.getContext())
                    .load(event.getPosterURL())
                    .placeholder(R.drawable.ic_entrant_profile) // A placeholder drawable
                    .error(R.drawable.ic_trash_bin) // An error drawable
                    .into(holder.imagePreview);
        } else {
            holder.imagePreview.setImageResource(R.drawable.ic_images); // Default image if no URL
        }


        holder.deleteButton.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public void updateData(List<Event> newEventList) {
        this.eventList.clear();
        this.eventList.addAll(newEventList);
        notifyDataSetChanged();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imagePreview, deleteButton;
        TextView eventName, eventDescription;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imagePreview = itemView.findViewById(R.id.image_preview);
            eventName = itemView.findViewById(R.id.image_uploader); // Re-using this TextView
            eventDescription = itemView.findViewById(R.id.image_upload_date); // Re-using this TextView
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
}
