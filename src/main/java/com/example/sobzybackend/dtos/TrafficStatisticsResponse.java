package com.example.sobzybackend.dtos;

import java.util.Map;

public class TrafficStatisticsResponse {
    private long totalRequests;
    private long blockedRequests;
    private long allowedRequests;
    private double blockRate;
    private long activeDevices;
    private long activeUsers;
    private long totalBandwidth;
    private Map<String, Long> trafficByCategory;
    private Map<String, Long> topDomains;

    public TrafficStatisticsResponse() {}

    public TrafficStatisticsResponse(long totalRequests, long blockedRequests, long allowedRequests, double blockRate, long activeDevices, long activeUsers, long totalBandwidth, Map<String, Long> trafficByCategory, Map<String, Long> topDomains) {
        this.totalRequests = totalRequests;
        this.blockedRequests = blockedRequests;
        this.allowedRequests = allowedRequests;
        this.blockRate = blockRate;
        this.activeDevices = activeDevices;
        this.activeUsers = activeUsers;
        this.totalBandwidth = totalBandwidth;
        this.trafficByCategory = trafficByCategory;
        this.topDomains = topDomains;
    }

    public long getTotalRequests() { return totalRequests; }
    public void setTotalRequests(long totalRequests) { this.totalRequests = totalRequests; }
    public long getBlockedRequests() { return blockedRequests; }
    public void setBlockedRequests(long blockedRequests) { this.blockedRequests = blockedRequests; }
    public long getAllowedRequests() { return allowedRequests; }
    public void setAllowedRequests(long allowedRequests) { this.allowedRequests = allowedRequests; }
    public double getBlockRate() { return blockRate; }
    public void setBlockRate(double blockRate) { this.blockRate = blockRate; }
    public long getActiveDevices() { return activeDevices; }
    public void setActiveDevices(long activeDevices) { this.activeDevices = activeDevices; }
    public long getActiveUsers() { return activeUsers; }
    public void setActiveUsers(long activeUsers) { this.activeUsers = activeUsers; }
    public long getTotalBandwidth() { return totalBandwidth; }
    public void setTotalBandwidth(long totalBandwidth) { this.totalBandwidth = totalBandwidth; }
    public Map<String, Long> getTrafficByCategory() { return trafficByCategory; }
    public void setTrafficByCategory(Map<String, Long> trafficByCategory) { this.trafficByCategory = trafficByCategory; }
    public Map<String, Long> getTopDomains() { return topDomains; }
    public void setTopDomains(Map<String, Long> topDomains) { this.topDomains = topDomains; }

    public static TrafficStatisticsResponseBuilder builder() {
        return new TrafficStatisticsResponseBuilder();
    }

    public static class TrafficStatisticsResponseBuilder {
        private long totalRequests;
        private long blockedRequests;
        private long allowedRequests;
        private double blockRate;
        private long activeDevices;
        private long activeUsers;
        private long totalBandwidth;
        private Map<String, Long> trafficByCategory;
        private Map<String, Long> topDomains;

        public TrafficStatisticsResponseBuilder totalRequests(long totalRequests) { this.totalRequests = totalRequests; return this; }
        public TrafficStatisticsResponseBuilder blockedRequests(long blockedRequests) { this.blockedRequests = blockedRequests; return this; }
        public TrafficStatisticsResponseBuilder allowedRequests(long allowedRequests) { this.allowedRequests = allowedRequests; return this; }
        public TrafficStatisticsResponseBuilder blockRate(double blockRate) { this.blockRate = blockRate; return this; }
        public TrafficStatisticsResponseBuilder activeDevices(long activeDevices) { this.activeDevices = activeDevices; return this; }
        public TrafficStatisticsResponseBuilder activeUsers(long activeUsers) { this.activeUsers = activeUsers; return this; }
        public TrafficStatisticsResponseBuilder totalBandwidth(long totalBandwidth) { this.totalBandwidth = totalBandwidth; return this; }
        public TrafficStatisticsResponseBuilder trafficByCategory(Map<String, Long> trafficByCategory) { this.trafficByCategory = trafficByCategory; return this; }
        public TrafficStatisticsResponseBuilder topDomains(Map<String, Long> topDomains) { this.topDomains = topDomains; return this; }

        public TrafficStatisticsResponse build() {
            return new TrafficStatisticsResponse(totalRequests, blockedRequests, allowedRequests, blockRate, activeDevices, activeUsers, totalBandwidth, trafficByCategory, topDomains);
        }
    }
}
