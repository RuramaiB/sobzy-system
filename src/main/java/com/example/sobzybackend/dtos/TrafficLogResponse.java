package com.example.sobzybackend.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrafficLogResponse {

    private Long id;
    private Long userId;
    private String username;
    private Long deviceId;
    private String deviceName;
    private String url;
    private String domain;
    private String method;
    private Integer statusCode;
    private Long requestSize;
    private Long responseSize;
    private Long totalSize;
    private String category;
    private BigDecimal confidence;
    private Boolean isBlocked;
    private String blockReason;
    private Integer responseTimeMs;
    private String ipAddress;
    private String userAgent;
    private String referer;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime requestTimestamp;
}
