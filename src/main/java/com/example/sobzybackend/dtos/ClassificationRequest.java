package com.example.sobzybackend.dtos;

public class ClassificationRequest {
    private String url;
    private String content;
    private String method;
    private String ipAddress;
    private String userAgent;
    private String referer;
    private String deviceId;

    public ClassificationRequest() {}

    public ClassificationRequest(String url, String content, String method, String ipAddress, String userAgent, String referer, String deviceId) {
        this.url = url;
        this.content = content;
        this.method = method;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.referer = referer;
        this.deviceId = deviceId;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getReferer() { return referer; }
    public void setReferer(String referer) { this.referer = referer; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public static ClassificationRequestBuilder builder() {
        return new ClassificationRequestBuilder();
    }

    public static class ClassificationRequestBuilder {
        private String url;
        private String content;
        private String method;
        private String ipAddress;
        private String userAgent;
        private String referer;
        private String deviceId;

        public ClassificationRequestBuilder url(String url) { this.url = url; return this; }
        public ClassificationRequestBuilder content(String content) { this.content = content; return this; }
        public ClassificationRequestBuilder method(String method) { this.method = method; return this; }
        public ClassificationRequestBuilder ipAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }
        public ClassificationRequestBuilder userAgent(String userAgent) { this.userAgent = userAgent; return this; }
        public ClassificationRequestBuilder referer(String referer) { this.referer = referer; return this; }
        public ClassificationRequestBuilder deviceId(String deviceId) { this.deviceId = deviceId; return this; }

        public ClassificationRequest build() {
            return new ClassificationRequest(url, content, method, ipAddress, userAgent, referer, deviceId);
        }
    }
}
