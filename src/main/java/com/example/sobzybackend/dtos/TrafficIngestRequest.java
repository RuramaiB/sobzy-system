package com.example.sobzybackend.dtos;

import java.util.Map;

public class TrafficIngestRequest {
    private String clientIp;
    private String host;
    private String url;
    private String method;
    private Map<String, String> requestHeaders;
    private String requestBody;
    private Integer responseCode;
    private Map<String, String> responseHeaders;
    private String responseBody;
    private String timestamp;

    public TrafficIngestRequest() {}

    public TrafficIngestRequest(String clientIp, String host, String url, String method, Map<String, String> requestHeaders, String requestBody, Integer responseCode, Map<String, String> responseHeaders, String responseBody, String timestamp) {
        this.clientIp = clientIp;
        this.host = host;
        this.url = url;
        this.method = method;
        this.requestHeaders = requestHeaders;
        this.requestBody = requestBody;
        this.responseCode = responseCode;
        this.responseHeaders = responseHeaders;
        this.responseBody = responseBody;
        this.timestamp = timestamp;
    }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public Map<String, String> getRequestHeaders() { return requestHeaders; }
    public void setRequestHeaders(Map<String, String> requestHeaders) { this.requestHeaders = requestHeaders; }
    public String getRequestBody() { return requestBody; }
    public void setRequestBody(String requestBody) { this.requestBody = requestBody; }
    public Integer getResponseCode() { return responseCode; }
    public void setResponseCode(Integer responseCode) { this.responseCode = responseCode; }
    public Map<String, String> getResponseHeaders() { return responseHeaders; }
    public void setResponseHeaders(Map<String, String> responseHeaders) { this.responseHeaders = responseHeaders; }
    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public static TrafficIngestRequestBuilder builder() {
        return new TrafficIngestRequestBuilder();
    }

    public static class TrafficIngestRequestBuilder {
        private String clientIp, host, url, method, requestBody, timestamp;
        private Map<String, String> requestHeaders, responseHeaders;
        private Integer responseCode;
        private String responseBody;

        public TrafficIngestRequestBuilder clientIp(String clientIp) { this.clientIp = clientIp; return this; }
        public TrafficIngestRequestBuilder host(String host) { this.host = host; return this; }
        public TrafficIngestRequestBuilder url(String url) { this.url = url; return this; }
        public TrafficIngestRequestBuilder method(String method) { this.method = method; return this; }
        public TrafficIngestRequestBuilder requestHeaders(Map<String, String> requestHeaders) { this.requestHeaders = requestHeaders; return this; }
        public TrafficIngestRequestBuilder requestBody(String requestBody) { this.requestBody = requestBody; return this; }
        public TrafficIngestRequestBuilder responseCode(Integer responseCode) { this.responseCode = responseCode; return this; }
        public TrafficIngestRequestBuilder responseHeaders(Map<String, String> responseHeaders) { this.responseHeaders = responseHeaders; return this; }
        public TrafficIngestRequestBuilder responseBody(String responseBody) { this.responseBody = responseBody; return this; }
        public TrafficIngestRequestBuilder timestamp(String timestamp) { this.timestamp = timestamp; return this; }

        public TrafficIngestRequest build() {
            return new TrafficIngestRequest(clientIp, host, url, method, requestHeaders, requestBody, responseCode, responseHeaders, responseBody, timestamp);
        }
    }
}
