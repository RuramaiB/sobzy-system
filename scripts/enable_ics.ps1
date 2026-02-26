# Enable-ICS.ps1
# This script enables Internet Connection Sharing (ICS) between a source adapter and a target adapter.
# It requires Administrator privileges.

param (
    [string]$SourceAdapterName, # The adapter with internet access (e.g., "Wi-Fi" or "Ethernet")
    [string]$TargetAdapterName  # The hotspot adapter (e.g., "Local Area Connection* 1")
)

# 1. Self-elevation to Administrator if not already running as admin
if (-not ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Host "[!] This script must be run as Administrator. Attempting to elevate..." -ForegroundColor Red
    Start-Process powershell.exe -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`"" -Verb RunAs
    exit
}

# 2. Automatically detect adapters if not provided
if (-not $SourceAdapterName -or -not $TargetAdapterName) {
    Write-Host "[*] Detecting network adapters..." -ForegroundColor Cyan
    
    # Source is usually the one with a gateway
    $SourceAdapter = Get-NetRoute | Where-Object { $_.DestinationPrefix -eq '0.0.0.0/0' } | Get-NetAdapter | Select-Object -First 1
    
    # Target is usually the Microsoft Wi-Fi Direct Virtual Adapter
    $TargetAdapter = Get-NetAdapter | Where-Object { $_.InterfaceDescription -like "*Wi-Fi Direct Virtual Adapter*" } | Select-Object -First 1

    if (-not $SourceAdapter) {
        Write-Host "[!] Could not detect source adapter with internet access." -ForegroundColor Red
        return
    }
    if (-not $TargetAdapter) {
        Write-Host "[!] Could not detect target hotspot adapter." -ForegroundColor Red
        return
    }

    $SourceAdapterName = $SourceAdapter.Name
    $TargetAdapterName = $TargetAdapter.Name
}

Write-Host "[*] Source Adapter: $SourceAdapterName" -ForegroundColor Green
Write-Host "[*] Target Adapter: $TargetAdapterName" -ForegroundColor Green

# 3. Enable ICS using COM objects (The standard Windows way)
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
        Write-Host "[*] Enabling ICS..." -ForegroundColor Yellow
        $SourceConn.EnableSharing(0) # 0 = Public (Internet)
        $TargetConn.EnableSharing(1) # 1 = Private (Local)
        Write-Host "[+] ICS enabled successfully!" -ForegroundColor Green
    } else {
        Write-Host "[!] Failed to find one or both connections in NetSharingManager." -ForegroundColor Red
    }
} catch {
    Write-Host "[!] Error enabling ICS: $($_.Exception.Message)" -ForegroundColor Red
}

# 4. Ensure the hotspot service is actually started (if needed)
# netsh wlan start hostednetwork # (Old way)
# Powershell can also use: Start-Process powershell -ArgumentList "Start-Service WlanSvc"
