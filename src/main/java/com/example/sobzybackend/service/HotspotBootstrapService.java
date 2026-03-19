package com.example.sobzybackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotspotBootstrapService implements ApplicationRunner {

    private final HotspotService hotspotService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("=== Hotspot Environment Validation ===");

        // 1. Check Python
        checkPython();

        // 2. Check Scripts
        checkScripts();

        // 3. Dry-run PowerShell
        checkPowerShell();

        log.info("Hotspot Environment Validation Complete.");
    }

    private void checkPython() {
        try {
            Process p = Runtime.getRuntime().exec("python --version");
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            if (line == null) {
                // Try python3
                p = Runtime.getRuntime().exec("python3 --version");
                reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                line = reader.readLine();
            }
            if (line != null) {
                log.info("Python version: {}", line);
            } else {
                log.warn("Python not found in PATH. Please ensure Python is installed.");
            }
        } catch (Exception e) {
            log.warn("Failed to check Python version: {}", e.getMessage());
        }
    }

    private void checkScripts() {
        String[] requiredScripts = { "src/main/python/mitm_addon.py" };
        for (String script : requiredScripts) {
            File file = new File(script);
            if (file.exists()) {
                log.info("Found component: {}", script);
            } else {
                log.error("CRITICAL ERROR: Component NOT found: {}", script);
            }
        }
    }

    private void checkPowerShell() {
        try {
            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-Command",
                    "Write-Output 'PowerShell is functional'");
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            if ("PowerShell is functional".equals(line)) {
                log.info("PowerShell dry-run successful.");
            } else {
                log.warn("PowerShell dry-run returned unexpected output: {}", line);
            }
            p.waitFor();
        } catch (Exception e) {
            log.error("PowerShell dry-run failed: {}", e.getMessage());
        }
    }
}
