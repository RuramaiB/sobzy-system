package com.example.sobzybackend.dtos;

import java.time.LocalDateTime;

public class TrafficLogResponse {
    private Long id;
    private Long userId;
    private String username;
    private Long deviceId;
    private String deviceName;
    private String url;
    private String domain;
    private String method;
    private Integer statusCode;
    private Long requestSize;
    private Long responseSize;
    private Long totalSize;
    private String category;
    private Double confidence;
    private Boolean isBlocked;
    private String blockReason;
    private Integer responseTimeMs;
    private LocalDateTime requestTimestamp;
    private String clientIp;
    private String ipAddress; // Entity ip_address
    private String userAgent;
    private String referer;

    public TrafficLogResponse() {}

    public TrafficLogResponse(Long id, Long userId, String username, Long deviceId, String deviceName, String url, String domain, String method, Integer statusCode, Long requestSize, Long responseSize, Long totalSize, String category, Double confidence, Boolean isBlocked, String blockReason, Integer responseTimeMs, LocalDateTime requestTimestamp, String clientIp, String ipAddress, String userAgent, String referer) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.url = url;
        this.domain = domain;
        this.method = method;
        this.statusCode = statusCode;
        this.requestSize = requestSize;
        this.responseSize = responseSize;
        this.totalSize = totalSize;
        this.category = category;
        this.confidence = confidence;
        this.isBlocked = isBlocked;
        this.blockReason = blockReason;
        this.responseTimeMs = responseTimeMs;
        this.requestTimestamp = requestTimestamp;
        this.clientIp = clientIp;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.referer = referer;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
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
    public Long getTotalSize() { return totalSize; }
    public void setTotalSize(Long totalSize) { this.totalSize = totalSize; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public Boolean getIsBlocked() { return isBlocked; }
    public void setIsBlocked(Boolean isBlocked) { this.isBlocked = isBlocked; }
    public String getBlockReason() { return blockReason; }
    public void setBlockReason(String blockReason) { this.blockReason = blockReason; }
    public Integer getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(Integer responseTimeMs) { this.responseTimeMs = responseTimeMs; }
    public LocalDateTime getRequestTimestamp() { return requestTimestamp; }
    public void setRequestTimestamp(LocalDateTime requestTimestamp) { this.requestTimestamp = requestTimestamp; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getReferer() { return referer; }
    public void setReferer(String referer) { this.referer = referer; }

    public static TrafficLogResponseBuilder builder() {
        return new TrafficLogResponseBuilder();
    }

    public static class TrafficLogResponseBuilder {
        private Long id;
        private Long userId;
        private String username;
        private Long deviceId;
        private String deviceName;
        private String url;
        private String domain;
        private String method;
        private Integer statusCode;
        private Long requestSize;
        private Long responseSize;
        private Long totalSize;
        private String category;
        private Double confidence;
        private Boolean isBlocked;
        private String blockReason;
        private Integer responseTimeMs;
        private LocalDateTime requestTimestamp;
        private String clientIp;
        private String ipAddress;
        private String userAgent;
        private String referer;

        public TrafficLogResponseBuilder id(Long id) { this.id = id; return this; }
        public TrafficLogResponseBuilder userId(Long userId) { this.userId = userId; return this; }
        public TrafficLogResponseBuilder username(String username) { this.username = username; return this; }
        public TrafficLogResponseBuilder deviceId(Long deviceId) { this.deviceId = deviceId; return this; }
        public TrafficLogResponseBuilder deviceName(String deviceName) { this.deviceName = deviceName; return this; }
        public TrafficLogResponseBuilder url(String url) { this.url = url; return this; }
        public TrafficLogResponseBuilder domain(String domain) { this.domain = domain; return this; }
        public TrafficLogResponseBuilder method(String method) { this.method = method; return this; }
        public TrafficLogResponseBuilder statusCode(Integer statusCode) { this.statusCode = statusCode; return this; }
        public TrafficLogResponseBuilder requestSize(Long requestSize) { this.requestSize = requestSize; return this; }
        public TrafficLogResponseBuilder responseSize(Long responseSize) { this.responseSize = responseSize; return this; }
        public TrafficLogResponseBuilder totalSize(Long totalSize) { this.totalSize = totalSize; return this; }
        public TrafficLogResponseBuilder category(String category) { this.category = category; return this; }
        public TrafficLogResponseBuilder confidence(Double confidence) { this.confidence = confidence; return this; }
        public TrafficLogResponseBuilder isBlocked(Boolean isBlocked) { this.isBlocked = isBlocked; return this; }
        public TrafficLogResponseBuilder blockReason(String blockReason) { this.blockReason = blockReason; return this; }
        public TrafficLogResponseBuilder responseTimeMs(Integer responseTimeMs) { this.responseTimeMs = responseTimeMs; return this; }
        public TrafficLogResponseBuilder requestTimestamp(LocalDateTime requestTimestamp) { this.requestTimestamp = requestTimestamp; return this; }
        public TrafficLogResponseBuilder clientIp(String clientIp) { this.clientIp = clientIp; return this; }
        public TrafficLogResponseBuilder ipAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }
        public TrafficLogResponseBuilder userAgent(String userAgent) { this.userAgent = userAgent; return this; }
        public TrafficLogResponseBuilder referer(String referer) { this.referer = referer; return this; }

        public TrafficLogResponse build() {
            return new TrafficLogResponse(id, userId, username, deviceId, deviceName, url, domain, method, statusCode, requestSize, responseSize, totalSize, category, confidence, isBlocked, blockReason, responseTimeMs, requestTimestamp, clientIp, ipAddress, userAgent, referer);
        }
    }
}
