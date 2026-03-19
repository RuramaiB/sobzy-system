package com.example.sobzybackend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.net.URL;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@Slf4j
@Service
public class FeatureExtractionService {

    private static final Pattern IP_PATTERN = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");
    private static final String[] SUSPICIOUS_KEYWORDS = { "login", "verify", "account", "secure", "bank", "update",
            "signin", "wp-admin", "wp-login" };
    private static final String[] HIGH_RISK_TLDS = { ".tk", ".xyz", ".top", ".rocks", ".win", ".bid", ".gdn", ".club" };

    /**
     * Extracts numerical features from a URL for ML classification.
     * Returns a double array of features.
     */
    public double[] extractUrlFeatures(String urlString) {
        if (urlString == null || urlString.isEmpty()) {
            return new double[10]; // Return empty/default features
        }

        try {
            String host = extractHost(urlString);

            double[] features = new double[10];
            features[0] = urlString.length(); // Total URL length
            features[1] = host.length(); // Host length
            features[2] = countOccurrences(host, '.'); // Number of dots
            features[3] = countOccurrences(urlString, '/') - 2; // Number of path segments (approx)
            features[4] = containsIp(host) ? 1.0 : 0.0; // Use of IP address
            features[5] = countSpecialChars(urlString); // Special char count
            features[6] = calculateEntropy(host); // Shannon Entropy of host
            features[7] = containsSuspiciousKeyword(urlString) ? 1.0 : 0.0;
            features[8] = isHighRiskTld(host) ? 1.0 : 0.0;
            features[9] = urlString.startsWith("https") ? 0.0 : 1.0; // Non-SSL risk

            return features;
        } catch (Exception e) {
            log.warn("Error extracting features from URL: {} - {}", urlString, e.getMessage());
            return new double[10];
        }
    }

    private String extractHost(String urlString) {
        try {
            if (urlString.contains("://")) {
                return new URL(urlString).getHost();
            } else {
                return urlString.split("/")[0];
            }
        } catch (Exception e) {
            return urlString;
        }
    }

    private int countOccurrences(String text, char target) {
        return (int) text.chars().filter(ch -> ch == target).count();
    }

    private boolean containsIp(String host) {
        return IP_PATTERN.matcher(host).matches();
    }

    private int countSpecialChars(String text) {
        String special = "-_?=&%";
        return (int) text.chars().filter(ch -> special.indexOf(ch) != -1).count();
    }

    private double calculateEntropy(String text) {
        if (text == null || text.isEmpty())
            return 0.0;
        int[] frequencies = new int[256];
        text.chars().forEach(c -> {
            if (c < 256)
                frequencies[c]++;
        });

        double entropy = 0.0;
        double length = text.length();
        for (int freq : frequencies) {
            if (freq > 0) {
                double probability = freq / length;
                entropy -= probability * (Math.log(probability) / Math.log(2));
            }
        }
        return entropy;
    }

    private boolean containsSuspiciousKeyword(String url) {
        String lowerUrl = url.toLowerCase();
        for (String keyword : SUSPICIOUS_KEYWORDS) {
            if (lowerUrl.contains(keyword))
                return true;
        }
        return false;
    }

    private boolean isHighRiskTld(String host) {
        String lowerHost = host.toLowerCase();
        for (String tld : HIGH_RISK_TLDS) {
            if (lowerHost.endsWith(tld))
                return true;
        }
        return false;
    }
}
