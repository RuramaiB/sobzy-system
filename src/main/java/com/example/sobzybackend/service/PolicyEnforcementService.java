package com.example.sobzybackend.service;

import com.example.sobzybackend.dtos.ClassificationRequest;
import com.example.sobzybackend.dtos.DecisionResponse;
import java.util.List;

@org.springframework.stereotype.Service
public class PolicyEnforcementService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PolicyEnforcementService.class);

    private final ClassificationService classificationService;
    private final VpnDetectionService vpnDetectionService;

    public PolicyEnforcementService(ClassificationService classificationService,
                                   VpnDetectionService vpnDetectionService) {
        this.classificationService = classificationService;
        this.vpnDetectionService = vpnDetectionService;
    }

    /**
     * Makes a final decision (ALLOW/BLOCK) based on policy and ML results.
     */
    public DecisionResponse enforce(ClassificationRequest request, List<String> activeBlockedDomains) {
        String url = request.getUrl();
        String domain = extractDomain(url);

        // 1. Layer 1: Anti-Evasion Check
        if (vpnDetectionService.isProxyDomain(domain) || vpnDetectionService.isSuspicious(request.getIpAddress())) {
            return DecisionResponse.builder()
                    .decision("BLOCK")
                    .reason("Anti-Evasion: Proxy/VPN pattern detected")
                    .category("SECURITY_EVASION")
                    .updatedDenyList(activeBlockedDomains)
                    .build();
        }

        // 2. Layer 2: Check Manual Deny List
        if (activeBlockedDomains.contains(domain)) {
            return DecisionResponse.builder()
                    .decision("BLOCK")
                    .reason("Domain is in manual deny list")
                    .category("RESTRICTED")
                    .updatedDenyList(activeBlockedDomains)
                    .build();
        }

        // 3. Layer 3: ML Classification
        ClassificationService.PredictionResult result = classificationService.predict(url);

        String category = result.category();
        boolean isEducational = "RESEARCH".equals(category) || "EDUCATION".equals(category);
        boolean isMusic = "MUSIC".equals(category);
        boolean isToolOrUtility = "BENIGN".equals(category) || "OTHER".equals(category); // Relaxing OTHER to allow more sites
        
        boolean isExplicitOrRestricted = "ADULT_CONTENT".equals(category) || 
                                        "GAMBLING".equals(category) ||
                                        "GAMING".equals(category);
                                        // Removing SOCIAL_MEDIA from strict block list to allow LinkedIn/Research sites

        String decision = "ALLOW";
        String reason = "AI Analysis: Low risk (" + category + ")";

        if (isExplicitOrRestricted) {
            decision = "BLOCK";
            reason = "Restricted category: " + category;
        } else if (isEducational || isMusic || isToolOrUtility || "SEARCH".equals(category) || "SOCIAL_MEDIA".equals(category)) {
            decision = "ALLOW";
            reason = "Allowed category: " + category;
        } else {
            // Default to allow for anything not explicitly malicious (Restricted policy can be toggled by admin)
            decision = "ALLOW";
            reason = "AI Analysis: Non-restricted category (" + category + ")";
        }

        return DecisionResponse.builder()
                .decision(decision)
                .reason(reason)
                .category(category)
                .confidence(result.confidence())
                .updatedDenyList(activeBlockedDomains)
                .build();
    }

    private String extractDomain(String url) {
        try {
            if (url == null) return "unknown";
            if (url.contains("://")) {
                return new java.net.URL(url).getHost().toLowerCase();
            }
            return url.split("/")[0].toLowerCase();
        } catch (Exception e) {
            return url;
        }
    }
}
