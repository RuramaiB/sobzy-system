package com.example.sobzybackend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrafficAnalysisResponse {

    private String decision; // ALLOW, BLOCK, THROTTLE
    private String category;
    private Double confidence;
    private String reason;
    private String policyApplied;
    private Long processingTimeMs;
    private Boolean success;
}
