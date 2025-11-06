package com.example.zypherevent.model;

import java.util.Objects;

/**
 * Represents the profile of an administrator.
 * This class is immutable.
 * @author Arunavo Dutta
 * @version 1.1
 */
public final class AdminProfile { // Made class final
    private final String name;
    private final String role;
    private final String phone;
    private final String email;

    /**
     * No-argument constructor, useful for frameworks like JPA or Jackson.
     */
    public AdminProfile() {
        this.name = null;
        this.role = null;
        this.phone = null;
        this.email = null;
    }

    /**
     * Constructs an AdminProfile with specified details.
     *
     * @param name  The name of the admin.
     * @param role  The role of the admin.
     * @param phone The phone number of the admin.
     * @param email The email address of the admin.
     */
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



    @Override
    public String toString() {
        return "AdminProfile{" +
                "name='" + name + '\'' +
                ", role='" + role + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
