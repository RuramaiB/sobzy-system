package com.example.sobzybackend.service;

import com.example.sobzybackend.dtos.ClassificationRequest;
import com.example.sobzybackend.dtos.ClassificationResult;
import com.example.sobzybackend.dtos.TrafficIngestRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class TrafficAnalysisService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TrafficAnalysisService.class);

    private final TrafficLogService trafficLogService;
    private final ClassificationService classificationService;

    public TrafficAnalysisService(TrafficLogService trafficLogService, 
                                 ClassificationService classificationService) {
        this.trafficLogService = trafficLogService;
        this.classificationService = classificationService;
    }

    @Async("mlExecutor")
    public void processIngestedTraffic(TrafficIngestRequest request) {
        log.info("Processing ingested traffic for URL: {}", request.getUrl());

        ClassificationRequest classificationRequest = ClassificationRequest.builder()
                .url(request.getUrl())
                .content(request.getResponseBody()) 
                .method(request.getMethod())
                .ipAddress(request.getClientIp())
                .userAgent(request.getRequestHeaders().getOrDefault("User-Agent", "Unknown"))
                .referer(request.getRequestHeaders().getOrDefault("Referer", null))
                .build();

        try {
            // 1. Classification (Local or AI)
            ClassificationResult result = trafficLogService.classify(classificationRequest);

            // 2. Additional keyword filtering (Prototypical) 
            // CRITICAL: Filter whitelisted domains from being blocked by keyword heuristics
            String host = request.getHost();
            boolean isWhitelisted = classificationService.isWhitelisted(host);

            if (!isWhitelisted && result.getIsAllowed() && request.getResponseBody() != null) {
                String body = request.getResponseBody().toLowerCase();
                // "Hard" block keywords (pornography, explicit gambling apps)
                String[] explicitKeywords = { "pornhub", "xvideos", "pornography", "1xbet", "sportybet" };
                
                for (String keyword : explicitKeywords) {
                    if (body.contains(keyword)) {
                        log.warn("Explicit keyword found in response for {}: {}. Blocking domain.", request.getUrl(), keyword);
                        trafficLogService.blockDomain(host, "Explicit content detected: " + keyword);
                        break;
                    }
                }
            }

            log.info("Ingested traffic processed for {}. Category: {}, Allowed: {}", host, result.getCategory(), result.getIsAllowed());
        } catch (Exception e) {
            log.error("Error processing ingested traffic: {}", e.getMessage());
        }
    }
}
