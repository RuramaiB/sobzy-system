package com.example.sobzybackend.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

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

    public ClassificationResult() {}

    public ClassificationResult(String url, String domain, String category, Double confidence, Boolean isAllowed, String riskLevel, String decision, String reason, Long processingTimeMs, Boolean error) {
        this.url = url;
        this.domain = domain;
        this.category = category;
        this.confidence = confidence;
        this.isAllowed = isAllowed;
        this.riskLevel = riskLevel;
        this.decision = decision;
        this.reason = reason;
        this.processingTimeMs = processingTimeMs;
        this.error = error;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public Boolean getIsAllowed() { return isAllowed; }
    public void setIsAllowed(Boolean isAllowed) { this.isAllowed = isAllowed; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    public Boolean getError() { return error; }
    public void setError(Boolean error) { this.error = error; }

    public static ClassificationResultBuilder builder() {
        return new ClassificationResultBuilder();
    }

    public static class ClassificationResultBuilder {
        private String url;
        private String domain;
        private String category;
        private Double confidence;
        private Boolean isAllowed;
        private String riskLevel;
        private String decision;
        private String reason;
        private Long processingTimeMs;
        private Boolean error = false;

        public ClassificationResultBuilder url(String url) { this.url = url; return this; }
        public ClassificationResultBuilder domain(String domain) { this.domain = domain; return this; }
        public ClassificationResultBuilder category(String category) { this.category = category; return this; }
        public ClassificationResultBuilder confidence(Double confidence) { this.confidence = confidence; return this; }
        public ClassificationResultBuilder isAllowed(Boolean isAllowed) { this.isAllowed = isAllowed; return this; }
        public ClassificationResultBuilder riskLevel(String riskLevel) { this.riskLevel = riskLevel; return this; }
        public ClassificationResultBuilder decision(String decision) { this.decision = decision; return this; }
        public ClassificationResultBuilder reason(String reason) { this.reason = reason; return this; }
        public ClassificationResultBuilder processingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; return this; }
        public ClassificationResultBuilder error(Boolean error) { this.error = error; return this; }

        public ClassificationResult build() {
            return new ClassificationResult(url, domain, category, confidence, isAllowed, riskLevel, decision, reason, processingTimeMs, error);
        }
    }
}
