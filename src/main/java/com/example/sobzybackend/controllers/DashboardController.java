//package com.example.sobzybackend.controllers;
//
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import org.springframework.format.annotation.DateTimeFormat;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//
//import java.time.LocalDate;
//import java.util.List;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/v1/dashboard")
//@RequiredArgsConstructor
//@Tag(name = "Dashboard", description = "Dashboard and Analytics APIs")
//public class DashboardController {
//
//    private final DashboardService dashboardService;
//
//    @Operation(summary = "Get overview statistics")
//    @GetMapping("/overview")
//    public ResponseEntity<DashboardResponse.Overview> getOverview(
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
//    ) {
//        return ResponseEntity.ok(dashboardService.getOverview(startDate, endDate));
//    }
//
//    @Operation(summary = "Get real-time traffic statistics")
//    @GetMapping("/realtime")
//    public ResponseEntity<DashboardResponse.RealtimeStats> getRealtimeStats() {
//        return ResponseEntity.ok(dashboardService.getRealtimeStats());
//    }
//
//    @Operation(summary = "Get top visited websites")
//    @GetMapping("/top-websites")
//    public ResponseEntity<List<DashboardResponse.TopWebsite>> getTopWebsites(
//            @RequestParam(defaultValue = "10") int limit,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
//    ) {
//        return ResponseEntity.ok(dashboardService.getTopWebsites(limit, startDate, endDate));
//    }
//
//    @Operation(summary = "Get top blocked websites")
//    @GetMapping("/top-blocked")
//    public ResponseEntity<List<DashboardResponse.TopBlocked>> getTopBlocked(
//            @RequestParam(defaultValue = "10") int limit,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
//    ) {
//        return ResponseEntity.ok(dashboardService.getTopBlocked(limit, startDate, endDate));
//    }
//
//    @Operation(summary = "Get bandwidth usage statistics")
//    @GetMapping("/bandwidth")
//    public ResponseEntity<DashboardResponse.BandwidthStats> getBandwidthStats(
//            @RequestParam(required = false) Long userId,
//            @RequestParam(required = false) Long deviceId,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
//    ) {
//        return ResponseEntity.ok(dashboardService.getBandwidthStats(userId, deviceId, startDate, endDate));
//    }
//
//    @Operation(summary = "Get traffic by category")
//    @GetMapping("/traffic-by-category")
//    public ResponseEntity<Map<String, Long>> getTrafficByCategory(
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
//    ) {
//        return ResponseEntity.ok(dashboardService.getTrafficByCategory(startDate, endDate));
//    }
//
//    @Operation(summary = "Get traffic timeline (hourly/daily)")
//    @GetMapping("/traffic-timeline")
//    public ResponseEntity<List<DashboardResponse.TrafficTimeline>> getTrafficTimeline(
//            @RequestParam(defaultValue = "HOURLY") String granularity,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
//    ) {
//        return ResponseEntity.ok(dashboardService.getTrafficTimeline(granularity, startDate, endDate));
//    }
//
//    @Operation(summary = "Get user activity ranking")
//    @GetMapping("/user-activity")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<List<DashboardResponse.UserActivity>> getUserActivity(
//            @RequestParam(defaultValue = "10") int limit,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
//    ) {
//        return ResponseEntity.ok(dashboardService.getUserActivity(limit, startDate, endDate));
//    }
//
//    @Operation(summary = "Get device statistics")
//    @GetMapping("/devices")
//    public ResponseEntity<List<DashboardResponse.DeviceStats>> getDeviceStats(
//            @RequestParam(required = false) Long userId
//    ) {
//        return ResponseEntity.ok(dashboardService.getDeviceStats(userId));
//    }
//
//    @Operation(summary = "Get security alerts summary")
//    @GetMapping("/alerts")
//    public ResponseEntity<DashboardResponse.AlertsSummary> getAlertsSummary(
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
//    ) {
//        return ResponseEntity.ok(dashboardService.getAlertsSummary(startDate, endDate));
//    }
//
//    @Operation(summary = "Get ML model performance metrics")
//    @GetMapping("/ml-metrics")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<DashboardResponse.MLMetrics> getMLMetrics() {
//        return ResponseEntity.ok(dashboardService.getMLMetrics());
//    }
//
//    @Operation(summary = "Get traffic heatmap (by hour and day)")
//    @GetMapping("/heatmap")
//    public ResponseEntity<List<DashboardResponse.HeatmapData>> getTrafficHeatmap(
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
//    ) {
//        return ResponseEntity.ok(dashboardService.getTrafficHeatmap(startDate, endDate));
//    }
//
//    @Operation(summary = "Export dashboard data")
//    @GetMapping("/export")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<byte[]> exportData(
//            @RequestParam(defaultValue = "CSV") String format,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
//    ) {
//        byte[] data = dashboardService.exportData(format, startDate, endDate);
//        return ResponseEntity.ok()
//                .header("Content-Disposition", "attachment; filename=traffic-report." + format.toLowerCase())
//                .body(data);
//    }
//
//    @Operation(summary = "Get policy effectiveness metrics")
//    @GetMapping("/policy-effectiveness")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<List<DashboardResponse.PolicyEffectiveness>> getPolicyEffectiveness() {
//        return ResponseEntity.ok(dashboardService.getPolicyEffectiveness());
//    }
//}
