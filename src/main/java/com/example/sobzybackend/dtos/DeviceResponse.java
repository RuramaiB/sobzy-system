package com.example.sobzybackend.dtos;

import java.time.LocalDateTime;

public class DeviceResponse {
    private Long id;
    private Long userId;
    private String username;
    private String deviceName;
    private String macAddress;
    private String ipAddress;
    private String deviceType;
    private String osInfo;
    private String browserInfo;
    private String status;
    private Long totalBandwidthUsed;
    private LocalDateTime firstSeen;
    private LocalDateTime lastSeen;

    public DeviceResponse() {}

    public DeviceResponse(Long id, Long userId, String username, String deviceName, String macAddress, String ipAddress, String deviceType, String osInfo, String browserInfo, String status, Long totalBandwidthUsed, LocalDateTime firstSeen, LocalDateTime lastSeen) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.deviceName = deviceName;
        this.macAddress = macAddress;
        this.ipAddress = ipAddress;
        this.deviceType = deviceType;
        this.osInfo = osInfo;
        this.browserInfo = browserInfo;
        this.status = status;
        this.totalBandwidthUsed = totalBandwidthUsed;
        this.firstSeen = firstSeen;
        this.lastSeen = lastSeen;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    public String getOsInfo() { return osInfo; }
    public void setOsInfo(String osInfo) { this.osInfo = osInfo; }
    public String getBrowserInfo() { return browserInfo; }
    public void setBrowserInfo(String browserInfo) { this.browserInfo = browserInfo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getTotalBandwidthUsed() { return totalBandwidthUsed; }
    public void setTotalBandwidthUsed(Long totalBandwidthUsed) { this.totalBandwidthUsed = totalBandwidthUsed; }
    public LocalDateTime getFirstSeen() { return firstSeen; }
    public void setFirstSeen(LocalDateTime firstSeen) { this.firstSeen = firstSeen; }
    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }

    public static DeviceResponseBuilder builder() {
        return new DeviceResponseBuilder();
    }

    public static class DeviceResponseBuilder {
        private Long id;
        private Long userId;
        private String username;
        private String deviceName;
        private String macAddress;
        private String ipAddress;
        private String deviceType;
        private String osInfo;
        private String browserInfo;
        private String status;
        private Long totalBandwidthUsed;
        private LocalDateTime firstSeen;
        private LocalDateTime lastSeen;

        public DeviceResponseBuilder id(Long id) { this.id = id; return this; }
        public DeviceResponseBuilder userId(Long userId) { this.userId = userId; return this; }
        public DeviceResponseBuilder username(String username) { this.username = username; return this; }
        public DeviceResponseBuilder deviceName(String deviceName) { this.deviceName = deviceName; return this; }
        public DeviceResponseBuilder macAddress(String macAddress) { this.macAddress = macAddress; return this; }
        public DeviceResponseBuilder ipAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }
        public DeviceResponseBuilder deviceType(String deviceType) { this.deviceType = deviceType; return this; }
        public DeviceResponseBuilder osInfo(String osInfo) { this.osInfo = osInfo; return this; }
        public DeviceResponseBuilder browserInfo(String browserInfo) { this.browserInfo = browserInfo; return this; }
        public DeviceResponseBuilder status(String status) { this.status = status; return this; }
        public DeviceResponseBuilder totalBandwidthUsed(Long totalBandwidthUsed) { this.totalBandwidthUsed = totalBandwidthUsed; return this; }
        public DeviceResponseBuilder firstSeen(LocalDateTime firstSeen) { this.firstSeen = firstSeen; return this; }
        public DeviceResponseBuilder lastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; return this; }

        public DeviceResponse build() {
            return new DeviceResponse(id, userId, username, deviceName, macAddress, ipAddress, deviceType, osInfo, browserInfo, status, totalBandwidthUsed, firstSeen, lastSeen);
        }
    }
}
