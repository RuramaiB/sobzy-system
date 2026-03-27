package com.example.sobzybackend.service;

import com.example.sobzybackend.dtos.HotspotInfoResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class NetworkAutomationService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NetworkAutomationService.class);

    private final HotspotService hotspotService;
    private final DeviceService deviceService;
    private final EmbeddedProxyServer embeddedProxyServer;
    private final EmbeddedDnsServer embeddedDnsServer;

    public NetworkAutomationService(HotspotService hotspotService, DeviceService deviceService, 
                                   EmbeddedProxyServer embeddedProxyServer, EmbeddedDnsServer embeddedDnsServer) {
        this.hotspotService = hotspotService;
        this.deviceService = deviceService;
        this.embeddedProxyServer = embeddedProxyServer;
        this.embeddedDnsServer = embeddedDnsServer;
    }

    @Value("${app.network.automation.enabled:true}")
    private boolean enabled;

    @Value("${app.network.automation.ssid:Sobzy_Safe_Hotspot}")
    private String ssid;

    @Value("${app.network.automation.password:Sobzy12345}")
    private String password;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Process proxyProcess;
    private String hostIp = "192.168.137.1"; // Default fallback

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
                .hostIp(hostIp)
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
                .hostIp(hostIp)
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
                            "$configAsync = $manager.ConfigureAccessPointAsync($config); " +
                            "while ($configAsync.Status -eq 'Started') { Start-Sleep -Milliseconds 100 }; " +
                            "$startAsync = $manager.StartTetheringAsync(); " +
                            "while ($startAsync.Status -eq 'Started') { Start-Sleep -Milliseconds 100 }; ",
                    ssid, password);
            runPowerShell("Start-Hotspot", hotspotScript);

            // 2. Enable ICS & Detect IP
            log.info("Enabling ICS Sharing and detecting Host IP...");
            String icsPath = System.getProperty("user.dir") + "\\scripts\\enable_ics.ps1";
            String detectedIp = runIcsAndGetIp(icsPath);
            
            if (detectedIp != null) {
                this.hostIp = detectedIp;
                log.info("DYNAMIC_IP_SUCCESS: Detected Hotspot IP: {}", this.hostIp);
            } else {
                log.warn("DYNAMIC_IP_FAILURE: Could not detect IP from script. Falling back to {}", this.hostIp);
            }

            // 3. Configure Port Forwarding using the CORRECT IP
            log.info("Configuring Port Redirection (80/443 -> 8080) for IP: {}...", this.hostIp);
            executeCommand("netsh interface portproxy reset");
            executeCommand(String.format(
                    "netsh interface portproxy add v4tov4 listenport=80 listenaddress=%s connectport=8080 connectaddress=%s",
                    this.hostIp, this.hostIp));
            executeCommand(String.format(
                    "netsh interface portproxy add v4tov4 listenport=443 listenaddress=%s connectport=8080 connectaddress=%s",
                    this.hostIp, this.hostIp));

            // 4. Handle Port 53 (DNS) Conflict Check
            // We check if another process is holding Port 53 on the host IP (often SharedAccess).
            // This prevents "Address already in use" errors during DNS server startup.
            checkPortConflict(this.hostIp, 53);

            // 4. Start DNS & Proxy
            log.info("Launching IWACS Pure Java Engine (DNS + Proxy) on {}...", this.hostIp);
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

    private String runIcsAndGetIp(String scriptPath) {
        String resultIp = null;
        try {
            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
                    "-Command", "& '" + scriptPath + "'");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[ICS-SCRIPT] {}", line);
                if (line.contains("[HOST_IP]")) {
                    String[] parts = line.split("\\[HOST_IP\\]");
                    if (parts.length > 1) {
                        resultIp = parts[1].trim();
                    }
                }
            }
            p.waitFor();
        } catch (Exception e) {
            log.error("Failed to execute ICS script and get IP", e);
        }

        // Use our robust detection as a fallback if the script didn't output an IP
        if (resultIp == null) {
            log.info("Script did not output [HOST_IP]. Attempting fallback detection...");
            resultIp = detectHostIp();
        }

        return resultIp;
    }

    private String detectHostIp() {
        log.info("Attempting to detect the active Hotspot (ICS) gateway IP...");
        try {
            // This query finds the IPv4 address of the adapter specifically used for Hosted Networks/Hotspots.
            // It looks for "Wi-Fi Direct Virtual Adapter" which is the standard Windows name,
            // or any adapter that has been assigned 192.168.137.1 (the common ICS default).
            String cmd = "(Get-NetIPAddress | Where-Object { " +
                         "$_.AddressFamily -eq 'IPv4' -and " +
                         "($_.InterfaceDescription -like '*Wi-Fi Direct Virtual Adapter*' -or " +
                         " $_.InterfaceAlias -like '*Local Area Connection* *' -or " +
                         " $_.IPAddress -like '192.168.137.*') " +
                         "}).IPAddress | Select-Object -First 1";
            
            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-Command", cmd);
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String ip = reader.readLine();
            
            if (ip != null && !ip.trim().isEmpty()) {
                log.info("Successfully detected Hotspot IP: {}", ip.trim());
                return ip.trim();
            }
        } catch (Exception e) {
            log.warn("Robust IP detection failed: {}", e.getMessage());
        }
        
        log.warn("Could not detect dynamic IP. Using 192.168.137.1 as a last-resort legacy fallback.");
        return "192.168.137.1";
    }

    private void updateFailureState(String errorStatus) {
        HotspotInfoResponse failedState = HotspotInfoResponse.builder()
                .ssid(ssid)
                .password(password)
                .hostIp(hostIp)
                .status("FAILED (" + errorStatus + ")")
                .connectedDevices(new ArrayList<>())
                .build();
        hotspotService.updateStateFromEngine(failedState);
        deviceService.markAllDevicesInactive();
    }

    private void checkPortConflict(String ip, int port) {
        log.info("Checking for potential port conflict on {}:{}...", ip, port);
        try {
            // Using a DatagramSocket test to see if UDP port is occupied (DNS is UDP)
            try (java.net.DatagramSocket ds = new java.net.DatagramSocket(port, java.net.InetAddress.getByName(ip))) {
                // If we reach here, port is free
            } catch (java.io.IOException e) {
                log.warn("PORT_CONFLICT ALERT: UDP Port {}:{} is already occupied! " +
                         "This is likely the Windows DNS Relay (ICS). " +
                         "The upcoming DNS Hijacker startup may fail.", ip, port);
                
                // Try to find the process holding the port via netstat (informative only)
                String netstat = runPowerShell("find-port-holder", 
                    String.format("netstat -ano -p udp | findstr :%d | findstr %s", port, ip));
                log.info("Current UDP port holder info:\n{}", netstat);
            }
        } catch (Exception e) {
             log.debug("Conflict check failed: {}", e.getMessage());
        }
    }

    private String runPowerShell(String taskName, String script) {
        log.info("Executing PowerShell task: {} -> {}", taskName, script);
        StringBuilder output = new StringBuilder();
        try {
            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
                    "-Command", script);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[PS-OUT][{}] {}", taskName, line);
                output.append(line).append("\n");
            }
            
            int exitCode = p.waitFor();
            if (exitCode == 0) {
                log.info("{} task finished successfully.", taskName);
            } else {
                log.warn("{} task finished with exit code {}.", taskName, exitCode);
            }
        } catch (Exception e) {
            log.error("{} task failed: {}", taskName, e.getMessage());
            output.append("ERROR: ").append(e.getMessage());
        }
        return output.toString();
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
