package com.example.sobzybackend.controllers;

import com.example.sobzybackend.dtos.TrafficLogResponse;
import com.example.sobzybackend.dtos.TrafficStatisticsResponse;
import com.example.sobzybackend.service.TrafficLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Dashboard and Analytics APIs")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DashboardController {

    private final TrafficLogService trafficLogService;

    public DashboardController(TrafficLogService trafficLogService) {
        this.trafficLogService = trafficLogService;
    }

    @Operation(summary = "Get overview statistics")
    @GetMapping("/overview")
    public ResponseEntity<TrafficStatisticsResponse> getOverview() {
        return ResponseEntity.ok(trafficLogService.getStatistics());
    }

    @Operation(summary = "Get recent traffic logs")
    @GetMapping("/recent-traffic")
    public ResponseEntity<List<TrafficLogResponse>> getRecentTraffic(
            @RequestParam(defaultValue = "30") int minutes,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(trafficLogService.getRecentTraffic(minutes, limit));
    }

    @Operation(summary = "Get blocked traffic")
    @GetMapping("/blocked")
    public ResponseEntity<Page<TrafficLogResponse>> getBlockedTraffic(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(trafficLogService.getBlockedTraffic(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestTimestamp"))));
    }
}

