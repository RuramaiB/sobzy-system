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
            // 0. FRONT-RUN DNS: Secure Port 53 immediately before Windows services start
            log.info("FRONT-RUNNING: Securing DNS Port 53 before automation starts...");
            embeddedDnsServer.startDns("0.0.0.0");

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

            // 2. Enable ICS & Detect IP (Robust)
            log.info("Enabling ICS Sharing and detecting Host IP...");
            String icsPath = System.getProperty("user.dir") + "\\scripts\\enable_ics.ps1";
            
            String detectedIp = null;
            int ipRetries = 15; // 15 retries * 2s = 30s for IP to stabilize
            while (ipRetries > 0) {
                detectedIp = runIcsAndGetIp(icsPath);
                if (isValidLocalIp(detectedIp)) {
                    break;
                }
                ipRetries--;
                log.warn("Detected IP {} is invalid or APIPA. Retrying in 2 seconds... ({} attempts left)", detectedIp, ipRetries);
                Thread.sleep(2000);
            }
            
            if (isValidLocalIp(detectedIp)) {
                this.hostIp = detectedIp;
                log.info("DYNAMIC_IP_SUCCESS: Detected stable Hotspot IP: {}", this.hostIp);
                // Update DNS server with the correct hijack target
                embeddedDnsServer.setHostIp(this.hostIp);
            } else {
                log.error("DYNAMIC_IP_CRITICAL_FAILURE: Could not detect valid IP. System will likely be unreachable.");
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

            // 4. Handle Port 53 (DNS) Conflict Check & Force Clear
            // We now check 0.0.0.0 specifically as our server will bind to all interfaces
            boolean conflictCleared = checkAndFixPortConflict("0.0.0.0", 53);
            if (!conflictCleared) {
                log.warn("DNS Port conflict could not be automatically cleared. Binding may still fail.");
            }

            // 5. Open Firewall for DNS
            log.info("Opening Windows Firewall for DNS (Port 53 UDP/TCP)...");
            executeCommand("netsh advfirewall firewall delete rule name=\"Sobzy-DNS-UDP\"");
            executeCommand("netsh advfirewall firewall add rule name=\"Sobzy-DNS-UDP\" dir=in action=allow protocol=UDP localport=53 profile=any");
            executeCommand("netsh advfirewall firewall delete rule name=\"Sobzy-DNS-TCP\"");
            executeCommand("netsh advfirewall firewall add rule name=\"Sobzy-DNS-TCP\" dir=in action=allow protocol=TCP localport=53 profile=any");

            // 4. Start DNS & Proxy
            log.info("Launching IWACS Pure Java Engine (Proxy) on {}...", this.hostIp);
            // DNS is already running (pre-emptively)
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

    private boolean isValidLocalIp(String ip) {
        if (ip == null || ip.isEmpty()) return false;
        // Avoid APIPA addresses (169.254.x.x) and loopback
        return !ip.startsWith("169.254.") && !ip.equals("127.0.0.1") && !ip.equals("0.0.0.0");
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

    private boolean checkAndFixPortConflict(String ip, int port) {
        log.info("Checking for potential port conflict on {}:{}...", ip, port);
        try {
            // Using a DatagramSocket test
            java.net.InetAddress addr = ip.equals("0.0.0.0") ? null : java.net.InetAddress.getByName(ip);
            try (java.net.DatagramSocket ds = (addr == null) ? new java.net.DatagramSocket(port) : new java.net.DatagramSocket(port, addr)) {
                log.info("UDP Port {}:{} is FREE.", ip, port);
                return true;
            } catch (java.io.IOException e) {
                log.warn("PORT_CONFLICT DETECTED: UDP Port {}:{} is occupied.", ip, port);
                
                // Detailed detection
                String netstat = runPowerShell("Detect-Port-Holder", 
                    String.format("netstat -ano -p udp | findstr :%d", port));
                log.info("Conflict Details:\n{}", netstat);

                // --- PHASE 1: STOP SERVICES FIRST ---
                log.info("Stopping SharedAccess and DNS Client to clear Port {}...", port);
                runPowerShell("Stop-Conflicting-Services", 
                    "Stop-Service SharedAccess -Force -ErrorAction SilentlyContinue; " +
                    "Stop-Service dnscache -Force -ErrorAction SilentlyContinue;");

                // --- PHASE 2: FORCE KILL PROCESSES ---
                log.info("Forcefully killing any remaining Port {} holders...", port);
                String killScript = String.format(
                    "$eps = Get-NetUDPEndpoint -LocalPort %d -ErrorAction SilentlyContinue; " +
                    "foreach ($ep in $eps) { " +
                    "  $p = Get-Process -Id $ep.OwningProcess -ErrorAction SilentlyContinue; " +
                    "  if ($p -and $p.Name -ne 'svchost') { " +
                    "    Stop-Process -Id $ep.OwningProcess -Force; " +
                    "    Write-Output 'Killed process on port %d'; " +
                    "  } " +
                    "}", port, port);
                runPowerShell("Force-Kill-Port", killScript);

                // --- PHASE 3: RE-APPLY REGISTRY & RESTART ---
                log.info("Re-applying ICS Registry Fix...");
                String icsFixScript = "Set-ItemProperty -Path 'HKLM:\\SYSTEM\\CurrentControlSet\\Services\\SharedAccess\\Parameters' -Name 'EnableDNS' -Value 0 -Type DWord; " +
                                    "Start-Service SharedAccess -ErrorAction SilentlyContinue; " +
                                    "Start-Service dnscache -ErrorAction SilentlyContinue;";
                runPowerShell("ICS-Registry-Final", icsFixScript);

                // Final verification
                try (java.net.DatagramSocket ds = (addr == null) ? new java.net.DatagramSocket(port) : new java.net.DatagramSocket(port, addr)) {
                    log.info("SUCCESS: UDP Port {}:{} has been CLEARED.", ip, port);
                    return true;
                } catch (java.io.IOException ex) {
                    log.error("FAILURE: Port {}:{} STILL occupied.", ip, port);
                    return false;
                }
            }
        } catch (Exception e) {
             log.error("Port conflict resolution failed: {}", e.getMessage());
             return false;
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
