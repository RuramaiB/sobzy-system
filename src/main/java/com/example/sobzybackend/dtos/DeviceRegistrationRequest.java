package com.example.sobzybackend.dtos;

public class DeviceRegistrationRequest {
    private Long userId;
    private String macAddress;
    private String ipAddress;
    private String deviceName;
    private String deviceType;
    private String osInfo;
    private String browserInfo;

    public DeviceRegistrationRequest() {}

    public DeviceRegistrationRequest(Long userId, String macAddress, String ipAddress, String deviceName, String deviceType, String osInfo, String browserInfo) {
        this.userId = userId;
        this.macAddress = macAddress;
        this.ipAddress = ipAddress;
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.osInfo = osInfo;
        this.browserInfo = browserInfo;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    public String getOsInfo() { return osInfo; }
    public void setOsInfo(String osInfo) { this.osInfo = osInfo; }
    public String getBrowserInfo() { return browserInfo; }
    public void setBrowserInfo(String browserInfo) { this.browserInfo = browserInfo; }

    public static DeviceRegistrationRequestBuilder builder() {
        return new DeviceRegistrationRequestBuilder();
    }

    public static class DeviceRegistrationRequestBuilder {
        private Long userId;
        private String macAddress;
        private String ipAddress;
        private String deviceName;
        private String deviceType;
        private String osInfo;
        private String browserInfo;

        public DeviceRegistrationRequestBuilder userId(Long userId) { this.userId = userId; return this; }
        public DeviceRegistrationRequestBuilder macAddress(String macAddress) { this.macAddress = macAddress; return this; }
        public DeviceRegistrationRequestBuilder ipAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }
        public DeviceRegistrationRequestBuilder deviceName(String deviceName) { this.deviceName = deviceName; return this; }
        public DeviceRegistrationRequestBuilder deviceType(String deviceType) { this.deviceType = deviceType; return this; }
        public DeviceRegistrationRequestBuilder osInfo(String osInfo) { this.osInfo = osInfo; return this; }
        public DeviceRegistrationRequestBuilder browserInfo(String browserInfo) { this.browserInfo = browserInfo; return this; }

        public DeviceRegistrationRequest build() {
            return new DeviceRegistrationRequest(userId, macAddress, ipAddress, deviceName, deviceType, osInfo, browserInfo);
        }
    }
}
