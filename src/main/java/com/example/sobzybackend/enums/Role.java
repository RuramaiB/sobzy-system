package com.example.sobzybackend.enums;


/**
 * User role enumeration
 * Defines different access levels in the system
 */
public enum Role {
    ADMIN("Administrator with full system access"),
    USER("Regular user with limited access"),
    GUEST("Guest user with minimal access");

    private final String description;

    Role(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean hasAdminPrivileges() {
        return this == ADMIN;
    }

    public boolean isUser() {
        return this == USER;
    }

    public boolean isGuest() {
        return this == GUEST;
    }
}
