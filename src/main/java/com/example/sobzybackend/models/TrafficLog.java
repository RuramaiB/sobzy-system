package com.example.sobzybackend.models;

import com.example.sobzybackend.users.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "traffic_logs", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_device_id", columnList = "device_id"),
        @Index(name = "idx_domain", columnList = "domain"),
        @Index(name = "idx_timestamp", columnList = "request_timestamp"),
        @Index(name = "idx_is_blocked", columnList = "is_blocked")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrafficLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(nullable = false, length = 255)
    private String domain;

    @Column(length = 10)
    private String method;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "request_size")
    @Builder.Default
    private Long requestSize = 0L;

    @Column(name = "response_size")
    @Builder.Default
    private Long responseSize = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "classification_confidence", precision = 5, scale = 4)
    private BigDecimal classificationConfidence;

    @Column(name = "is_blocked")
    @Builder.Default
    private Boolean isBlocked = false;

    @Column(name = "block_reason", length = 255)
    private String blockReason;

    @Column(name = "request_timestamp")
    private LocalDateTime requestTimestamp;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(columnDefinition = "TEXT")
    private String referer;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (requestTimestamp == null) {
            requestTimestamp = LocalDateTime.now();
        }
    }

    public Long getTotalSize() {
        return requestSize + responseSize;
    }

    public boolean isHighConfidence() {
        return classificationConfidence != null &&
                classificationConfidence.compareTo(new BigDecimal("0.80")) >= 0;
    }
}
