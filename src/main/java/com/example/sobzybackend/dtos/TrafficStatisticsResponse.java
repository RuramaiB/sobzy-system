package com.example.sobzybackend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrafficStatisticsResponse {

    private Long totalRequests;
    private Long blockedRequests;
    private Long allowedRequests;
    private Double blockRate;
    private Long totalBandwidth;
    private Long activeDevices;
    private Long activeUsers;
    private Map<String, Long> trafficByCategory;
    private Map<String, Long> topDomains;
}
