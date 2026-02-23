package com.example.sobzybackend.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassificationResult {

    private String url;
    private String domain;
    private String category;
    private Double confidence;

    @JsonProperty("is_allowed")
    private Boolean isAllowed;

    @JsonProperty("risk_level")
    private String riskLevel;

    private String decision;  // ALLOW, BLOCK, THROTTLE
    private String reason;

    @JsonProperty("processing_time_ms")
    private Long processingTimeMs;

    private Boolean error = false;
}
