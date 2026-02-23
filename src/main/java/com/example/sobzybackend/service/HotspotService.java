package com.example.sobzybackend.service;

import com.example.sobzybackend.dtos.HotspotInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class HotspotService {

    public CompletableFuture<String> startHotspot() {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Attempting to start Windows Mobile Hotspot...");
            String script = "$connectionProfile = [Windows.Networking.Connectivity.NetworkInformation, Windows.Networking.Connectivity, ContentType=WindowsRuntime]::GetInternetConnectionProfile(); "
                    + "$tetheringManager = [Windows.Networking.NetworkOperators.NetworkOperatorTetheringManager, Windows.Networking.NetworkOperators, ContentType=WindowsRuntime]::CreateFromConnectionProfile($connectionProfile); "
                    + "if ($tetheringManager) { "
                    + "  $asyncOp = $tetheringManager.StartTetheringAsync(); "
                    + "  while ($asyncOp.Status -eq 'Started') { Start-Sleep -Milliseconds 100 } "
                    + "  $result = $asyncOp.GetResults(); "
                    + "  if ($result.Status -eq 'Success') { 'Success' } else { 'Failed: ' + $result.Status } "
                    + "} else { 'Failed to get tethering manager' }";
            String result = runPowerShell(script);

            if ("Success".equals(result)) {
                log.info("Hotspot started. Automating network setup...");
                // 1. Enable Internet Connection Sharing (ICS)
                runPowerShell("./scripts/enable_ics.ps1");

                // 2. Open Portal on host machine browser as requested
                try {
                    String hostIp = runPowerShell(
                            "(Get-NetIPAddress | Where-Object {$_.AddressFamily -eq 'IPv4' -and $_.InterfaceAlias -notlike 'Loopback*'}).IPAddress[0]");
                    if (hostIp != null && !hostIp.isEmpty()) {
                        log.info("Opening portal for host machine at http://{}:3000/login", hostIp);
                        runPowerShell("Start-Process 'http://" + hostIp + ":3000/login'");
                    }
                } catch (Exception e) {
                    log.warn("Failed to auto-open portal browser: {}", e.getMessage());
                }
            }
            return result;
        });
    }

    public CompletableFuture<String> stopHotspot() {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Stopping Windows Mobile Hotspot...");
            String script = "$connectionProfile = [Windows.Networking.Connectivity.NetworkInformation, Windows.Networking.Connectivity, ContentType=WindowsRuntime]::GetInternetConnectionProfile(); "
                    + "$tetheringManager = [Windows.Networking.NetworkOperators.NetworkOperatorTetheringManager, Windows.Networking.NetworkOperators, ContentType=WindowsRuntime]::CreateFromConnectionProfile($connectionProfile); "
                    + "if ($tetheringManager) { "
                    + "  $asyncOp = $tetheringManager.StopTetheringAsync(); "
                    + "  while ($asyncOp.Status -eq 'Started') { Start-Sleep -Milliseconds 100 } "
                    + "  $result = $asyncOp.GetResults(); "
                    + "  if ($result.Status -eq 'Success') { 'Stopped' } else { 'Failed: ' + $result.Status } "
                    + "} else { 'Failed to get tethering manager' }";
            return runPowerShell(script);
        });
    }

    public CompletableFuture<HotspotInfoResponse> getHotspotDetails() {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Retrieving Windows Mobile Hotspot details...");
            String script = "$connectionProfile = [Windows.Networking.Connectivity.NetworkInformation, Windows.Networking.Connectivity, ContentType=WindowsRuntime]::GetInternetConnectionProfile(); "
                    + "$tetheringManager = [Windows.Networking.NetworkOperators.NetworkOperatorTetheringManager, Windows.Networking.NetworkOperators, ContentType=WindowsRuntime]::CreateFromConnectionProfile($connectionProfile); "
                    + "if ($tetheringManager) { "
                    + "  $config = $tetheringManager.GetCurrentAccessPointConfiguration(); "
                    + "  $status = $tetheringManager.TetheringOperationalState; "
                    + "  Write-Output \"SSID:$($config.Ssid)\"; "
                    + "  Write-Output \"PASS:$($config.Passphrase)\"; "
                    + "  Write-Output \"STATUS:$status\"; "
                    + "} else { Write-Output 'ERROR:Failed to get tethering manager' }";

            String output = runPowerShell(script);
            HotspotInfoResponse response = new HotspotInfoResponse();

            if (output != null && !output.isEmpty()) {
                String[] lines = output.split("\\r?\\n");
                for (String line : lines) {
                    if (line.startsWith("SSID:"))
                        response.setSsid(line.substring(5).trim());
                    else if (line.startsWith("PASS:"))
                        response.setPassword(line.substring(5).trim());
                    else if (line.startsWith("STATUS:"))
                        response.setStatus(line.substring(7).trim());
                }
            }
            return response;
        });
    }

    public CompletableFuture<String> checkIcsStatus() {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Checking Windows Internet Connection Sharing (ICS) status...");
            String script = "$sharingManager = New-Object -ComObject HNetCfg.HNetShare; "
                    + "$shared = $false; "
                    + "$sharingManager.EnumEveryConnection | ForEach-Object { "
                    + "  $props = $sharingManager.NetConnectionProps($_); "
                    + "  $sharingCfg = $sharingManager.INetSharingConfigurationForINetConnection($_); "
                    + "  if ($sharingCfg.SharingEnabled -and ($sharingCfg.SharingType -eq 0)) { $shared = $true } "
                    + "}; "
                    + "if ($shared) { 'Enabled' } else { 'Disabled' }";
            return runPowerShell(script);
        });
    }

    public void logIcsConfigInstructions() {
        log.warn("=== INTERNET SHARING (ICS) REQUIRED ===");
        log.warn("Devices on the hotspot will have NO INTERNET unless you share your primary connection.");
        log.warn("1. Open 'Network Connections' (ncpa.cpl).");
        log.warn("2. Right-click your Ethernet or primary Wi-Fi adapter -> Properties.");
        log.warn("3. Go to 'Sharing' tab.");
        log.warn("4. Check 'Allow other network users to connect through this computer's Internet connection'.");
        log.warn("5. Select your Mobile Hotspot adapter from the dropdown.");
        log.warn("========================================");
    }

    private String runPowerShell(String script) {
        try {
            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-Command",
                    script);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            p.waitFor();
            log.debug("PowerShell Output: {}", output);
            return output.toString().trim();
        } catch (Exception e) {
            log.error("Failed to execute PowerShell script", e);
            return "ERROR: " + e.getMessage();
        }
    }
}
