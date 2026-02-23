package com.example.sobzybackend.controllers;

import com.example.sobzybackend.dtos.ClassificationRequest;
import com.example.sobzybackend.dtos.ClassificationResult;
import com.example.sobzybackend.dtos.MessageResponse;
import com.example.sobzybackend.dtos.TrafficLogResponse;
import com.example.sobzybackend.dtos.TrafficStatisticsResponse;
import com.example.sobzybackend.service.TrafficLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/traffic")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class TrafficController {

    private final TrafficLogService trafficLogService;

    @GetMapping("/logs")
    public ResponseEntity<Page<TrafficLogResponse>> getAllTrafficLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "requestTimestamp") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        log.info("Get all traffic logs - page: {}, size: {}", page, size);

        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<TrafficLogResponse> logs = trafficLogService.getAllTrafficLogs(pageable);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/logs/user/{userId}")
    public ResponseEntity<Page<TrafficLogResponse>> getTrafficLogsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Get traffic logs for user: {}", userId);
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "requestTimestamp"));

        Page<TrafficLogResponse> logs = trafficLogService.getTrafficLogsByUserId(userId, pageable);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/logs/device/{deviceId}")
    public ResponseEntity<Page<TrafficLogResponse>> getTrafficLogsByDevice(
            @PathVariable Long deviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Get traffic logs for device: {}", deviceId);
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "requestTimestamp"));

        Page<TrafficLogResponse> logs = trafficLogService.getTrafficLogsByDeviceId(deviceId, pageable);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/logs/blocked")
    public ResponseEntity<Page<TrafficLogResponse>> getBlockedTraffic(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Get blocked traffic logs");
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "requestTimestamp"));

        Page<TrafficLogResponse> logs = trafficLogService.getBlockedTraffic(pageable);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/logs/recent")
    public ResponseEntity<List<TrafficLogResponse>> getRecentTraffic(
            @RequestParam(defaultValue = "30") int minutes,
            @RequestParam(defaultValue = "100") int limit) {

        log.info("Get recent traffic - last {} minutes", minutes);
        List<TrafficLogResponse> logs = trafficLogService.getRecentTraffic(minutes, limit);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/statistics")
    public ResponseEntity<TrafficStatisticsResponse> getStatistics() {
        log.info("Get traffic statistics");
        TrafficStatisticsResponse stats = trafficLogService.getStatistics();
        return ResponseEntity.ok(stats);
    }

    @DeleteMapping("/logs/cleanup")
    public ResponseEntity<MessageResponse> cleanupOldLogs(
            @RequestParam(defaultValue = "90") int daysToKeep) {
        log.info("Cleanup logs older than {} days", daysToKeep);
        trafficLogService.cleanOldLogs(daysToKeep);
        return ResponseEntity.ok(MessageResponse.success(
                "Old traffic logs cleaned successfully"));
    }

    @PostMapping("/classify")
    public ResponseEntity<ClassificationResult> classifyTraffic(@RequestBody ClassificationRequest request) {
        log.info("Real-time classification request for URL: {}", request.getUrl());
        // In a real implementation, we would also log this to TrafficLogRepository
        // For now, we delegate to the Python classifier
        return ResponseEntity.ok(trafficLogService.classify(request));
    }

    @GetMapping("/health")
    public ResponseEntity<MessageResponse> health() {
        return ResponseEntity.ok(MessageResponse.success("Traffic service is running"));
    }
}
