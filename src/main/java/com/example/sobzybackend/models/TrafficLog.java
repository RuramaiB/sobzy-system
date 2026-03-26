package com.example.sobzybackend.models;

import com.example.sobzybackend.users.User;
import jakarta.persistence.*;
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
public class TrafficLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = true)
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
    private Long requestSize = 0L;

    @Column(name = "response_size")
    private Long responseSize = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "classification_confidence", precision = 5, scale = 4)
    private BigDecimal classificationConfidence;

    @Column(name = "is_blocked")
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

    public TrafficLog() {}

    public TrafficLog(Long id, User user, Device device, String url, String domain, String method, Integer statusCode, Long requestSize, Long responseSize, Category category, BigDecimal classificationConfidence, Boolean isBlocked, String blockReason, LocalDateTime requestTimestamp, Integer responseTimeMs, String userAgent, String referer, String ipAddress, LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.device = device;
        this.url = url;
        this.domain = domain;
        this.method = method;
        this.statusCode = statusCode;
        this.requestSize = requestSize;
        this.responseSize = responseSize;
        this.category = category;
        this.classificationConfidence = classificationConfidence;
        this.isBlocked = isBlocked;
        this.blockReason = blockReason;
        this.requestTimestamp = requestTimestamp;
        this.responseTimeMs = responseTimeMs;
        this.userAgent = userAgent;
        this.referer = referer;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Device getDevice() { return device; }
    public void setDevice(Device device) { this.device = device; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }
    public Long getRequestSize() { return requestSize; }
    public void setRequestSize(Long requestSize) { this.requestSize = requestSize; }
    public Long getResponseSize() { return responseSize; }
    public void setResponseSize(Long responseSize) { this.responseSize = responseSize; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public BigDecimal getClassificationConfidence() { return classificationConfidence; }
    public void setClassificationConfidence(BigDecimal classificationConfidence) { this.classificationConfidence = classificationConfidence; }
    public Boolean getIsBlocked() { return isBlocked; }
    public void setIsBlocked(Boolean isBlocked) { this.isBlocked = isBlocked; }
    public String getBlockReason() { return blockReason; }
    public void setBlockReason(String blockReason) { this.blockReason = blockReason; }
    public LocalDateTime getRequestTimestamp() { return requestTimestamp; }
    public void setRequestTimestamp(LocalDateTime requestTimestamp) { this.requestTimestamp = requestTimestamp; }
    public Integer getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(Integer responseTimeMs) { this.responseTimeMs = responseTimeMs; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getReferer() { return referer; }
    public void setReferer(String referer) { this.referer = referer; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getTotalSize() {
        return (requestSize != null ? requestSize : 0L) + (responseSize != null ? responseSize : 0L);
    }

    public static TrafficLogBuilder builder() {
        return new TrafficLogBuilder();
    }

    public static class TrafficLogBuilder {
        private Long id;
        private User user;
        private Device device;
        private String url;
        private String domain;
        private String method;
        private Integer statusCode;
        private Long requestSize = 0L;
        private Long responseSize = 0L;
        private Category category;
        private BigDecimal classificationConfidence;
        private Boolean isBlocked = false;
        private String blockReason;
        private LocalDateTime requestTimestamp;
        private Integer responseTimeMs;
        private String userAgent;
        private String referer;
        private String ipAddress;
        private LocalDateTime createdAt;

        public TrafficLogBuilder id(Long id) { this.id = id; return this; }
        public TrafficLogBuilder user(User user) { this.user = user; return this; }
        public TrafficLogBuilder device(Device device) { this.device = device; return this; }
        public TrafficLogBuilder url(String url) { this.url = url; return this; }
        public TrafficLogBuilder domain(String domain) { this.domain = domain; return this; }
        public TrafficLogBuilder method(String method) { this.method = method; return this; }
        public TrafficLogBuilder statusCode(Integer statusCode) { this.statusCode = statusCode; return this; }
        public TrafficLogBuilder requestSize(Long requestSize) { this.requestSize = requestSize; return this; }
        public TrafficLogBuilder responseSize(Long responseSize) { this.responseSize = responseSize; return this; }
        public TrafficLogBuilder category(Category category) { this.category = category; return this; }
        public TrafficLogBuilder classificationConfidence(BigDecimal classificationConfidence) { this.classificationConfidence = classificationConfidence; return this; }
        public TrafficLogBuilder isBlocked(Boolean isBlocked) { this.isBlocked = isBlocked; return this; }
        public TrafficLogBuilder blockReason(String blockReason) { this.blockReason = blockReason; return this; }
        public TrafficLogBuilder requestTimestamp(LocalDateTime requestTimestamp) { this.requestTimestamp = requestTimestamp; return this; }
        public TrafficLogBuilder responseTimeMs(Integer responseTimeMs) { this.responseTimeMs = responseTimeMs; return this; }
        public TrafficLogBuilder userAgent(String userAgent) { this.userAgent = userAgent; return this; }
        public TrafficLogBuilder referer(String referer) { this.referer = referer; return this; }
        public TrafficLogBuilder ipAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }
        public TrafficLogBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public TrafficLog build() {
            return new TrafficLog(id, user, device, url, domain, method, statusCode, requestSize, responseSize, category, classificationConfidence, isBlocked, blockReason, requestTimestamp, responseTimeMs, userAgent, referer, ipAddress, createdAt);
        }
    }
}
