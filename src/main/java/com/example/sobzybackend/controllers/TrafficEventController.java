package com.example.sobzybackend.controllers;

import com.example.sobzybackend.models.TrafficEvent;
import com.example.sobzybackend.service.TrafficEventService;
import com.example.sobzybackend.dtos.MessageResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/traffic/events")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TrafficEventController {

    private final TrafficEventService trafficEventService;

    @PostMapping
    public ResponseEntity<TrafficEvent> logEvent(@RequestBody TrafficEvent event, HttpServletRequest request) {
        // If IP is not provided in body, use request remote address
        if (event.getIpAddress() == null || event.getIpAddress().isEmpty()) {
            event.setIpAddress(request.getRemoteAddr());
        }
        log.debug("Logging browser event from IP: {}: {} on {}", event.getIpAddress(), event.getEventType(),
                event.getUrl());
        return ResponseEntity.ok(trafficEventService.logEvent(event));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<TrafficEvent>> getRecentEvents() {
        return ResponseEntity.ok(trafficEventService.getRecentEvents());
    }
}
