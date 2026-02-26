package com.example.sobzybackend.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;

/**
 * Service to manage the lifecycle of the Traffic Monitoring Proxy (mitmproxy).
 * Automatically starts the proxy when the backend bootstraps and shuts it down
 * on exit.
 */
@Slf4j
@Service
public class ProxyManagementService {

    private Process proxyProcess;

    public void startProxy() {
        if (proxyProcess != null && proxyProcess.isAlive()) {
            log.info("Proxy is already running.");
            return;
        }

        log.info("Starting Traffic Monitoring Proxy via run_proxy.ps1...");

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-ExecutionPolicy", "Bypass",
                    "-File", "run_proxy.ps1");

            pb.redirectErrorStream(true);
            proxyProcess = pb.start();

            // Read the output in a separate thread to avoid blocking and to log it to Java
            // console
            CompletableFuture.runAsync(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(proxyProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("[Proxy] {}", line);
                    }
                } catch (Exception e) {
                    log.error("Error reading proxy output: {}", e.getMessage());
                }
            });

            log.info("Proxy process started successfully.");
        } catch (Exception e) {
            log.error("Failed to start Proxy process: {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void stopProxy() {
        if (proxyProcess != null && proxyProcess.isAlive()) {
            log.info("Shutting down Traffic Monitoring Proxy...");
            proxyProcess.destroy();
            try {
                // Give it a moment to shut down gracefully before forcing
                if (!proxyProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    log.warn("Proxy did not shut down gracefully. Forcing termination...");
                    proxyProcess.destroyForcibly();
                }
                log.info("Proxy shut down successfully.");
            } catch (InterruptedException e) {
                log.error("Interrupted while waiting for proxy shutdown", e);
                proxyProcess.destroyForcibly();
                Thread.currentThread().interrupt();
            }
        }
    }
}
