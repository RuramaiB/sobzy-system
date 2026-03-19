package com.example.sobzybackend.service;

import com.example.sobzybackend.dtos.ClassificationRequest;
import com.example.sobzybackend.dtos.ClassificationResult;
import com.example.sobzybackend.dtos.TrafficLogResponse;
import com.example.sobzybackend.dtos.TrafficStatisticsResponse;
import com.example.sobzybackend.models.BlockedSite;
import com.example.sobzybackend.models.Category;
import com.example.sobzybackend.models.Device;
import com.example.sobzybackend.models.TrafficLog;
import com.example.sobzybackend.repository.*;
import com.example.sobzybackend.users.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrafficLogService {

    private final TrafficLogRepository trafficLogRepository;
    private final ClassificationService classificationService;
    private final PortalService portalService;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final BlockedSiteRepository blockedSiteRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public Page<TrafficLogResponse> getAllTrafficLogs(Pageable pageable) {
        return trafficLogRepository.findAll(pageable)
                .map(this::convertToResponse);
    }

    @Transactional
    public ClassificationResult classify(ClassificationRequest request) {
        log.info("Classifying traffic for IP: {}, URL: {}", request.getIpAddress(), request.getUrl());

        // 1. Get classification from ML model
        ClassificationResult result = classificationService.classify(request);

        // 2. Override with local policy (Blocked Sites)
        String domain = extractDomain(request.getUrl());
        blockedSiteRepository.findByUrl(domain).ifPresent(site -> {
            if (site.isActive()) {
                result.setIsAllowed(false);
                result.setDecision("BLOCK");
                result.setReason("Site is blocked by policy: " + site.getReason());
            }
        });

        // 3. Identify User and Device
        String username = portalService.getUsernameForIp(request.getIpAddress());
        if (username == null)
            username = "guest";

        final String userKey = username;
        User user = userRepository.findByUsernameOrEmail(userKey, userKey)
                .orElseGet(() -> userRepository.findAll().stream().findFirst().orElse(null));

        // Attempt to find device by MAC address for more accurate tracking
        String macAddress = portalService.getMacForIp(request.getIpAddress());
        Device device = null;

        if (macAddress != null) {
            device = deviceRepository.findByMacAddress(macAddress).orElse(null);
        }

        if (device == null) {
            // Fallback to IP address if MAC lookup fails or no device found by MAC
            device = deviceRepository.findByIpAddress(request.getIpAddress())
                    .stream().findFirst()
                    .orElseGet(() -> deviceRepository.findAll().stream().findFirst().orElse(null));
        }

        // Note: Even if user/device is null, we STILL persist the log (Anonymous/Guest)
        // This ensures 100% coverage as requested by the user.

        // 4. Map Category
        Category category = null;
        if (result.getCategory() != null) {
            category = categoryRepository.findByName(result.getCategory().toUpperCase()).orElse(null);
        }

        // 5. Persist Log
        TrafficLog logEntry = TrafficLog.builder()
                .user(user)
                .device(device)
                .url(request.getUrl())
                .domain(result.getDomain())
                .method(request.getMethod())
                .ipAddress(request.getIpAddress())
                .userAgent(request.getUserAgent())
                .referer(request.getReferer())
                .category(category)
                .classificationConfidence(java.math.BigDecimal.valueOf(result.getConfidence()))
                .isBlocked(!result.getIsAllowed())
                .blockReason(result.getReason())
                .requestTimestamp(LocalDateTime.now())
                .requestSize(0L)
                .responseSize(0L)
                .build();

        trafficLogRepository.save(logEntry);
        return result;
    }

    @Transactional(readOnly = true)
    public List<String> getActiveBlockedDomains() {
        return blockedSiteRepository.findByActive(true).stream()
                .map(site -> site.getUrl())
                .collect(Collectors.toList());
    }

    @Transactional
    public void blockDomain(String domain, String reason) {
        log.info("Blocking domain: {} for reason: {}", domain, reason);
        if (!blockedSiteRepository.existsByUrl(domain)) {
            BlockedSite site = BlockedSite.builder()
                    .url(domain)
                    .reason(reason)
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            blockedSiteRepository.save(site);
        }
    }

    @Transactional(readOnly = true)
    public Page<TrafficLogResponse> getTrafficLogsByUserId(Long userId, Pageable pageable) {
        return trafficLogRepository.findByUserId(userId, pageable)
                .map(this::convertToResponse);
    }

    @Transactional(readOnly = true)
    public Page<TrafficLogResponse> getTrafficLogsByDeviceId(Long deviceId, Pageable pageable) {
        return trafficLogRepository.findByDeviceId(deviceId, pageable)
                .map(this::convertToResponse);
    }

    @Transactional(readOnly = true)
    public Page<TrafficLogResponse> getBlockedTraffic(Pageable pageable) {
        return trafficLogRepository.findByIsBlocked(true, pageable)
                .map(this::convertToResponse);
    }

    @Transactional(readOnly = true)
    public List<TrafficLogResponse> getRecentTraffic(int minutes, int limit) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        return trafficLogRepository.findRecentTraffic(since,
                Pageable.ofSize(limit)).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TrafficStatisticsResponse getStatistics() {
        long totalRequests = trafficLogRepository.count();
        long blockedRequests = trafficLogRepository.countBlockedTraffic();
        long allowedRequests = totalRequests - blockedRequests;
        double blockRate = totalRequests > 0 ? (double) blockedRequests / totalRequests * 100 : 0;

        List<Object[]> categoryData = trafficLogRepository.countByCategory();
        Map<String, Long> trafficByCategory = new HashMap<>();
        for (Object[] row : categoryData) {
            trafficByCategory.put((String) row[0], (Long) row[1]);
        }

        List<Object[]> domainData = trafficLogRepository.findTopDomains(Pageable.ofSize(10));
        Map<String, Long> topDomains = new HashMap<>();
        for (Object[] row : domainData) {
            topDomains.put((String) row[0], (Long) row[1]);
        }

        return TrafficStatisticsResponse.builder()
                .totalRequests(totalRequests)
                .blockedRequests(blockedRequests)
                .allowedRequests(allowedRequests)
                .blockRate(blockRate)
                .trafficByCategory(trafficByCategory)
                .topDomains(topDomains)
                .build();
    }

    @Transactional
    public void cleanOldLogs(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        trafficLogRepository.deleteByRequestTimestampBefore(cutoffDate);
        log.info("Deleted traffic logs older than {} days", daysToKeep);
    }

    private String extractDomain(String url) {
        try {
            if (url == null)
                return "";
            String host = url;
            if (url.contains("://")) {
                host = new java.net.URL(url).getHost();
            } else if (url.contains("/")) {
                host = url.split("/")[0];
            }
            return host.toLowerCase();
        } catch (Exception e) {
            return url;
        }
    }

    private TrafficLogResponse convertToResponse(TrafficLog log) {
        return TrafficLogResponse.builder()
                .id(log.getId())
                .userId(log.getUser() != null ? log.getUser().getId() : null)
                .username(log.getUser() != null ? log.getUser().getUsername() : "Guest")
                .deviceId(log.getDevice() != null ? log.getDevice().getId() : null)
                .deviceName(log.getDevice() != null ? log.getDevice().getDeviceName() : "Unknown Device")
                .url(log.getUrl())
                .domain(log.getDomain())
                .method(log.getMethod())
                .statusCode(log.getStatusCode())
                .requestSize(log.getRequestSize())
                .responseSize(log.getResponseSize())
                .totalSize(log.getTotalSize())
                .category(log.getCategory() != null ? log.getCategory().getName() : null)
                .confidence(log.getClassificationConfidence())
                .isBlocked(log.getIsBlocked())
                .blockReason(log.getBlockReason())
                .responseTimeMs(log.getResponseTimeMs())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .referer(log.getReferer())
                .requestTimestamp(log.getRequestTimestamp())
                .build();
    }
}