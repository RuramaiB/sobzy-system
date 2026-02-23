package com.example.sobzybackend.controllers;

import com.example.sobzybackend.models.BlockedSite;
import com.example.sobzybackend.service.BlockedSiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/blocked-sites")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BlockedSiteController {
    private final BlockedSiteService service;

    @GetMapping
    public ResponseEntity<List<BlockedSite>> getAll() {
        return ResponseEntity.ok(service.getAllBlockedSites());
    }

    @PostMapping
    public ResponseEntity<BlockedSite> add(@RequestBody BlockedSite site) {
        return ResponseEntity.ok(service.addBlockedSite(site));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<BlockedSite> toggle(@PathVariable Long id) {
        return ResponseEntity.ok(service.toggleStatus(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteBlockedSite(id);
        return ResponseEntity.noContent().build();
    }
}
