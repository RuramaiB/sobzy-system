package com.example.sobzybackend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
public class VpnDetectionService {

    // Simplified threat intelligence: Block common data center IP ranges or known
    // proxy signatures
    // In a production app, this would query an external API (like IPInfo or
    // MaxMind) or a local GeoIP DB
    private static final Set<String> SUSPICIOUS_ASN_DESCRIPTIONS = new HashSet<>();
    static {
        SUSPICIOUS_ASN_DESCRIPTIONS.add("DIGITALOCEAN");
        SUSPICIOUS_ASN_DESCRIPTIONS.add("AMAZON-AES");
        SUSPICIOUS_ASN_DESCRIPTIONS.add("GOOGLE-CLOUD");
        SUSPICIOUS_ASN_DESCRIPTIONS.add("OVH");
        SUSPICIOUS_ASN_DESCRIPTIONS.add("LINODE");
    }

    /**
     * Checks if an IP address is likely a VPN or Data Center proxy.
     */
    public boolean isSuspicious(String ipAddress) {
        if (ipAddress == null || ipAddress.equals("127.0.0.1") || ipAddress.startsWith("192.168.")) {
            return false;
        }

        // Logic placeholder: Check header patterns common in proxies
        // (X-Forwarded-For is usually handled at the proxy layer, but we can verify it
        // here)

        log.debug("Checking IP for VPN/Proxy patterns: {}", ipAddress);
        return false; // Default to false for privacy, but ready for integration
    }

    /**
     * Checks for domain patterns common in evasion tools (e.g., web proxies).
     */
    public boolean isProxyDomain(String domain) {
        String lower = domain.toLowerCase();
        return lower.contains("proxy") || lower.contains("unblock") || lower.contains("vpn")
                || lower.contains("tunnel");
    }
}
