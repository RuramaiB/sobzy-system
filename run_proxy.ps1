# PowerShell script to start mitmproxy with the IWACS addon
# This script handles its own elevation if not run as Admin.

param(
    [string]$pythonPath = "python",
    [string]$addonPath = "scripts/mitm_addon.py"
)

# 1. Self-Elevate if needed
$currentPrincipal = New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())
if (-not $currentPrincipal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Host "[!] Restarting with Administrator privileges..." -ForegroundColor Yellow
    $args = "-ExecutionPolicy Bypass -File `"$PSCommandPath`""
    if ($PSBoundParameters.Count -gt 0) {
        foreach ($key in $PSBoundParameters.Keys) {
            $args += " -$key `"$($PSBoundParameters[$key])`""
        }
    }
    Start-Process powershell -ArgumentList $args -Verb RunAs -Wait
    exit $LASTEXITCODE
}

Write-Host "====================================================" -ForegroundColor Cyan
Write-Host "   SOBZY TRAFFIC PROXY & REDIRECTION ENGINE         " -ForegroundColor Cyan -BackgroundColor DarkBlue
Write-Host "====================================================" -ForegroundColor Cyan

# 2. Open Firewall ports
Write-Host "[*] Configuring Windows Firewall..." -ForegroundColor Gray
netsh advfirewall firewall add rule name="Sobzy-Proxy-8080" dir=in action=allow protocol=TCP localport=8080 profile=any

# 3. Identify Host IP for port forwarding
$hostIp = (Get-NetIPAddress | Where-Object { $_.AddressFamily -eq 'IPv4' -and ($_.IPAddress -like '172.24.*' -or $_.IPAddress -like '192.168.137.*') }).IPAddress
if ($hostIp -is [array]) { $hostIp = $hostIp[0] }
if (-not $hostIp) { $hostIp = "192.168.137.1" }

Write-Host "[*] Configuring Port Forwarding (80/443 -> 8080) on $hostIp..." -ForegroundColor Gray
netsh interface portproxy reset # Reset to avoid conflicts
netsh interface portproxy add v4tov4 listenport=80 listenaddress=$hostIp connectport=8080 connectaddress=$hostIp
netsh interface portproxy add v4tov4 listenport=443 listenaddress=$hostIp connectport=8080 connectaddress=$hostIp

# 4. Start mitmproxy
Write-Host "[*] Starting mitmdump with addon: $addonPath" -ForegroundColor Yellow
Write-Host "    Press Ctrl+C to stop."
Write-Host "----------------------------------------------------"

& $pythonPath -m mitmproxy.tools.dump -s $addonPath --mode regular --listen-port 8080 --listen-host 0.0.0.0
