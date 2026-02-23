package com.example.sobzybackend.enums;

public enum AlertSeverity {
    LOW("Low priority alert"),
    MEDIUM("Medium priority alert"),
    HIGH("High priority alert"),
    CRITICAL("Critical priority alert");

    private final String description;

    AlertSeverity(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
