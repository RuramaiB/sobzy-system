package com.example.sobzybackend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@org.springframework.stereotype.Service
public class PortalService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PortalService.class);

    // Store authenticated IPs mapped to Usernames
    private final Map<String, String> authenticatedIps = new ConcurrentHashMap<>();
    // Store IP to MAC mapping for device identification
    private final Map<String, String> ipToMacMap = new ConcurrentHashMap<>();

    public void authenticateIp(String ipAddress, String username) {
        log.info("Authenticating IP {} for user {}", ipAddress, username);
        authenticatedIps.put(ipAddress, username);

        // Try to capture MAC address when authenticating
        String mac = getMacFromArp(ipAddress);
        if (mac != null) {
            ipToMacMap.put(ipAddress, mac);
            log.info("Linked IP {} to MAC {}", ipAddress, mac);
        }
    }

    public boolean isIpAuthenticated(String ipAddress) {
        return authenticatedIps.containsKey(ipAddress);
    }

    public String getUsernameForIp(String ipAddress) {
        return authenticatedIps.get(ipAddress);
    }

    public String getMacForIp(String ipAddress) {
        String mac = ipToMacMap.get(ipAddress);
        if (mac == null) {
            // Background discovery to avoid blocking the proxy pipeline
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                String discoveredMac = discoverMacAddress(ipAddress);
                if (discoveredMac != null) {
                    ipToMacMap.put(ipAddress, discoveredMac);
                    log.info("Async discovery: Linked IP {} to MAC {}", ipAddress, discoveredMac);
                }
            });
            return null; // Return null for now, next request will have it if discovered
        }
        return mac;
    }

    public void removeIp(String ipAddress) {
        log.info("Removing IP address from authenticated sessions: {}", ipAddress);
        authenticatedIps.remove(ipAddress);
        ipToMacMap.remove(ipAddress);
    }

    private String discoverMacAddress(String ipAddress) {
        return getMacFromArp(ipAddress);
    }

    public Map<String, String> getAuthenticatedIps() {
        return authenticatedIps;
    }

    /**
     * Helper to get MAC address from ARP table on Windows
     * Uses PowerShell for better reliability than 'arp -a'
     */
    private String getMacFromArp(String ipAddress) {
        try {
            // Ping first to ensure the entry is in the ARP table
            try {
                new ProcessBuilder("ping", "-n", "1", "-w", "500", ipAddress).start().waitFor();
            } catch (Exception pe) {
                // Ignore ping failures
            }

            // Use Get-NetNeighbor for robust MAC discovery
            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-Command",
                    "Get-NetNeighbor -IPAddress " + ipAddress + " | Select-Object -ExpandProperty LinkLayerAddress");
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String mac = reader.readLine();
            
            if (mac != null && !mac.trim().isEmpty()) {
                return mac.trim().replace("-", ":").toUpperCase();
            }
            
            // Fallback to traditional arp -a if PowerShell fails or returns empty
            pb = new ProcessBuilder("arp", "-a", ipAddress);
            process = pb.start();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            Pattern macPattern = Pattern.compile("([0-9a-fA-F]{2}[:-]){5}([0-9a-fA-F]{2})");

            while ((line = reader.readLine()) != null) {
                if (line.contains(ipAddress)) {
                    Matcher matcher = macPattern.matcher(line);
                    if (matcher.find()) {
                        return matcher.group().replace("-", ":").toUpperCase();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get MAC for IP {}: {}", ipAddress, e.getMessage());
        }
        return null;
    }
}
