package com.example.sobzybackend.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRegistrationRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "MAC address is required")
    private String macAddress;

    @NotBlank(message = "IP address is required")
    private String ipAddress;

    private String deviceName;

    private String deviceType;

    private String osInfo;

    private String browserInfo;
}
