package com.example.sobzybackend.service;

import com.example.sobzybackend.dtos.HotspotDevice;
import com.example.sobzybackend.dtos.HotspotInfoResponse;
import com.example.sobzybackend.enums.DeviceStatus;
import com.example.sobzybackend.models.Device;
import com.example.sobzybackend.repository.DeviceRepository;
import com.example.sobzybackend.repository.UserRepository;
import com.example.sobzybackend.users.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class HotspotService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HotspotService.class);

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final DeviceService deviceService;

    public HotspotService(DeviceRepository deviceRepository,
                         UserRepository userRepository,
                         DeviceService deviceService) {
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
        this.deviceService = deviceService;
    }

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private NetworkAutomationService networkAutomationService;

    private final Set<String> lastKnownMacs = Collections.synchronizedSet(new HashSet<>());
    private static final String HOST_MAC = "F4-3B-D8-AB-E1-7C";

    // Cache for hotspot details to avoid frequent slow PS calls
    private final AtomicReference<HotspotInfoResponse> detailsCache = new AtomicReference<>();
    private final AtomicReference<Instant> lastCacheUpdate = new AtomicReference<>(Instant.EPOCH);
    private final AtomicBoolean isScanning = new AtomicBoolean(false);

    @org.springframework.beans.factory.annotation.Value("${app.network.automation.ssid:Sobzy_Safe_Hotspot}")
    private String ssid;

    @Scheduled(fixedDelay = 2000)
    public void monitorConnections() {
        // v6: Expert Discovery - Use ARP table for instantaneous detection
        List<HotspotDevice> arpDevices = fetchArpDevices();
        
        synchronized (lastKnownMacs) {
            Set<String> currentMacs = arpDevices.stream()
                    .map(HotspotDevice::getMac)
                    .filter(mac -> mac != null && !mac.equalsIgnoreCase(HOST_MAC))
                    .collect(Collectors.toSet());

            // Check for joined
            for (HotspotDevice d : arpDevices) {
                String mac = d.getMac();
                if (mac != null && !mac.equalsIgnoreCase(HOST_MAC) && !lastKnownMacs.contains(mac)) {
                    log.info(">>> INSTANT DISCOVERY: Device joined via ARP: {} ({})", mac, d.getIp());
                    saveDeviceAsFailsafe(d);
                }
            }

            // Check for left (Optional: ARP is less reliable for fast leavers, but good enough)
            for (String mac : lastKnownMacs) {
                if (!currentMacs.contains(mac)) {
                    log.info("<<< DEVICE LEFT HOTSPOT: {}", mac);
                    deviceService.updateDeviceStatus(mac, DeviceStatus.INACTIVE);
                }
            }

            lastKnownMacs.clear();
            lastKnownMacs.addAll(currentMacs);
        }
    }

    private List<HotspotDevice> fetchArpDevices() {
        List<HotspotDevice> devices = new ArrayList<>();
        try {
            Process p = Runtime.getRuntime().exec("arp -a");
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = r.readLine()) != null) {
                // Focus on dynamic entries (which hotspot devices usually are)
                if (line.contains("dynamic")) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 3) {
                        String ip = parts[0];
                        String mac = parts[1].toUpperCase().replace("-", ":");
                        devices.add(HotspotDevice.builder()
                            .ip(ip)
                            .mac(mac)
                            .hostname("Discovering...")
                            .build());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch ARP table", e);
        }
        return devices;
    }

    private void saveDeviceAsFailsafe(HotspotDevice hotspotDevice) {
        try {
            // Find a default user to associate with (for system tracking)
            User admin = userRepository.findByUsername("admin")
                    .orElseGet(() -> userRepository.findAll().stream().findFirst().orElse(null));

            if (admin == null) {
                log.warn("No user found to associate hotspot device with. Skipping persistence.");
                return;
            }

            deviceRepository.findByMacAddress(hotspotDevice.getMac())
                    .ifPresentOrElse(
                            existing -> {
                                existing.setIpAddress(hotspotDevice.getIp());
                                existing.setDeviceName(hotspotDevice.getHostname());
                                existing.setLastSeen(LocalDateTime.now());
                                deviceRepository.save(existing);
                                log.debug("Updated existing device record for MAC: {}", hotspotDevice.getMac());
                            },
                            () -> {
                                Device newDevice = Device.builder()
                                        .user(admin)
                                        .macAddress(hotspotDevice.getMac())
                                        .ipAddress(hotspotDevice.getIp())
                                        .deviceName(hotspotDevice.getHostname())
                                        .deviceType(hotspotDevice.getHostname().toLowerCase().contains("android") ||
                                                hotspotDevice.getHostname().toLowerCase().contains("iphone") ? "Mobile"
                                                        : "Desktop")
                                        .status(DeviceStatus.PENDING_AUTH)
                                        .firstSeen(LocalDateTime.now())
                                        .lastSeen(LocalDateTime.now())
                                        .build();
                                deviceRepository.save(newDevice);
                                log.info("Saved new hotspot device record for MAC: {}", hotspotDevice.getMac());
                            });
        } catch (Exception e) {
            log.error("Failed to persist hotspot device: {}", hotspotDevice.getMac(), e);
        }
    }

    public CompletableFuture<String> startHotspot() {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Requesting Java Engine to start/restart Hotspot...");
            networkAutomationService.restartEngine();
            return "Engine Restart Requested";
        });
    }

    public CompletableFuture<String> stopHotspot() {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Stopping Engine/Hotspot...");
            networkAutomationService.stopEngine();
            return "Engine Stop Requested";
        });
    }

    public CompletableFuture<HotspotInfoResponse> getHotspotDetails() {
        return CompletableFuture.supplyAsync(() -> {
            // Actively refresh device list from scan if engine is running
            HotspotInfoResponse cached = detailsCache.get();
            if (cached != null && "RUNNING".equals(cached.getStatus())) {
                if (isScanning.compareAndSet(false, true)) {
                    try {
                        log.debug("V5: Triggering throttled network scan...");
                        List<HotspotDevice> hotspotDevices = deviceService.scanNetwork().stream()
                            .map((com.example.sobzybackend.dtos.DeviceResponse d) -> HotspotDevice.builder()
                                .mac(d.getMacAddress())
                                .ip(d.getIpAddress())
                                .hostname(d.getDeviceName())
                                .status(d.getStatus())
                                .authenticated(d.isAuthenticated())
                                .build())
                            .collect(Collectors.toList());
                        cached.setConnectedDevices(hotspotDevices);
                    } catch (Exception e) {
                        log.error("Failed to update device list during details fetch", e);
                    } finally {
                        isScanning.set(false);
                    }
                } else {
                    log.debug("V5: Network scan already in progress, skipping this cycle.");
                }
                return cached;
            }

            if (cached != null) return cached;

            // Failsafe response with known config
            return HotspotInfoResponse.builder()
                    .ssid(ssid != null ? ssid : "Sobzy_Safe_Hotspot")
                    .password("Sobzy12345")
                    .status("INITIALIZING")
                    .hostIp("192.168.137.1")
                    .connectedDevices(new ArrayList<>())
                    .build();
        });
    }

    public CompletableFuture<String> checkIcsStatus() {
        return CompletableFuture.completedFuture("Managed by Java Engine");
    }

    public void logIcsConfigInstructions() {
        log.info("ICS is managed automatically by the Sobzy Java Engine.");
    }

    public void updateStateFromEngine(HotspotInfoResponse state) {
        if (state != null) {
            if (state.getConnectedDevices() == null) {
                state.setConnectedDevices(new ArrayList<>());
            }
            this.detailsCache.set(state);
            this.lastCacheUpdate.set(Instant.now());
            log.debug("Hotspot state updated from Java Engine: {}", state.getStatus());
        }
    }
}
