package com.example.sobzybackend.service;

import com.example.sobzybackend.dtos.ClassificationRequest;
import com.example.sobzybackend.dtos.ClassificationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.*;

/**
 * Service for bridging Java application with Python ML classifier
 * Executes Python scripts and parses results
 */
@Slf4j
@Service
public class PythonBridgeService {

    @Value("${app.ml.python-path}")
    private String pythonPath;

    @Value("${app.ml.script-path}")
    private String scriptPath;

    @Value("${app.ml.classifier-script}")
    private String classifierScript;

    @Value("${app.ml.timeout-ms}")
    private long timeout;

    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;

    public PythonBridgeService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.executorService = Executors.newFixedThreadPool(5);
    }

    /**
     * Classify URL synchronously
     */
    public ClassificationResult classify(ClassificationRequest request) {
        log.debug("Classifying URL: {}", request.getUrl());

        long startTime = System.currentTimeMillis();

        try {
            String result = executePythonScript(request);
            ClassificationResult classificationResult = objectMapper.readValue(result, ClassificationResult.class);

            long processingTime = System.currentTimeMillis() - startTime;
            classificationResult.setProcessingTimeMs(processingTime);

            log.info("Classification complete - URL: {}, Category: {}, Confidence: {}, Time: {}ms",
                    request.getUrl(),
                    classificationResult.getCategory(),
                    classificationResult.getConfidence(),
                    processingTime);

            return classificationResult;

        } catch (TimeoutException e) {
            log.error("Classification timeout for URL: {}", request.getUrl());
            return createFallbackResult(request.getUrl(), "Classification timeout");
        } catch (Exception e) {
            log.error("Error during classification: {}", e.getMessage(), e);
            return createFallbackResult(request.getUrl(), "Classification error: " + e.getMessage());
        }
    }

    /**
     * Classify URL asynchronously
     */
    @Async("mlExecutor")
    public CompletableFuture<ClassificationResult> classifyAsync(ClassificationRequest request) {
        return CompletableFuture.supplyAsync(() -> classify(request), executorService);
    }

    /**
     * Execute Python script with timeout
     */
    private String executePythonScript(ClassificationRequest request)
            throws IOException, InterruptedException, TimeoutException, ExecutionException {

        String scriptFile = scriptPath + "/" + classifierScript;

        ProcessBuilder processBuilder = new ProcessBuilder(
                pythonPath,
                scriptFile,
                request.getUrl(),
                request.getContent() != null ? request.getContent() : "");

        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        // Read output with timeout
        Future<String> future = executorService.submit(() -> {
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            return output.toString();
        });

        try {
            String output = future.get(timeout, TimeUnit.MILLISECONDS);
            boolean completed = process.waitFor(timeout, TimeUnit.MILLISECONDS);

            if (!completed) {
                process.destroyForcibly();
                throw new TimeoutException("Python process timed out");
            }

            if (process.exitValue() != 0) {
                log.error("Python script failed with exit code: {}, Output: {}",
                        process.exitValue(), output);
                throw new IOException("Python script failed with exit code: " + process.exitValue());
            }

            return output.trim();

        } catch (TimeoutException | ExecutionException e) {
            process.destroyForcibly();
            throw e;
        }
    }

    /**
     * Create fallback result when classification fails
     */
    private ClassificationResult createFallbackResult(String url, String reason) {
        log.warn("Using fallback classification for URL: {} - Reason: {}", url, reason);

        ClassificationResult result = new ClassificationResult();
        result.setUrl(url);
        result.setDomain(extractDomain(url));
        result.setCategory("UNKNOWN");
        result.setConfidence(0.50);
        result.setIsAllowed(true);
        result.setRiskLevel("LOW");
        result.setDecision("ALLOW");
        result.setReason("Classification unavailable - " + reason);
        result.setError(true);

        return result;
    }

    /**
     * Extract domain from URL
     */
    private String extractDomain(String url) {
        try {
            java.net.URL parsedUrl = new java.net.URL(url);
            return parsedUrl.getHost();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Test Python connectivity and attempt auto-correction if it fails
     */
    public boolean testPythonConnection() {
        if (checkPython(pythonPath)) {
            return true;
        }

        log.warn("Configured Python path '{}' failed. Searching for Python...", pythonPath);
        String[] commonPaths = { "python", "python3", "py" };
        for (String path : commonPaths) {
            if (checkPython(path)) {
                log.info("Auto-detected Python at: {}", path);
                this.pythonPath = path;
                return true;
            }
        }

        return false;
    }

    private boolean checkPython(String path) {
        try {
            ProcessBuilder pb = new ProcessBuilder(path, "--version");
            Process process = pb.start();
            return process.waitFor(3, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Shutdown executor service
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
