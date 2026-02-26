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
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotspotService {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;

    private final Set<String> lastKnownMacs = Collections.synchronizedSet(new HashSet<>());
    private static final String HOST_MAC = "F4-3B-D8-AB-E1-7C";

    // Cache for hotspot details to avoid frequent slow PS calls
    private final AtomicReference<HotspotInfoResponse> detailsCache = new AtomicReference<>();
    private final AtomicReference<Instant> lastCacheUpdate = new AtomicReference<>(Instant.EPOCH);
    private static final long CACHE_TTL_MS = 2000; // 2 seconds

    @Scheduled(fixedDelay = 5000)
    public void monitorConnections() {
        getHotspotDetails().thenAccept(response -> {
            Set<String> currentMacs = response.getConnectedDevices().stream()
                    .map(HotspotDevice::getMac)
                    .filter(mac -> mac != null && !mac.equalsIgnoreCase(HOST_MAC))
                    .collect(Collectors.toSet());

            synchronized (lastKnownMacs) {
                // Check for new connections
                for (String mac : currentMacs) {
                    if (!lastKnownMacs.contains(mac)) {
                        log.info(">>> DEVICE JOINED HOTSPOT: {}", mac);

                        HotspotDevice d = response.getConnectedDevices().stream()
                                .filter(dev -> mac.equalsIgnoreCase(dev.getMac()))
                                .findFirst()
                                .orElse(null);

                        if (d != null) {
                            log.info("Details - IP: {}, Hostname: {}", d.getIp(), d.getHostname());
                            saveDeviceAsFailsafe(d);
                        }
                    }
                }

                // Check for disconnections
                for (String mac : lastKnownMacs) {
                    if (!currentMacs.contains(mac)) {
                        log.info("<<< DEVICE LEFT HOTSPOT: {}", mac);
                    }
                }

                lastKnownMacs.clear();
                lastKnownMacs.addAll(currentMacs);
            }
        }).exceptionally(ex -> {
            log.error("Error monitoring hotspot connections: {}", ex.getMessage());
            return null;
        });
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
                                        .status(DeviceStatus.ACTIVE)
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
            log.info("Attempting to start Windows Mobile Hotspot...");
            String script = "try { "
                    + "  $connectionProfile = [Windows.Networking.Connectivity.NetworkInformation, Windows.Networking.Connectivity, ContentType=WindowsRuntime]::GetInternetConnectionProfile(); "
                    + "  if (-not $connectionProfile) { throw 'No internet connection profile found' } "
                    + "  $tetheringManager = [Windows.Networking.NetworkOperators.NetworkOperatorTetheringManager, Windows.Networking.NetworkOperators, ContentType=WindowsRuntime]::CreateFromConnectionProfile($connectionProfile); "
                    + "  if ($tetheringManager) { "
                    + "    $asyncOp = $tetheringManager.StartTetheringAsync(); "
                    + "    $timeout = 100; "
                    + "    while ($asyncOp.Status -eq 'Started' -and $timeout -gt 0) { Start-Sleep -Milliseconds 100; $timeout-- } "
                    + "    $result = $asyncOp.GetResults(); "
                    + "    if ($result.Status -eq 'Success') { 'Success' } else { 'Failed: ' + $result.Status } "
                    + "  } else { 'Failed to get tethering manager' } "
                    + "} catch { 'ERROR: ' + $_.Exception.Message }";
            String result = runPowerShell("-Command", script);

            if ("Success".equals(result)) {
                log.info("Hotspot started. Automating network setup...");
                // 1. Enable Internet Connection Sharing (ICS)
                runPowerShell("-File", "./scripts/enable_ics.ps1");

                // 2. Open Portal on host machine browser as requested
                try {
                    String hostIp = runPowerShell("-Command",
                            "(Get-NetIPAddress | Where-Object {$_.AddressFamily -eq 'IPv4' -and $_.InterfaceAlias -notlike 'Loopback*'}).IPAddress[0]");
                    if (hostIp != null && !hostIp.isEmpty()) {
                        log.info("Opening portal for host machine at http://{}:3000/login", hostIp);
                        runPowerShell("-Command", "Start-Process 'http://" + hostIp + ":3000/login'");
                    }
                } catch (Exception e) {
                    log.warn("Failed to auto-open portal browser: {}", e.getMessage());
                }
            }
            return result;
        });
    }

    public CompletableFuture<String> stopHotspot() {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Stopping Windows Mobile Hotspot...");
            String script = "try { "
                    + "  $connectionProfile = [Windows.Networking.Connectivity.NetworkInformation, Windows.Networking.Connectivity, ContentType=WindowsRuntime]::GetInternetConnectionProfile(); "
                    + "  if (-not $connectionProfile) { throw 'No internet connection profile found' } "
                    + "  $tetheringManager = [Windows.Networking.NetworkOperators.NetworkOperatorTetheringManager, Windows.Networking.NetworkOperators, ContentType=WindowsRuntime]::CreateFromConnectionProfile($connectionProfile); "
                    + "  if ($tetheringManager) { "
                    + "    $asyncOp = $tetheringManager.StopTetheringAsync(); "
                    + "    $timeout = 100; "
                    + "    while ($asyncOp.Status -eq 'Started' -and $timeout -gt 0) { Start-Sleep -Milliseconds 100; $timeout-- } "
                    + "    $result = $asyncOp.GetResults(); "
                    + "    if ($result.Status -eq 'Success') { 'Stopped' } else { 'Failed: ' + $result.Status } "
                    + "  } else { 'Failed to get tethering manager' } "
                    + "} catch { 'ERROR: ' + $_.Exception.Message }";
            return runPowerShell("-Command", script);
        });
    }

    public CompletableFuture<HotspotInfoResponse> getHotspotDetails() {
        return CompletableFuture.supplyAsync(() -> {
            // Check cache first
            HotspotInfoResponse cached = detailsCache.get();
            Instant lastUpdate = lastCacheUpdate.get();
            if (cached != null && Instant.now().minusMillis(CACHE_TTL_MS).isBefore(lastUpdate)) {
                return cached;
            }

            log.debug("Retrieving Windows Mobile Hotspot details via script...");

            // Run the verified external script
            String output = runPowerShell("-File", "scripts/get_details.ps1");

            if (output == null || output.isEmpty() || output.startsWith("ERROR:")) {
                log.warn("PowerShell script failed or returned empty: {}", output);
                return cached != null ? cached : new HotspotInfoResponse();
            }

            HotspotInfoResponse response = new HotspotInfoResponse();
            response.setConnectedDevices(new ArrayList<>());
            response.setGatewayIp("192.168.137.1");

            String[] lines = output.split("\\r?\\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty())
                    continue;

                if (line.startsWith("SSID:"))
                    response.setSsid(line.substring(5).trim());
                else if (line.startsWith("PASS:"))
                    response.setPassword(line.substring(5).trim());
                else if (line.startsWith("STATUS:"))
                    response.setStatus(line.substring(7).trim());
                else if (line.startsWith("HOSTIP:"))
                    response.setHostIp(line.substring(7).trim());
                else if (line.startsWith("UPSTREAM:"))
                    response.setUpstreamInterface(line.substring(9).trim());
                else if (line.startsWith("DEVICE|")) {
                    String[] parts = line.split("\\|");
                    HotspotDevice device = new HotspotDevice();
                    for (String part : parts) {
                        if (part.startsWith("MAC:"))
                            device.setMac(part.substring(4).trim());
                        else if (part.startsWith("IP:"))
                            device.setIp(part.substring(3).trim());
                        else if (part.startsWith("HOSTNAME:"))
                            device.setHostname(part.substring(9).trim());
                    }

                    if (device.getMac() != null && !device.getMac().equalsIgnoreCase(HOST_MAC)) {
                        String name = device.getHostname().toLowerCase();
                        device.setDeviceType(
                                name.contains("android") || name.contains("iphone") || name.contains("phone") ? "Mobile"
                                        : "Desktop");
                        response.getConnectedDevices().add(device);
                    }
                }
            }

            // Always add host device at index 0
            HotspotDevice host = new HotspotDevice();
            host.setHostname("This Machine (Host)");
            host.setIp(response.getHostIp());
            host.setMac(HOST_MAC);
            host.setDeviceType("Desktop");
            response.getConnectedDevices().add(0, host);

            // Update cache only if we got some real data
            if (response.getStatus() != null || response.getHostIp() != null) {
                detailsCache.set(response);
                lastCacheUpdate.set(Instant.now());
            }

            return response;
        });
    }

    public CompletableFuture<String> checkIcsStatus() {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Checking Windows Internet Connection Sharing (ICS) status...");
            String script = "$sharingManager = New-Object -ComObject HNetCfg.HNetShare; "
                    + "$shared = $false; "
                    + "$sharingManager.EnumEveryConnection | ForEach-Object { "
                    + "  $props = $sharingManager.NetConnectionProps($_); "
                    + "  $sharingCfg = $sharingManager.INetSharingConfigurationForINetConnection($_); "
                    + "  if ($sharingCfg.SharingEnabled -and ($sharingCfg.SharingType -eq 0)) { $shared = $true } "
                    + "}; "
                    + "if ($shared) { 'Enabled' } else { 'Disabled' }";
            return runPowerShell("-Command", script);
        });
    }

    public void logIcsConfigInstructions() {
        log.warn("=== INTERNET SHARING (ICS) REQUIRED ===");
        log.warn("Devices on the hotspot will have NO INTERNET unless you share your primary connection.");
        log.warn("1. Open 'Network Connections' (ncpa.cpl).");
        log.warn("2. Right-click your Ethernet or primary Wi-Fi adapter -> Properties.");
        log.warn("3. Go to 'Sharing' tab.");
        log.warn("4. Check 'Allow other network users to connect through this computer's Internet connection'.");
        log.warn("5. Select your Mobile Hotspot adapter from the dropdown.");
        log.warn("========================================");
    }

    private String runPowerShell(String... args) {
        try {
            java.util.List<String> command = new java.util.ArrayList<>();
            command.add("powershell.exe");
            command.add("-NoProfile");
            command.add("-NonInteractive");
            command.add("-ExecutionPolicy");
            command.add("Bypass");

            for (String arg : args) {
                command.add(arg);
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            StringBuilder output = new StringBuilder();
            // Use a separate thread or at least non-blocking check to reading output while
            // waiting
            // To keep it simple but safe, we'll read while p is alive, but with a timeout
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                long start = System.currentTimeMillis();
                while (p.isAlive() || reader.ready()) {
                    if (reader.ready()) {
                        line = reader.readLine();
                        if (line != null) {
                            output.append(line).append("\n");
                        }
                    } else {
                        Thread.sleep(50);
                    }

                    if (System.currentTimeMillis() - start > 45000) {
                        log.error("Reading PowerShell output timed out");
                        p.destroyForcibly();
                        break;
                    }
                }
            }

            p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            String result = output.toString().trim();
            log.debug("PowerShell Output Length: {}", result.length());
            return result;
        } catch (Exception e) {
            log.error("Failed to execute PowerShell script", e);
            return "ERROR: " + e.getMessage();
        }
    }
}
