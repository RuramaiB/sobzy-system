package com.example.sobzybackend.models;

import com.example.sobzybackend.enums.DeviceStatus;
import com.example.sobzybackend.users.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "devices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Device {

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
    @Column(nullable = false)
    @Builder.Default
    private DeviceStatus status = DeviceStatus.ACTIVE;

    @Column(name = "first_seen")
    private LocalDateTime firstSeen;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(name = "total_bandwidth_used")
    @Builder.Default
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

    public void updateBandwidth(Long bytes) {
        this.totalBandwidthUsed += bytes;
    }
}
