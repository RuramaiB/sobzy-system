package com.example.sobzybackend.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TrafficAnalysisRequest {

    @NotBlank(message = "URL is required")
    private String url;

    @NotNull(message = "User ID is required")
    private Long userId;

    private String method = "GET";

    @NotBlank(message = "IP address is required")
    private String ipAddress;

    @NotBlank(message = "MAC address is required")
    private String macAddress;

    private String userAgent;

    private String referer;

    private Long requestSize = 0L;

    private Long responseSize = 0L;

    private Integer statusCode;

    public TrafficAnalysisRequest() {}

    public TrafficAnalysisRequest(String url, Long userId, String method, String ipAddress, String macAddress, String userAgent, String referer, Long requestSize, Long responseSize, Integer statusCode) {
        this.url = url;
        this.userId = userId;
        this.method = method;
        this.ipAddress = ipAddress;
        this.macAddress = macAddress;
        this.userAgent = userAgent;
        this.referer = referer;
        this.requestSize = requestSize;
        this.responseSize = responseSize;
        this.statusCode = statusCode;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getReferer() { return referer; }
    public void setReferer(String referer) { this.referer = referer; }
    public Long getRequestSize() { return requestSize; }
    public void setRequestSize(Long requestSize) { this.requestSize = requestSize; }
    public Long getResponseSize() { return responseSize; }
    public void setResponseSize(Long responseSize) { this.responseSize = responseSize; }
    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }

    public static TrafficAnalysisRequestBuilder builder() {
        return new TrafficAnalysisRequestBuilder();
    }

    public static class TrafficAnalysisRequestBuilder {
        private String url;
        private Long userId;
        private String method = "GET";
        private String ipAddress;
        private String macAddress;
        private String userAgent;
        private String referer;
        private Long requestSize = 0L;
        private Long responseSize = 0L;
        private Integer statusCode;

        public TrafficAnalysisRequestBuilder url(String url) { this.url = url; return this; }
        public TrafficAnalysisRequestBuilder userId(Long userId) { this.userId = userId; return this; }
        public TrafficAnalysisRequestBuilder method(String method) { this.method = method; return this; }
        public TrafficAnalysisRequestBuilder ipAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }
        public TrafficAnalysisRequestBuilder macAddress(String macAddress) { this.macAddress = macAddress; return this; }
        public TrafficAnalysisRequestBuilder userAgent(String userAgent) { this.userAgent = userAgent; return this; }
        public TrafficAnalysisRequestBuilder referer(String referer) { this.referer = referer; return this; }
        public TrafficAnalysisRequestBuilder requestSize(Long requestSize) { this.requestSize = requestSize; return this; }
        public TrafficAnalysisRequestBuilder responseSize(Long responseSize) { this.responseSize = responseSize; return this; }
        public TrafficAnalysisRequestBuilder statusCode(Integer statusCode) { this.statusCode = statusCode; return this; }

        public TrafficAnalysisRequest build() {
            return new TrafficAnalysisRequest(url, userId, method, ipAddress, macAddress, userAgent, referer, requestSize, responseSize, statusCode);
        }
    }
}
