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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TrafficLogService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TrafficLogService.class);

    private final TrafficLogRepository trafficLogRepository;
    private final ClassificationService classificationService;
    private final PortalService portalService;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final BlockedSiteRepository blockedSiteRepository;
    private final CategoryRepository categoryRepository;

    public TrafficLogService(TrafficLogRepository trafficLogRepository, 
                             ClassificationService classificationService,
                             PortalService portalService,
                             UserRepository userRepository,
                             DeviceRepository deviceRepository,
                             BlockedSiteRepository blockedSiteRepository,
                             CategoryRepository categoryRepository) {
        this.trafficLogRepository = trafficLogRepository;
        this.classificationService = classificationService;
        this.portalService = portalService;
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
        this.blockedSiteRepository = blockedSiteRepository;
        this.categoryRepository = categoryRepository;
    }

    private final Map<String, Category> categoryCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Boolean> blockedDomainCache = new java.util.concurrent.ConcurrentHashMap<>();
    private LocalDateTime lastCacheRefresh = LocalDateTime.MIN;

    @Transactional
    public ClassificationResult classify(ClassificationRequest request) {
        // 1. Get classification from ML model
        ClassificationResult result = classificationService.classify(request);

        // 2. Override with local policy (Blocked Sites) - Using Cache
        String domain = extractDomain(request.getUrl());
        if (isDomainBlocked(domain)) {
            result.setIsAllowed(false);
            result.setDecision("BLOCK");
            result.setReason("Site is blocked by policy");
        }

        // 3. Identify User and Device (Fast lookups)
        String username = portalService.getUsernameForIp(request.getIpAddress());
        if (username == null) username = "guest";

        String macAddress = portalService.getMacForIp(request.getIpAddress());
        
        // 4. Map Category - Using Cache
        Category category = null;
        if (result.getCategory() != null) {
            category = getCategoryCached(result.getCategory().toUpperCase());
        }

        // 5. Build and Persist Log ASYNCHRONOUSLY
        // We capture IDs instead of entities to avoid LazyInitialization / Session issues in Async
        persistLogAsync(request, result, domain, username, macAddress, category != null ? category.getId() : null);

        return result;
    }

    private boolean isDomainBlocked(String domain) {
        refreshCacheIfNeeded();
        return blockedDomainCache.getOrDefault(domain, false);
    }

    private Category getCategoryCached(String name) {
        refreshCacheIfNeeded();
        return categoryCache.get(name);
    }

    private synchronized void refreshCacheIfNeeded() {
        if (lastCacheRefresh.isBefore(LocalDateTime.now().minusMinutes(5))) {
            log.debug("Refreshing TrafficLogService caches...");
            blockedSiteRepository.findByActive(true).forEach(site -> 
                blockedDomainCache.put(site.getUrl().toLowerCase(), true));
            categoryRepository.findAll().forEach(cat -> 
                categoryCache.put(cat.getName().toUpperCase(), cat));
            lastCacheRefresh = LocalDateTime.now();
        }
    }

    @org.springframework.scheduling.annotation.Async
    public void persistLogAsync(ClassificationRequest request, ClassificationResult result, 
                               String domain, String username, String macAddress, Long categoryId) {
        try {
            User user = userRepository.findByUsernameOrEmail(username, username)
                    .orElseGet(() -> userRepository.findAll().stream().findFirst().orElse(null));

            Device device = null;
            if (macAddress != null) {
                device = deviceRepository.findByMacAddress(macAddress).orElse(null);
            }
            if (device == null) {
                device = deviceRepository.findByIpAddress(request.getIpAddress())
                        .stream().findFirst().orElse(null);
            }

            Category category = categoryId != null ? categoryRepository.findById(categoryId).orElse(null) : null;

            TrafficLog logEntry = TrafficLog.builder()
                    .user(user)
                    .device(device)
                    .url(request.getUrl())
                    .domain(domain)
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
        } catch (Exception e) {
            log.error("Failed to persist traffic log asynchronously", e);
        }
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
    public Page<TrafficLogResponse> getAllTrafficLogs(Pageable pageable) {
        return trafficLogRepository.findAll(pageable)
                .map(this::convertToResponse);
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

        long activeDevices = deviceRepository.countByStatus(com.example.sobzybackend.enums.DeviceStatus.ACTIVE);
        long activeUsers = portalService.getAuthenticatedIps().values().stream().distinct().count();
        long totalBandwidth = trafficLogRepository.sumTotalSize();

        return TrafficStatisticsResponse.builder()
                .totalRequests(totalRequests)
                .blockedRequests(blockedRequests)
                .allowedRequests(allowedRequests)
                .blockRate(blockRate)
                .activeDevices(activeDevices)
                .activeUsers(activeUsers)
                .totalBandwidth(totalBandwidth)
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
                .confidence(log.getClassificationConfidence() != null ? log.getClassificationConfidence().doubleValue() : null)
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