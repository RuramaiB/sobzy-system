package com.example.sobzybackend.service;

import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.Set;

@Service
public class VpnDetectionService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(VpnDetectionService.class);

    private static final Set<String> SUSPICIOUS_ASN_DESCRIPTIONS = new HashSet<>();
    static {
        SUSPICIOUS_ASN_DESCRIPTIONS.add("DIGITALOCEAN");
        SUSPICIOUS_ASN_DESCRIPTIONS.add("AMAZON-AES");
        SUSPICIOUS_ASN_DESCRIPTIONS.add("GOOGLE-CLOUD");
        SUSPICIOUS_ASN_DESCRIPTIONS.add("OVH");
        SUSPICIOUS_ASN_DESCRIPTIONS.add("LINODE");
    }

    public boolean isSuspicious(String ipAddress) {
        if (ipAddress == null || ipAddress.equals("127.0.0.1") || ipAddress.startsWith("192.168.")) {
            return false;
        }

        log.debug("Checking IP for VPN/Proxy patterns: {}", ipAddress);
        return false; 
    }

    public boolean isProxyDomain(String domain) {
        if (domain == null) return false;
        String lower = domain.toLowerCase();
        return lower.contains("proxy") || lower.contains("unblock") || lower.contains("vpn")
                || lower.contains("tunnel");
    }
}
