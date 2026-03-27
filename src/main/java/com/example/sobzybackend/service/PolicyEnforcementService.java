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
    public DecisionResponse enforce(ClassificationRequest request, List<String> activeBlockedDomains, String role) {
        String url = request.getUrl();
        String domain = extractDomain(url);

        // 1. Layer 1: Anti-Evasion Check (Always Block)
        if (vpnDetectionService.isProxyDomain(domain) || vpnDetectionService.isSuspicious(request.getIpAddress())) {
            return DecisionResponse.builder()
                    .decision("BLOCK")
                    .reason("Anti-Evasion: Proxy/VPN pattern detected")
                    .category("SECURITY_EVASION")
                    .updatedDenyList(activeBlockedDomains)
                    .build();
        }

        // 2. Layer 2: Check Manual Deny List (Always Block)
        if (activeBlockedDomains.contains(domain)) {
            return DecisionResponse.builder()
                    .decision("BLOCK")
                    .reason("Domain is in manual deny list")
                    .category("RESTRICTED")
                    .updatedDenyList(activeBlockedDomains)
                    .build();
        }

        // 3. Layer 3: AI Classification & Role-Based Policy
        ClassificationService.PredictionResult result = classificationService.predict(url);
        String category = result.category();
        double confidence = result.confidence();

        boolean isAdmin = "ADMIN".equals(role);
        boolean isEducational = "EDUCATION".equals(category) || "RESEARCH".equals(category) || classificationService.isWhitelisted(domain);
        boolean isMalicious = "ADULT_CONTENT".equals(category) || "GAMBLING".equals(category) || "TORRENT".equals(category);

        String decision = "ALLOW";
        String reason = "Policy: Allowed (" + category + ")";

        if (isAdmin) {
            // ADMINS: Block only malicious content
            if (isMalicious) {
                decision = "BLOCK";
                reason = "Admin Policy: Restricted malicious category (" + category + ")";
            }
        } else {
            // USERS/GUESTS: Strict "Educational Only" Policy
            if (isMalicious) {
                decision = "BLOCK";
                reason = "Policy: Restricted malicious category (" + category + ")";
            } else if (!isEducational) {
                // If not clearly educational, block for non-admins
                decision = "BLOCK";
                reason = "Policy: Non-educational content restricted for " + (role != null ? role : "Guest");
            } else {
                decision = "ALLOW";
                reason = "Policy: Allowed educational content";
            }
        }

        return DecisionResponse.builder()
                .decision(decision)
                .reason(reason)
                .category(category)
                .confidence(confidence)
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
