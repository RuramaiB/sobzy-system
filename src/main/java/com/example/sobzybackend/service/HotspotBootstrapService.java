package com.example.sobzybackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotspotBootstrapService implements ApplicationRunner {

    private final HotspotService hotspotService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("=== Hotspot Environment Validation ===");

        // 1. Check Scripts (Modified for Pure Java)
        checkScripts();

        // 2. Dry-run PowerShell
        checkPowerShell();

        log.info("Hotspot Environment Validation Complete.");
    }



    private void checkScripts() {
        // No Python scripts needed for Pure Java implementation
        log.info("Pure Java implementation: skipping legacy component checks.");
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
