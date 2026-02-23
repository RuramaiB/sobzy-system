# PowerShell script to enable Internet Connection Sharing (ICS) on Windows
# Author: IWACS System Integration

$m = New-Object -ComObject HNetCfg.HNetShare

# 1. Identify the Private Connection (Hotspot)
# Usually has the specific Hotspot IP prefix or 'Hotspot' in the name
$privateConn = $m.EnumEveryConnection | Where-Object { 
    $props = $m.NetConnectionProps($_)
    $props.Name -like "*Local Area Connection*" -or $props.Name -like "*Hotspot*"
} | Select-Object -First 1

# 2. Identify the Public Connection (Internet)
# Usually Wi-Fi or Ethernet that is connected
$publicConn = $m.EnumEveryConnection | Where-Object { 
    $props = $m.NetConnectionProps($_)
    ($props.Name -eq "Wi-Fi" -or $props.Name -eq "Ethernet") -and $_ -ne $privateConn
} | Select-Object -First 1

if (-not $publicConn -or -not $privateConn) {
    Write-Error "Could not identify adapters for ICS. Public: $($m.NetConnectionProps($publicConn).Name), Private: $($m.NetConnectionProps($privateConn).Name)"
    exit 1
}

Write-Host "Enabling ICS: $($m.NetConnectionProps($publicConn).Name) -> $($m.NetConnectionProps($privateConn).Name)"

$publicConfig = $m.INetSharingConfigurationForINetConnection($publicConn)
$privateConfig = $m.INetSharingConfigurationForINetConnection($privateConn)

# Enable sharing
$publicConfig.EnableSharing(0) # 0 = Public
$privateConfig.EnableSharing(1) # 1 = Private

Write-Host "ICS Enabled Successfully."
