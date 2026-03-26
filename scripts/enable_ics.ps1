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

# 2. Automatically detect adapters
Write-Log "Detecting network adapters..."

# Source: The one with a valid IPv4 gateway that is NOT a virtual adapter
$SourceAdapter = Get-NetRoute -DestinationPrefix '0.0.0.0/0' | 
                 Get-NetIPInterface -AddressFamily IPv4 | 
                 Get-NetAdapter | 
                 Where-Object { $_.Status -eq 'Up' -and $_.InterfaceDescription -notlike "*Virtual*" -and $_.InterfaceDescription -notlike "*Loopback*" } | 
                 Select-Object -First 1

# Target: The Microsoft Wi-Fi Direct Virtual Adapter used for Hotspot
$TargetAdapter = Get-NetAdapter | 
                 Where-Object { $_.InterfaceDescription -like "*Wi-Fi Direct Virtual Adapter*" -or $_.Name -like "*Local Area Connection* *" } | 
                 Sort-Object Name -Descending | 
                 Select-Object -First 1

if (-not $SourceAdapter) {
    Write-Log "ERROR: Could not detect source adapter with internet access."
    exit 1
}
if (-not $TargetAdapter) {
    Write-Log "ERROR: Could not detect target hotspot adapter (Virtual Adapter)."
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
        Start-Sleep -Seconds 2 # Wait for IP stabilization
        $AssignedIP = (Get-NetIPAddress -InterfaceAlias $TargetAdapterName -AddressFamily IPv4).IPAddress
        if ($AssignedIP) {
            Write-Log "SUCCESS: Assigned IP detected: $AssignedIP"
            Write-Host "[HOST_IP] $AssignedIP"
        } else {
            Write-Log "WARNING: Could not detect IP on $TargetAdapterName. Defaulting to 192.168.137.1"
            Write-Host "[HOST_IP] 192.168.137.1"
        }

    } else {
        Write-Log "ERROR: Failed to map connection handles in NetSharingManager."
        exit 1
    }
} catch {
    Write-Log "CRITICAL ERROR: $($_.Exception.Message)"
    exit 1
}

