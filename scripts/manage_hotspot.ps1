# manage_hotspot.ps1
# World-Class Mobile Hotspot Orchestrator
$SSID = $args[0]
if (-not $SSID) { $SSID = "Sobzy_Safe_Hotspot" }
$Password = $args[1]
if (-not $Password) { $Password = "Sobzy12345" }

$LogFile = "$env:TEMP\sobzy_hotspot_manager.log"
function Write-Log($msg) {
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    "$timestamp - $msg" | Out-File -FilePath $LogFile -Append
    Write-Host "[*] $msg"
}

Write-Log "--- Starting Managed Hotspot Lifecycle ($SSID) ---"

# 1. Ensure Privileges
if (-not ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Log "ERROR: Administrator privileges required."
    exit 1
}

# 2. Restart icssvc service to clear any bad state instead of stopping SharedAccess
Write-Log "Restarting icssvc to clear state..."
Restart-Service icssvc -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2

# 3. Load WinRT Types
[void][Windows.Networking.Connectivity.NetworkInformation, Windows.Networking.Connectivity, ContentType=WindowsRuntime]
[void][Windows.Networking.NetworkOperators.NetworkOperatorTetheringManager, Windows.Networking.NetworkOperators, ContentType=WindowsRuntime]

# 4. Find Internet Profile
$Profile = [Windows.Networking.Connectivity.NetworkInformation]::GetInternetConnectionProfile()
if (-not $Profile) {
    Write-Log "ERROR: No active internet connection found to share."
    exit 1
}

$Manager = [Windows.Networking.NetworkOperators.NetworkOperatorTetheringManager]::CreateFromConnectionProfile($Profile)

# 5. Stop existing if ON
if ($Manager.TetheringOperationalState -eq 'On') {
    Write-Log "Stopping current hotspot..."
    $Async = $Manager.StopTetheringAsync()
    while ($Async.Status -eq 'Started') { Start-Sleep -Milliseconds 100 }
    Start-Sleep -Seconds 2
}

# 6. Configure
Write-Log "Configuring Access Point..."
$Config = $Manager.GetCurrentAccessPointConfiguration()
$Config.Ssid = $SSID
$Config.Passphrase = $Password
$ConfigAsync = $Manager.ConfigureAccessPointAsync($Config)
while ($ConfigAsync.Status -eq 'Started') { Start-Sleep -Milliseconds 100 }
Start-Sleep -Seconds 1

# 7. Start
Write-Log "Starting Hotspot..."
$StartAsync = $Manager.StartTetheringAsync()
while ($StartAsync.Status -eq 'Started') { Start-Sleep -Milliseconds 100 }
Start-Sleep -Seconds 2

if ($Manager.TetheringOperationalState -ne 'On') {
    $err = $StartAsync.ErrorCode
    Write-Log "ERROR: Hotspot failed to start. State: $($Manager.TetheringOperationalState), ErrorCode: $err"
    # Even if it says Off, Windows sometimes takes a few seconds. We'll wait and recheck.
    Start-Sleep -Seconds 3
    if ($Manager.TetheringOperationalState -ne 'On') {
        exit 1
    }
}

# 8. Wait for Virtual Adapter
Write-Log "Waiting for Virtual Adapter to initialize..."
$TargetAdapter = $null
for ($i=0; $i -lt 15; $i++) {
    $TargetAdapter = Get-NetAdapter | Where-Object { 
        $_.InterfaceDescription -like "*Wi-Fi Direct Virtual Adapter*" -or 
        $_.Name -like "*Local Area Connection* *" -or
        $_.InterfaceDescription -like "*Microsoft Wi-Fi Direct*"
    } | Sort-Object Status, Name -Descending | Select-Object -First 1
    if ($TargetAdapter) { break }
    Start-Sleep -Seconds 1
}

if (-not $TargetAdapter) {
    Write-Log "ERROR: Virtual Adapter never appeared."
    exit 1
}

$TargetName = $TargetAdapter.Name
Write-Log "Found Target Adapter: $TargetName"

# 9. Get Source Adapter
$SourceAdapter = Get-NetRoute -DestinationPrefix '0.0.0.0/0' | Get-NetIPInterface -AddressFamily IPv4 | Get-NetAdapter | Where-Object { $_.InterfaceDescription -notlike "*Virtual*" } | Select-Object -First 1

# 10. DNS Hardening
Write-Log "Finalizing DNS Hardening..."
$regPath = "HKLM:\SYSTEM\CurrentControlSet\Services\SharedAccess\Parameters"
Set-ItemProperty -Path $regPath -Name "EnableDNS" -Value 0 -Type DWord -ErrorAction SilentlyContinue
Start-Service SharedAccess -ErrorAction SilentlyContinue

# 11. IP Detection & Output
Start-Sleep -Seconds 3
$HostIP = (Get-NetIPAddress -InterfaceAlias $TargetName -AddressFamily IPv4 | Where-Object { $_.IPAddress -notlike "169.254.*" }).IPAddress
$NetworkIP = (Get-NetIPAddress -InterfaceAlias $SourceAdapter.Name -AddressFamily IPv4).IPAddress

Write-Host "[HOST_IP] $HostIP"
Write-Host "[NETWORK_IP] $NetworkIP"
Write-Host "[ADAPTER_NAME] $TargetName"

Write-Log "--- Hotspot Lifecycle Complete ---"
