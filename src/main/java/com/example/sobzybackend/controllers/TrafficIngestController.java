package com.example.sobzybackend.controllers;

import com.example.sobzybackend.dtos.ClassificationRequest;
import com.example.sobzybackend.dtos.DecisionResponse;
import com.example.sobzybackend.dtos.TrafficIngestRequest;
import com.example.sobzybackend.service.PolicyEnforcementService;
import com.example.sobzybackend.service.PortalService;
import com.example.sobzybackend.service.TrafficAnalysisService;
import com.example.sobzybackend.service.TrafficLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/traffic")
@CrossOrigin(origins = "*")
public class TrafficIngestController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TrafficIngestController.class);

    private final TrafficAnalysisService trafficAnalysisService;
    private final TrafficLogService trafficLogService;
    private final PolicyEnforcementService policyEnforcementService;
    private final PortalService portalService;

    public TrafficIngestController(TrafficAnalysisService trafficAnalysisService,
                                  TrafficLogService trafficLogService,
                                  PolicyEnforcementService policyEnforcementService,
                                  PortalService portalService) {
        this.trafficAnalysisService = trafficAnalysisService;
        this.trafficLogService = trafficLogService;
        this.policyEnforcementService = policyEnforcementService;
        this.portalService = portalService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<DecisionResponse> ingestTraffic(@RequestBody TrafficIngestRequest request) {
        log.debug("Received traffic ingestion for URL: {}", request.getUrl());

        // 1. Get real-time decision
        List<String> activeBlocked = trafficLogService.getActiveBlockedDomains();

        ClassificationRequest classificationRequest = ClassificationRequest.builder()
                .url(request.getUrl())
                .ipAddress(request.getClientIp())
                .method(request.getMethod())
                .userAgent(request.getRequestHeaders().getOrDefault("User-Agent", "Unknown"))
                .build();

        String role = portalService.getRoleForIp(request.getClientIp());
        DecisionResponse decision = policyEnforcementService.enforce(classificationRequest, activeBlocked, role);

        // 2. Forward to async processing for persistent logging
        trafficAnalysisService.processIngestedTraffic(request);

        return ResponseEntity.ok(decision);
    }

    @GetMapping("/blocked-domains")
    public ResponseEntity<List<String>> getBlockedDomains() {
        return ResponseEntity.ok(trafficLogService.getActiveBlockedDomains());
    }
}
