package com.example.zypherevent.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Date;
import java.util.Objects;

/**
 * This is not an actual class for the application. It is just a Mock class created for Demonstration
 * purposes.
 * @author Arunavo Dutta
 * @version 1.0
 */
public final class AdminImage {

    private final String imageId;
    private final String imageUrl;
    private final String uploader;
    private final Date uploadDate;

    /**
     * Constructs a new AdminImage.
     *
     * @param imageId   A unique identifier for the image. Cannot be null.
     * @param imageUrl  The URL where the image is stored. Cannot be null.
     * @param uploader  The identifier for the admin who uploaded the image. Cannot be null.
     * @param uploadDate The date and time the image was uploaded. Cannot be null.
     */
    public AdminImage(@NonNull String imageId, @NonNull String imageUrl, @NonNull String uploader, @NonNull Date uploadDate) {
        this.imageId = imageId;
        this.imageUrl = imageUrl;
        this.uploader = uploader;
        this.uploadDate = new Date(uploadDate.getTime()); // Defensive copy for immutability
    }

    // Getters
    @NonNull
    public String getImageId() {
        return imageId;
    }

    @NonNull
    public String getImageUrl() {
        return imageUrl;
    }

    @NonNull
    public String getUploader() {
        return uploader;
    }

    /**
     * Returns a defensive copy of the upload date to maintain immutability.
     * @return A new Date object representing the upload date.
     */
    @NonNull
    public Date getUploadDate() {
        return new Date(uploadDate.getTime());
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdminImage that = (AdminImage) o;
        return imageId.equals(that.imageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(imageId);
    }

    @NonNull
    @Override
    public String toString() {
        return "AdminImage{" +
                "imageId='" + imageId + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", uploader='" + uploader + '\'' +
                ", uploadDate=" + uploadDate +
                '}';
    }
}
