package com.example.sobzybackend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class PortalService {

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
            mac = getMacFromArp(ipAddress);
            if (mac != null)
                ipToMacMap.put(ipAddress, mac);
        }
        return mac;
    }

    public void removeIp(String ipAddress) {
        log.info("Removing IP address from authenticated sessions: {}", ipAddress);
        authenticatedIps.remove(ipAddress);
        ipToMacMap.remove(ipAddress);
    }

    public Map<String, String> getAuthenticatedIps() {
        return authenticatedIps;
    }

    /**
     * Helper to get MAC address from ARP table on Windows
     */
    private String getMacFromArp(String ipAddress) {
        try {
            ProcessBuilder pb = new ProcessBuilder("arp", "-a", ipAddress);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            // Pattern for MAC address on Windows (e.g., 00-11-22-33-44-55)
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
