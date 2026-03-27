package com.example.sobzybackend.models;

import com.example.sobzybackend.enums.DeviceStatus;
import com.example.sobzybackend.users.User;
import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "devices")
public class Device {

    public Device() {}

    public Device(Long id, User user, String deviceName, String macAddress, String ipAddress, String deviceType, String osInfo, String browserInfo, DeviceStatus status, LocalDateTime firstSeen, LocalDateTime lastSeen, Long totalBandwidthUsed, LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.deviceName = deviceName;
        this.macAddress = macAddress;
        this.ipAddress = ipAddress;
        this.deviceType = deviceType;
        this.osInfo = osInfo;
        this.browserInfo = browserInfo;
        this.status = status;
        this.firstSeen = firstSeen;
        this.lastSeen = lastSeen;
        this.totalBandwidthUsed = totalBandwidthUsed;
        this.createdAt = createdAt;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Column(name = "mac_address", unique = true, length = 17)
    private String macAddress;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(name = "os_info", length = 100)
    private String osInfo;

    @Column(name = "browser_info", length = 100)
    private String browserInfo;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_status", nullable = false, length = 100)
    private DeviceStatus status = DeviceStatus.PENDING_AUTH;

    @Column(name = "is_authenticated")
    private boolean isAuthenticated = false;

    @Column(name = "first_seen")
    private LocalDateTime firstSeen;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(name = "total_bandwidth_used")
    private Long totalBandwidthUsed = 0L;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (firstSeen == null) {
            firstSeen = LocalDateTime.now();
        }
        if (lastSeen == null) {
            lastSeen = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
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
    public DeviceStatus getStatus() { return status; }
    public void setStatus(DeviceStatus status) { this.status = status; }
    public boolean isAuthenticated() { return isAuthenticated; }
    public void setAuthenticated(boolean authenticated) { isAuthenticated = authenticated; }
    public LocalDateTime getFirstSeen() { return firstSeen; }
    public void setFirstSeen(LocalDateTime firstSeen) { this.firstSeen = firstSeen; }
    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }
    public Long getTotalBandwidthUsed() { return totalBandwidthUsed; }
    public void setTotalBandwidthUsed(Long totalBandwidthUsed) { this.totalBandwidthUsed = totalBandwidthUsed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public void updateBandwidth(Long bytes) {
        if (this.totalBandwidthUsed == null) this.totalBandwidthUsed = 0L;
        this.totalBandwidthUsed += bytes;
    }

    public static DeviceBuilder builder() {
        return new DeviceBuilder();
    }

    public static class DeviceBuilder {
        private Long id;
        private User user;
        private String deviceName;
        private String macAddress;
        private String ipAddress;
        private String deviceType;
        private String osInfo;
        private String browserInfo;
        private DeviceStatus status = DeviceStatus.ACTIVE;
        private LocalDateTime firstSeen;
        private LocalDateTime lastSeen;
        private Long totalBandwidthUsed = 0L;
        private LocalDateTime createdAt;

        public DeviceBuilder id(Long id) { this.id = id; return this; }
        public DeviceBuilder user(User user) { this.user = user; return this; }
        public DeviceBuilder deviceName(String deviceName) { this.deviceName = deviceName; return this; }
        public DeviceBuilder macAddress(String macAddress) { this.macAddress = macAddress; return this; }
        public DeviceBuilder ipAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }
        public DeviceBuilder deviceType(String deviceType) { this.deviceType = deviceType; return this; }
        public DeviceBuilder osInfo(String osInfo) { this.osInfo = osInfo; return this; }
        public DeviceBuilder browserInfo(String browserInfo) { this.browserInfo = browserInfo; return this; }
        public DeviceBuilder status(DeviceStatus status) { this.status = status; return this; }
        public DeviceBuilder firstSeen(LocalDateTime firstSeen) { this.firstSeen = firstSeen; return this; }
        public DeviceBuilder lastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; return this; }
        public DeviceBuilder totalBandwidthUsed(Long totalBandwidthUsed) { this.totalBandwidthUsed = totalBandwidthUsed; return this; }
        public DeviceBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Device build() {
            return new Device(id, user, deviceName, macAddress, ipAddress, deviceType, osInfo, browserInfo, status, firstSeen, lastSeen, totalBandwidthUsed, createdAt);
        }
    }
}
