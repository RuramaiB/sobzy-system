package com.example.sobzybackend.service;

import com.example.sobzybackend.dtos.ClassificationRequest;
import com.example.sobzybackend.dtos.ClassificationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import smile.classification.RandomForest;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.data.vector.IntVector;
import jakarta.annotation.PostConstruct;
import java.io.*;

@Slf4j
@Service
public class ClassificationService {

    private RandomForest model;
    private final FeatureExtractionService featureExtractionService;
    private final String modelPath = "models/rf_model.ser";

    private static final java.util.Set<String> WHITELISTED_DOMAINS = java.util.Set.of(
            "google.com", "bing.com", "duckduckgo.com", "yahoo.com",
            "wikipedia.org", "britannica.com", "coursera.org", "udemy.com",
            "stackoverflow.com", "github.com", "gitlab.com", "bitbucket.org",
            "microsoft.com", "apple.com", "adobe.com", "npmmirror.com", "npmjs.com",
            "maven.org", "apache.org", "spring.io", "oracle.com", "java.com",
            "google.co.zw", "econet.co.zw", "netone.co.zw", "telecel.co.zw",
            "hit.ac.zw", "uz.ac.zw", "msu.ac.zw", "nust.ac.zw", "cut.ac.zw",
            "gstatic.com", "googleapis.com", "googleusercontent.com", "ggpht.com",
            "youtube.com", "ytimg.com", "googlevideo.com", "android.com", 
            "play.google.com"
    );

    public ClassificationService(FeatureExtractionService featureExtractionService) {
        this.featureExtractionService = featureExtractionService;
    }

    @PostConstruct
    public void init() {
        loadModel();
        if (model == null) {
            log.info("No pre-trained model found. Initializing skeleton model for training placeholder.");
            trainDummyModel();
        }
    }

    private static final String[] X_NAMES = { "x1", "x2", "x3", "x4", "x5", "x6", "x7", "x8", "x9", "x10" };

    public PredictionResult predict(String url) {
        String lowerUrl = url.toLowerCase();
        String domain = extractDomain(lowerUrl);

        // 1. Whitelist Check (Critical for Education/Productivity)
        if (WHITELISTED_DOMAINS.stream().anyMatch(d -> domain.equals(d) || domain.endsWith("." + d))) {
            return new PredictionResult("BENIGN", 1.0);
        }

        // 2. Strong Keyword / Heuristic Logic
        if (lowerUrl.contains("bet") || lowerUrl.contains("casino") || lowerUrl.contains("poker")
                || lowerUrl.contains("gamble") || lowerUrl.contains("slot") || lowerUrl.contains("jackpot")
                || lowerUrl.contains("bookmaker") || lowerUrl.contains("sportybet") || lowerUrl.contains("bet9ja")
                || lowerUrl.contains("1xbet")) {
            return new PredictionResult("GAMBLING", 0.98);
        }
        if (lowerUrl.contains("porn") || lowerUrl.contains("xxx") || lowerUrl.contains("sex")
                || lowerUrl.contains("nsfw") || lowerUrl.contains("onlyfans") || lowerUrl.contains("adult")
                || lowerUrl.contains("redtube") || lowerUrl.contains("pornhub") || lowerUrl.contains("xvideos")) {
            return new PredictionResult("ADULT_CONTENT", 0.99);
        }
        if (lowerUrl.contains("game") || lowerUrl.contains("playstation") || lowerUrl.contains("xbox")
                || lowerUrl.contains("steam") || lowerUrl.contains("roblox") || lowerUrl.contains("fortnite")
                || lowerUrl.contains("twitch.tv") || lowerUrl.contains("discord.com")) {
            return new PredictionResult("GAMING", 0.92);
        }
        if (lowerUrl.contains("edu") || lowerUrl.contains("school") || lowerUrl.contains("academy")
                || lowerUrl.contains("learn") || lowerUrl.contains("study") || lowerUrl.contains("research")
                || lowerUrl.contains("scholar") || lowerUrl.contains("ac.zw") || lowerUrl.contains("university")
                || lowerUrl.contains("library") || lowerUrl.contains("archive.org")) {
            return new PredictionResult("EDUCATION", 0.95);
        }

        // 3. ML Model fallback
        double[] features = featureExtractionService.extractUrlFeatures(url);
        if (model == null) {
            return new PredictionResult("BENIGN", 0.5);
        }

        // Convert double[] to Tuple for Smile 3.x DataFrame model
        DataFrame df = DataFrame.of(new double[][] { features }, X_NAMES);
        df = df.merge(IntVector.of("y", new int[] { 0 }));
        int prediction = model.predict(df.get(0));

        String category = getCategoryName(prediction);
        return new PredictionResult(category, 0.95);
    }

