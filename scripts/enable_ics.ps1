# Enable-ICS.ps1
# Improved script for robust ICS enabling on Windows 10/11
# Requires Administrator privileges.

$LogFile = "$env:TEMP\sobzy_ics_setup.log"
function Write-Log($msg) {
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    "$timestamp - $msg" | Out-File -FilePath $LogFile -Append
    Write-Host "[*] $msg"
}

Write-Log "Starting ICS Configuration script..."

# 1. Admin Check (No self-elevation here to avoid silent failures in Java)
if (-not ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Log "ERROR: Script is not running as Administrator. ICS cannot be enabled."
    exit 1
}

# 2. Disable built-in ICS DNS to avoid Port 53 conflict
Write-Log "Ensuring ICS DNS is disabled in Registry..."
$regPath = "HKLM:\SYSTEM\CurrentControlSet\Services\SharedAccess\Parameters"
$regValue = "EnableDNS"

try {
    # 2.1 Force stop the service first (v5: with stuck service detection)
    Write-Log "Stopping SharedAccess (ICS) service..."
    $service = Get-Service "SharedAccess" -ErrorAction SilentlyContinue
    if ($service) {
        Stop-Service SharedAccess -Force -ErrorAction SilentlyContinue
        $timeout = 10
        while ($service.Status -ne 'Stopped' -and $timeout -gt 0) {
            Write-Log "Waiting for SharedAccess to stop ($timeout s)..."
            Start-Sleep -Seconds 1
            $service = Get-Service "SharedAccess"
            $timeout--
        }
        if ($service.Status -ne 'Stopped') {
            Write-Log "WARNING: SharedAccess STUCK. Force-killing service process..."
            taskkill /F /FI "SERVICES eq SharedAccess" /T
            Start-Sleep -Seconds 2
        }
    }
    
    # 2.2 Identify and KILL any process holding Port 53 (v5: aggressive but safe)
    Write-Log "Checking for other processes on UDP Port 53..."
    $myPid = $pid
    $parentPid = (Get-CimInstance Win32_Process -Filter "ProcessId = $myPid").ParentProcessId
    
    $dnsEndpoints = Get-NetUDPEndpoint -LocalPort 53 -ErrorAction SilentlyContinue
    foreach ($endpoint in $dnsEndpoints) {
        $ownerPid = $endpoint.OwningProcess
        if ($ownerPid -eq $myPid -or $ownerPid -eq $parentPid) {
            Write-Log "Ignoring Port 53 hold by self or parent (PID: $ownerPid)."
            continue
        }
        Write-Log "PORT_CONFLICT: Terminating PID: $ownerPid holding Port 53..."
        taskkill /F /PID $ownerPid /T
    }
    Start-Sleep -Seconds 2 # Settling wait for kernel

    if (-not (Test-Path $regPath)) {
        New-Item -Path $regPath -Force | Out-Null
    }
    Set-ItemProperty -Path $regPath -Name $regValue -Value 0 -Type DWord
    Write-Log "Registry: SharedAccess\EnableDNS set to 0."
} catch {
    Write-Log "WARNING: Robust DNS clearing failed. $($_.Exception.Message)"
}

# 3. Automatically detect adapters
Write-Log "Detecting network adapters..."

# Source: The one with a valid IPv4 gateway that is NOT a virtual adapter
$SourceAdapter = Get-NetRoute -DestinationPrefix '0.0.0.0/0' | 
                 Get-NetIPInterface -AddressFamily IPv4 | 
                 Get-NetAdapter | 
                 Where-Object { $_.Status -eq 'Up' -and $_.InterfaceDescription -notlike "*Virtual*" -and $_.InterfaceDescription -notlike "*Loopback*" } | 
                 Select-Object -First 1

# Target: The Microsoft Wi-Fi Direct Virtual Adapter used for Hotspot
$TargetAdapter = $null
$MaxAdapterWait = 10
for ($i = 0; $i -lt $MaxAdapterWait; $i++) {
    $TargetAdapter = Get-NetAdapter | 
                     Where-Object { ($_.InterfaceDescription -like "*Wi-Fi Direct Virtual Adapter*" -or 
                                    $_.Name -like "*Local Area Connection* *" -or
                                    $_.InterfaceDescription -like "*Microsoft Wi-Fi Direct*") } | 
                     Sort-Object Status, Name -Descending | 
                     Select-Object -First 1
    if ($TargetAdapter) { break }
    Write-Log "Waiting for Virtual Adapter to appear ($($i+1)/$MaxAdapterWait)..."
    Start-Sleep -Seconds 1
}

