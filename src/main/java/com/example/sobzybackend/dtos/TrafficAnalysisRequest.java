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
}

