
package com.example.zypherevent.model;

public class AdminProfile {
    private String name;
    private String role;
    private String phone;
    private String email;

    // Constructor for placeholder data
    public AdminProfile(String name, String role, String phone, String email) {
        this.name = name;
        this.role = role;
        this.phone = phone;
        this.email = email;
    }

    // Getters
    public String getName() { return name; }
    public String getRole() { return role; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
}