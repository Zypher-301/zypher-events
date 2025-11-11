package com.example.zypherevent.ui.admin.events;

import android.view.LayoutInflater;
import android.view.View;import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.zypherevent.R;
import com.example.zypherevent.model.AdminImage;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * @author Arunavo Dutta
 * @version 1.1
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
        SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM, yyyy", Locale.getDefault());

        holder.imageUploader.setText("Uploaded by: " + image.getUploader());
        holder.imageUploadDate.setText("Upload date: " + dateFormat.format(image.getUploadDate()));

        Glide.with(holder.itemView.getContext())
                .load(image.getImageUrl())
                .placeholder(R.drawable.ic_entrant_profile) // A placeholder drawable
                .error(R.drawable.ic_trash_bin) // An error drawable
                .into(holder.imagePreview);


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
