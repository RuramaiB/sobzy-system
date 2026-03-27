package com.example.sobzybackend.enums;

public enum DeviceStatus {
    ACTIVE("Device is active and allowed"),
    PENDING_AUTH("Device is connected but pending authentication"),
    INACTIVE("Device is currently disconnected"),
    BLOCKED("Device is blocked"),
    SUSPENDED("Device is temporarily suspended");

    private final String description;

    DeviceStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
