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

        // 1. Strong Keyword / Heuristic Logic (Overriding Dummy Model for specific
        // policies)
        if (lowerUrl.contains("bet") || lowerUrl.contains("casino") || lowerUrl.contains("poker")
                || lowerUrl.contains("gamble") || lowerUrl.contains("slot")) {
            return new PredictionResult("GAMBLING", 0.98);
        }
        if (lowerUrl.contains("porn") || lowerUrl.contains("xxx") || lowerUrl.contains("sex")
                || lowerUrl.contains("nsfw") || lowerUrl.contains("onlyfans") || lowerUrl.contains("adult")) {
            return new PredictionResult("ADULT_CONTENT", 0.99);
        }
        if (lowerUrl.contains("game") || lowerUrl.contains("playstation") || lowerUrl.contains("xbox")
                || lowerUrl.contains("steam") || lowerUrl.contains("roblox") || lowerUrl.contains("fortnite")) {
            return new PredictionResult("GAMING", 0.92);
        }
        if (lowerUrl.contains("edu") || lowerUrl.contains("school") || lowerUrl.contains("academy")
                || lowerUrl.contains("learn") || lowerUrl.contains("study") || lowerUrl.contains("research")) {
            return new PredictionResult("EDUCATION", 0.95);
        }

        // 2. ML Model fallback
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

        // Refined decision logic for educational/music leniency
        boolean isMalicious = "ADULT_CONTENT".equals(prediction.category()) ||
                "GAMBLING".equals(prediction.category()) ||
                "GAMING".equals(prediction.category());

        return ClassificationResult.builder()
                .url(request.getUrl())
                .domain(extractDomain(request.getUrl()))
                .category(prediction.category())
                .confidence(prediction.confidence())
                .isAllowed(!isMalicious)
                .decision(isMalicious ? "BLOCK" : "ALLOW")
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