    public ClassificationResult classify(ClassificationRequest request) {
        PredictionResult prediction = predict(request.getUrl());

        // Refined decision logic: Only block strictly explicit or gambling content by default
        boolean isMalicious = "ADULT_CONTENT".equals(prediction.category()) ||
                "GAMBLING".equals(prediction.category());

        String reason = isMalicious ? "Site categorized as " + prediction.category() : "Allowed";

        return ClassificationResult.builder()
                .url(request.getUrl())
                .domain(extractDomain(request.getUrl()))
                .category(prediction.category())
                .confidence(prediction.confidence())
                .isAllowed(!isMalicious)
                .decision(isMalicious ? "BLOCK" : "ALLOW")
                .reason(reason)
                .processingTimeMs(0L)
                .build();
    }

    private String extractDomain(String url) {
        try {
            if (url.contains("://")) {
                return new java.net.URL(url).getHost();
            }
            return url.split("/")[0];
        } catch (Exception e) {
            return "unknown";
        }
    }

    public boolean isWhitelisted(String host) {
        if (host == null) return false;
        String lowerHost = host.toLowerCase();
        return WHITELISTED_DOMAINS.stream().anyMatch(lowerHost::contains);
    }

    private void loadModel() {
        File file = new File(modelPath);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                model = (RandomForest) ois.readObject();
                log.info("Random Forest model loaded successfully from {}", modelPath);
            } catch (Exception e) {
                log.error("Failed to load ML model: {}", e.getMessage());
            }
        }
    }

    public synchronized void saveModel() {
        File directory = new File("models");
        if (!directory.exists())
            directory.mkdirs();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(modelPath))) {
            oos.writeObject(model);
            log.info("Random Forest model saved to {}", modelPath);
        } catch (IOException e) {
            log.error("Failed to save ML model: {}", e.getMessage());
        }
    }

    private void trainDummyModel() {
        double[][] x = new double[40][10];
        int[] y = new int[40];
        for (int i = 0; i < 40; i++) {
            int category = i / 10; // 4 categories, 10 samples each
            y[i] = category;
            for (int j = 0; j < 10; j++) {
                x[i][j] = (category + 1) * (i + 1.2) + j;
            }
        }

        DataFrame df = DataFrame.of(x, X_NAMES);
        df = df.merge(IntVector.of("y", y));

        model = RandomForest.fit(Formula.lhs("y"), df);
        log.info("More robust Random Forest dummy model trained (40 samples).");
    }

    public String getCategoryName(int index) {
        switch (index) {
            case 0:
                return "BENIGN";
            case 1:
                return "ADULT_CONTENT";
            case 2:
                return "RESEARCH";
            case 3:
                return "GAMING";
            case 4:
                return "SOCIAL_MEDIA";
            case 5:
                return "SECURITY_EVASION";
            case 6:
                return "MUSIC";
            case 7:
                return "GAMBLING";
            case 8:
                return "EDUCATION";
            default:
                return "OTHER";
        }
    }

    public static record PredictionResult(String category, double confidence) {
    }
}
