package com.example.sobzybackend.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "traffic_events", indexes = {
        @Index(name = "idx_event_ip", columnList = "ip_address"),
        @Index(name = "idx_event_type", columnList = "event_type"),
        @Index(name = "idx_event_timestamp", columnList = "timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrafficEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ip_address", length = 45, nullable = false)
    private String ipAddress;

    @Column(name = "event_type", length = 50, nullable = false)
    private String eventType; // CLICK, INPUT, COPY, FOCUS, SCROLL

    @Column(length = 2048)
    private String url;

    @Column(columnDefinition = "TEXT")
    private String details; // JSON or descriptive string of the action

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
