package com.example.sobzybackend.controllers;

import com.example.sobzybackend.models.TrafficEvent;
import com.example.sobzybackend.service.TrafficEventService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/traffic/events")
@CrossOrigin(origins = "*")
public class TrafficEventController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TrafficEventController.class);

    private final TrafficEventService trafficEventService;

    public TrafficEventController(TrafficEventService trafficEventService) {
        this.trafficEventService = trafficEventService;
    }

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
