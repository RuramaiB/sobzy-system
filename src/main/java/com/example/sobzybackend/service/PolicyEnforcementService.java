package com.example.sobzybackend.service;

import com.example.sobzybackend.dtos.ClassificationRequest;
import com.example.sobzybackend.dtos.DecisionResponse;
import com.example.sobzybackend.repository.BlockedSiteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyEnforcementService {

    private final ClassificationService classificationService;
    private final VpnDetectionService vpnDetectionService;

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
        boolean isExplicitOrRestricted = "ADULT_CONTENT".equals(category) || 
                                        "GAMING".equals(category) || 
                                        "GAMBLING".equals(category) ||
                                        "SOCIAL_MEDIA".equals(category);

        String decision = "ALLOW";
        String reason = "AI Analysis: Low risk (" + category + ")";

        if (isExplicitOrRestricted) {
            decision = "BLOCK";
            reason = "Restricted category: " + category;
        } else if (isEducational || isMusic || "BENIGN".equals(category)) {
            decision = "ALLOW";
            reason = "Allowed category: " + category;
        } else {
            // Default to block for everything else (Educational only policy)
            decision = "BLOCK";
            reason = "AI Analysis: Not in allowed educational/music categories (" + category + ")";
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
            if (url.contains("://")) {
                return new java.net.URL(url).getHost().toLowerCase();
            }
            return url.split("/")[0].toLowerCase();
        } catch (Exception e) {
            return url;
        }
    }
}
