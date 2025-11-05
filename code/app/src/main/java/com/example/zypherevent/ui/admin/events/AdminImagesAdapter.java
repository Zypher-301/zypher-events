package com.example.zypherevent.ui.admin.events;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.zypherevent.R;
import com.example.zypherevent.model.AdminEvent;
import com.example.zypherevent.model.AdminImage;
import java.util.List;

/**
 * @author Arunavo Dutta
 * @version 1.0
 * @see AdminImage
 * @see res/layout/fragment_admin_item_image_card.xml
 */

public class AdminImagesAdapter extends RecyclerView.Adapter<AdminImagesAdapter.ImageViewHolder> {

    private List<AdminImage> imageList;
    private OnDeleteListener deleteListener;

    public interface OnDeleteListener {
        void onDelete(AdminImage image);
    }

    public AdminImagesAdapter(List<AdminImage> imageList, OnDeleteListener deleteListener) {
        this.imageList = imageList;
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
        AdminImage image = imageList.get(position);
        holder.imageUploader.setText("Uploaded by: " + image.getUploader());
        holder.imageUploadDate.setText("Upload date: " + image.getUploadDate());

        // TODO: Load image from Firebase Storage into holder.imagePreview
        // holder.imagePreview.setImageResource(R.drawable.ic_images); // Placeholder

        holder.deleteButton.setOnClickListener(v -> {
            deleteListener.onDelete(image);
        });
    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imagePreview, deleteButton;
        TextView imageUploader, imageUploadDate;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imagePreview = itemView.findViewById(R.id.image_preview);
            imageUploader = itemView.findViewById(R.id.image_uploader);
            imageUploadDate = itemView.findViewById(R.id.image_upload_date);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
}