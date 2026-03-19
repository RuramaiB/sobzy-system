package com.example.sobzybackend.service;

import com.example.sobzybackend.dtos.HotspotInfoResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NetworkAutomationService {

    private final HotspotService hotspotService;
    private final DeviceService deviceService;
    private final EmbeddedProxyServer embeddedProxyServer;
    private final EmbeddedDnsServer embeddedDnsServer;

    @Value("${app.network.automation.enabled:true}")
    private boolean enabled;

    @Value("${app.network.automation.ssid:Sobzy_Safe_Hotspot}")
    private String ssid;

    @Value("${app.network.automation.password:Sobzy12345}")
    private String password;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Process proxyProcess;
    private String hostIp = "192.168.137.1";

    @PostConstruct
    public void init() {
        if (enabled) {
            log.info("Initializing Pure Java Network Automation...");
            restartEngine();
        }
    }

    @PreDestroy
    public void cleanup() {
        stopEngine();
        executorService.shutdownNow();
    }

    public synchronized void restartEngine() {
        stopEngine();
        deviceService.markAllDevicesInactive();

        HotspotInfoResponse startingState = HotspotInfoResponse.builder()
                .ssid(ssid)
                .password(password)
                .hostIp(HOST_IP)
                .status("STARTING")
                .connectedDevices(new ArrayList<>())
                .build();
        hotspotService.updateStateFromEngine(startingState);

        executorService.submit(this::runMasterSetup);
    }

    public synchronized void stopEngine() {
        if (proxyProcess != null && proxyProcess.isAlive()) {
            log.info("Stopping Proxy Process...");
            proxyProcess.destroyForcibly();
        }

        // Stop Hotspot via PowerShell
        runPowerShell("Stop-Tethering",
                "[void][Windows.Networking.Connectivity.NetworkInformation, Windows.Networking.Connectivity, ContentType=WindowsRuntime]; "
                        +
                        "$profile = [Windows.Networking.Connectivity.NetworkInformation]::GetInternetConnectionProfile(); "
                        +
                        "$manager = [Windows.Networking.NetworkOperators.NetworkOperatorTetheringManager]::CreateFromConnectionProfile($profile); "
                        +
                        "$manager.StopTetheringAsync()");

        deviceService.markAllDevicesInactive();

        HotspotInfoResponse stoppedState = HotspotInfoResponse.builder()
                .status("STOPPED")
                .ssid(ssid)
                .password(password)
                .hostIp(HOST_IP)
                .connectedDevices(new ArrayList<>())
                .build();
        hotspotService.updateStateFromEngine(stoppedState);
    }

    private void runMasterSetup() {
        try {
            // 1. Start Hotspot
            log.info("Configuring Windows Hotspot via PowerShell...");
            String hotspotScript = String.format(
                    "[void][Windows.Networking.Connectivity.NetworkInformation, Windows.Networking.Connectivity, ContentType=WindowsRuntime]; "
                            +
                            "[void][Windows.Networking.NetworkOperators.NetworkOperatorTetheringManager, Windows.Networking.NetworkOperators, ContentType=WindowsRuntime]; "
                            +
                            "$profile = [Windows.Networking.Connectivity.NetworkInformation]::GetInternetConnectionProfile(); "
                            +
                            "$manager = [Windows.Networking.NetworkOperators.NetworkOperatorTetheringManager]::CreateFromConnectionProfile($profile); "
                            +
                            "$config = $manager.GetCurrentAccessPointConfiguration(); " +
                            "$config.Ssid = '%s'; $config.Passphrase = '%s'; " +
                            "$manager.ConfigureAccessPointAsync($config).GetResults(); " +
                            "$manager.StartTetheringAsync().GetResults(); ",
                    ssid, password);
            runPowerShell("Start-Hotspot", hotspotScript);

            // 2. Enable ICS
            log.info("Enabling ICS Sharing...");
            runPowerShell("Enable-ICS", "powershell.exe -File scripts/enable_ics.ps1");

            // 3. Configure Port Forwarding
            log.info("Configuring Port Redirection (80/443 -> 8080)...");
            executeCommand("netsh interface portproxy reset");
            executeCommand(String.format(
                    "netsh interface portproxy add v4tov4 listenport=80 listenaddress=%s connectport=8080 connectaddress=%s",
                    this.hostIp, this.hostIp));
            executeCommand(String.format(
                    "netsh interface portproxy add v4tov4 listenport=443 listenaddress=%s connectport=8080 connectaddress=%s",
                    this.hostIp, this.hostIp));

            // 4. Update Host IP dynamically if possible
            this.hostIp = detectHostIp();
            log.info("Detected Hotspot IP: {}", this.hostIp);

            // 5. Start DNS & Proxy
            log.info("Launching IWACS Pure Java Engine (DNS + Proxy)...");
            embeddedDnsServer.startDns(this.hostIp);
            embeddedProxyServer.startProxy(this.hostIp);

            HotspotInfoResponse runningState = HotspotInfoResponse.builder()
                    .ssid(ssid)
                    .password(password)
                    .hostIp(this.hostIp)
                    .status("RUNNING")
                    .connectedDevices(new ArrayList<>())
                    .build();
            hotspotService.updateStateFromEngine(runningState);

            log.info("IWACS Pure Java Engine is now LIVE.");

        } catch (Exception e) {
            log.error("Failed to run network automation sequence", e);
            updateFailureState("SETUP_FAILED");
        }
    }

    private String detectHostIp() {
        try {
            // Priority: 192.168.137.1 (Standard Hotspot), then 172.24.x.x (Some Windows
            // setups)
            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-Command",
                    "(Get-NetIPAddress | Where-Object { $_.AddressFamily -eq 'IPv4' -and ($_.IPAddress -like '172.24.*' -or $_.IPAddress -like '192.168.137.*') }).IPAddress | Select-Object -First 1");
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String ip = reader.readLine();
            if (ip != null && !ip.trim().isEmpty()) {
                return ip.trim();
            }
        } catch (Exception e) {
            log.warn("Failed to detect dynamic host IP, falling back to default: {}", e.getMessage());
        }
        return "192.168.137.1";
    }

    private void updateFailureState(String errorStatus) {
        HotspotInfoResponse failedState = HotspotInfoResponse.builder()
                .ssid(ssid)
                .password(password)
                .hostIp(HOST_IP)
                .status("FAILED (" + errorStatus + ")")
                .connectedDevices(new ArrayList<>())
                .build();
        hotspotService.updateStateFromEngine(failedState);
        deviceService.markAllDevicesInactive();
    }

    private void runPowerShell(String taskName, String script) {
        try {
            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
                    "-Command", script);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.waitFor();
            log.debug("PowerShell task {} completed", taskName);
        } catch (Exception e) {
            log.error("PowerShell task {} failed: {}", taskName, e.getMessage());
        }
    }

    private void executeCommand(String command) {
        try {
            Process p = Runtime.getRuntime().exec(command);
            p.waitFor();
        } catch (Exception e) {
            log.warn("Command execution failed: {}. Error: {}", command, e.getMessage());
        }
    }
}
