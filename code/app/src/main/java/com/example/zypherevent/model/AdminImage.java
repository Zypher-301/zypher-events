
package com.example.zypherevent.model;

public class AdminImage {
    private String uploader;
    private String uploadDate;

    public AdminImage(String uploader, String uploadDate) {
        this.uploader = uploader;
        this.uploadDate = uploadDate;
    }

    // Getters
    public String getUploader() { return uploader; }
    public String getUploadDate() { return uploadDate; }
}