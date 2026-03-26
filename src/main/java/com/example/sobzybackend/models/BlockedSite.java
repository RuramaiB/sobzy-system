package com.example.sobzybackend.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "blocked_sites")
public class BlockedSite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String url;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt = LocalDateTime.now();

    public BlockedSite() {}

    public BlockedSite(Long id, String url, String reason, boolean active, LocalDateTime createdAt) {
        this.id = id;
        this.url = url;
        this.reason = reason;
        this.active = active;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static BlockedSiteBuilder builder() {
        return new BlockedSiteBuilder();
    }

    public static class BlockedSiteBuilder {
        private Long id;
        private String url;
        private String reason;
        private boolean active = true;
        private LocalDateTime createdAt = LocalDateTime.now();

        public BlockedSiteBuilder id(Long id) { this.id = id; return this; }
        public BlockedSiteBuilder url(String url) { this.url = url; return this; }
        public BlockedSiteBuilder reason(String reason) { this.reason = reason; return this; }
        public BlockedSiteBuilder active(boolean active) { this.active = active; return this; }
        public BlockedSiteBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public BlockedSite build() {
            return new BlockedSite(id, url, reason, active, createdAt);
        }
    }
}
