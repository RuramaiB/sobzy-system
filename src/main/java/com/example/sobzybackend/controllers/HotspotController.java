package com.example.sobzybackend.controllers;

import com.example.sobzybackend.dtos.HotspotInfoResponse;
import com.example.sobzybackend.service.HotspotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/hotspot")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HotspotController {

    private final HotspotService hotspotService;

    @GetMapping("/details")
    public CompletableFuture<ResponseEntity<HotspotInfoResponse>> getDetails() {
        return hotspotService.getHotspotDetails()
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/start")
    public CompletableFuture<ResponseEntity<Map<String, String>>> start() {
        return hotspotService.startHotspot()
                .thenApply(result -> ResponseEntity.ok(Map.of("status", result)));
    }

    @PostMapping("/stop")
    public CompletableFuture<ResponseEntity<Map<String, String>>> stop() {
        return hotspotService.stopHotspot()
                .thenApply(result -> ResponseEntity.ok(Map.of("status", result)));
    }
}
