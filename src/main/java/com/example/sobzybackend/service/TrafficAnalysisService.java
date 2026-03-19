package com.example.sobzybackend.service;

import com.example.sobzybackend.dtos.ClassificationRequest;
import com.example.sobzybackend.dtos.ClassificationResult;
import com.example.sobzybackend.dtos.TrafficIngestRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrafficAnalysisService {

    private final TrafficLogService trafficLogService;

    @Async("mlExecutor")
    public void processIngestedTraffic(TrafficIngestRequest request) {
        log.info("Processing ingested traffic for URL: {}", request.getUrl());

        ClassificationRequest classificationRequest = ClassificationRequest.builder()
                .url(request.getUrl())
                .content(request.getResponseBody()) // Use response body for classification if available
                .method(request.getMethod())
                .ipAddress(request.getClientIp())
                .userAgent(request.getRequestHeaders().getOrDefault("User-Agent", "Unknown"))
                .referer(request.getRequestHeaders().getOrDefault("Referer", null))
                .build();

        try {
            // 1. Classification (Local or AI)
            ClassificationResult result = trafficLogService.classify(classificationRequest);

            // 2. Additional keyword filtering (Prototypical)
            if (result.getIsAllowed() && request.getResponseBody() != null) {
                String body = request.getResponseBody().toLowerCase();
                String[] suspiciousKeywords = { "illegal", "malware", "phishing", "betting", "gambling" };
                for (String keyword : suspiciousKeywords) {
                    if (body.contains(keyword)) {
                        log.warn("Suspicious keyword found in response for {}: {}", request.getUrl(), keyword);
                        trafficLogService.blockDomain(request.getHost(), "Suspicious keyword found: " + keyword);
                        break;
                    }
                }
            }

            log.info("Ingested traffic processed. Decision: {}", result.getDecision());
        } catch (Exception e) {
            log.error("Error processing ingested traffic: {}", e.getMessage());
        }
    }
}
