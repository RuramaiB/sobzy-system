package com.example.sobzybackend.dtos;

import java.util.List;

public class DecisionResponse {
    private String decision; // ALLOW, BLOCK
    private String reason;
    private String category;
    private Double confidence;
    private List<String> updatedDenyList;

    public DecisionResponse() {}

    public DecisionResponse(String decision, String reason, String category, Double confidence, List<String> updatedDenyList) {
        this.decision = decision;
        this.reason = reason;
        this.category = category;
        this.confidence = confidence;
        this.updatedDenyList = updatedDenyList;
    }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public List<String> getUpdatedDenyList() { return updatedDenyList; }
    public void setUpdatedDenyList(List<String> updatedDenyList) { this.updatedDenyList = updatedDenyList; }

    public static DecisionResponseBuilder builder() {
        return new DecisionResponseBuilder();
    }

    public static class DecisionResponseBuilder {
        private String decision;
        private String reason;
        private String category;
        private Double confidence;
        private List<String> updatedDenyList;

        public DecisionResponseBuilder decision(String decision) { this.decision = decision; return this; }
        public DecisionResponseBuilder reason(String reason) { this.reason = reason; return this; }
        public DecisionResponseBuilder category(String category) { this.category = category; return this; }
        public DecisionResponseBuilder confidence(Double confidence) { this.confidence = confidence; return this; }
        public DecisionResponseBuilder updatedDenyList(List<String> updatedDenyList) { this.updatedDenyList = updatedDenyList; return this; }

        public DecisionResponse build() {
            return new DecisionResponse(decision, reason, category, confidence, updatedDenyList);
        }
    }
}