if (-not $SourceAdapter) {
    Write-Log "ERROR: Could not detect source adapter with internet access."
    exit 1
}
if (-not $TargetAdapter) {
    Write-Log "ERROR: Could not detect target hotspot adapter (Virtual Adapter) after ${MaxAdapterWait}s."
    exit 1
}

$SourceAdapterName = $SourceAdapter.Name
$TargetAdapterName = $TargetAdapter.Name

Write-Log "Source Adapter detected: $SourceAdapterName ($($SourceAdapter.InterfaceDescription))"
Write-Log "Target Adapter detected: $TargetAdapterName ($($TargetAdapter.InterfaceDescription))"

# 3. Enable ICS using COM objects
try {
    $NetSharingManager = New-Object -ComObject HNetCfg.HNetShare
    
    $SourceConn = $null
    $TargetConn = $null

    foreach ($conn in $NetSharingManager.EnumEveryConnection) {
        $props = $NetSharingManager.NetConnectionProps($conn)
        if ($props.Name -eq $SourceAdapterName) {
            $SourceConn = $NetSharingManager.INetSharingConfigurationForINetConnection($conn)
        }
        if ($props.Name -eq $TargetAdapterName) {
            $TargetConn = $NetSharingManager.INetSharingConfigurationForINetConnection($conn)
        }
    }

    if ($SourceConn -and $TargetConn) {
        Write-Log "Found connection handles. Enabling sharing..."
        
        $MaxRetries = 3
        $RetryCount = 0
        $Success = $false
        
        while (-not $Success -and $RetryCount -lt $MaxRetries) {
            $RetryCount++
            try {
                # Disable current sharing first to reset state
                $SourceConn.DisableSharing()
                $TargetConn.DisableSharing()
                Start-Sleep -Seconds 1
                
                $SourceConn.EnableSharing(0) # 0 = Public (Internet)
                $TargetConn.EnableSharing(1) # 1 = Private (Local)
                
                $Success = $true
                Write-Log "SUCCESS: ICS enabled successfully between $SourceAdapterName and $TargetAdapterName (Attempt $RetryCount)"
            } catch {
                Write-Log "Attempt $RetryCount failed: $($_.Exception.Message)"
                if ($_.Exception.Message -like "*0x80040201*" -or $_.Exception.Message -like "*invoked*") {
                    Write-Log "SharedAccess service issue detected. Restarting service..."
                    Restart-Service SharedAccess -Force
                    Start-Sleep -Seconds 5
                }
                Start-Sleep -Seconds 2
            }
        }
        
        if (-not $Success) {
            Write-Log "ERROR: Failed to enable ICS after $MaxRetries attempts."
            exit 1
        }

        # 4. Detect the assigned IP
        Write-Log "Detecting assigned IP on $TargetAdapterName..."
        Start-Sleep -Seconds 3 # Wait for IP stabilization
        # Filter out APIPA addresses (169.254.*) to ensure we get the real ICS IP
        $AssignedIP = (Get-NetIPAddress -InterfaceAlias $TargetAdapterName -AddressFamily IPv4 | Where-Object { $_.IPAddress -notlike "169.254.*" }).IPAddress
        
        if ($AssignedIP) {
            Write-Log "SUCCESS: Assigned IP detected: $AssignedIP"
            Write-Host "[HOST_IP] $AssignedIP"
        } else {
            Write-Log "WARNING: Could not detect valid IP on $TargetAdapterName. (Is the adapter still initializing?)"
            # No [HOST_IP] output here so Java can retry correctly
        }

        # 5. Open Firewall for DNS
        Write-Log "Configuring Windows Firewall for DNS (Port 53 UDP/TCP)..."
        netsh advfirewall firewall delete rule name="Sobzy-DNS-UDP" | Out-Null
        netsh advfirewall firewall add rule name="Sobzy-DNS-UDP" dir=in action=allow protocol=UDP localport=53 profile=any | Out-Null
        netsh advfirewall firewall delete rule name="Sobzy-DNS-TCP" | Out-Null
        netsh advfirewall firewall add rule name="Sobzy-DNS-TCP" dir=in action=allow protocol=TCP localport=53 profile=any | Out-Null

    } else {
        Write-Log "ERROR: Failed to map connection handles in NetSharingManager."
        exit 1
    }
} catch {
    Write-Log "CRITICAL ERROR: $($_.Exception.Message)"
    exit 1
}

