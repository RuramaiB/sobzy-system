package com.example.sobzybackend.controllers;

import com.example.sobzybackend.dtos.ClassificationRequest;
import com.example.sobzybackend.dtos.ClassificationResult;
import com.example.sobzybackend.dtos.DecisionResponse;
import com.example.sobzybackend.dtos.TrafficIngestRequest;
import com.example.sobzybackend.service.PolicyEnforcementService;
import com.example.sobzybackend.service.TrafficAnalysisService;
import com.example.sobzybackend.service.TrafficLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/traffic")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TrafficIngestController {

    private final TrafficAnalysisService trafficAnalysisService;
    private final TrafficLogService trafficLogService;
    private final PolicyEnforcementService policyEnforcementService;

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

        DecisionResponse decision = policyEnforcementService.enforce(classificationRequest, activeBlocked);

        // 2. Forward to async processing for persistent logging
        trafficAnalysisService.processIngestedTraffic(request);

        return ResponseEntity.ok(decision);
    }

    @GetMapping("/blocked-domains")
    public ResponseEntity<List<String>> getBlockedDomains() {
        return ResponseEntity.ok(trafficLogService.getActiveBlockedDomains());
    }
}
