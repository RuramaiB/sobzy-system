# PowerShell script to start mitmproxy with the IWACS addon
# This script ensures firewall rules are set and starts the proxy correctly

$pythonPath = "C:\Python314\python.exe"
$addonPath = "src/main/python/mitm_addon.py"

Write-Host "====================================================" -ForegroundColor Cyan
Write-Host "   IWACS TRAFFIC PROXY & REDIRECTION ENGINE         " -ForegroundColor Cyan -BackgroundColor DarkBlue
Write-Host "====================================================" -ForegroundColor Cyan

# 1. Open Firewall ports (Requires Admin usually)
Write-Host "[*] Configuring Windows Firewall..." -ForegroundColor Gray
netsh advfirewall firewall add rule name="IWACS-DNS-Hijacker" dir=in action=allow protocol=UDP localport=53
netsh advfirewall firewall add rule name="IWACS-HTTP-Redirection" dir=in action=allow protocol=TCP localport=80

# 2. Identify Host IP for logging
$hostIp = (Get-NetIPAddress | Where-Object { $_.AddressFamily -eq 'IPv4' -and ($_.IPAddress -like '172.24.*' -or $_.IPAddress -like '192.168.137.*') }).IPAddress
if ($hostIp -is [array]) { $hostIp = $hostIp[0] }
if (-not $hostIp) { $hostIp = "172.24.64.1" }

Write-Host "[*] Hotspot IP identified: $hostIp" -ForegroundColor Green

# 3. Port Forwarding for Zero-Config (WPAD & Transparent Interception)
Write-Host "[*] Setting up Port Forwarding (80/443 -> 8080)..." -ForegroundColor Gray
netsh interface portproxy add v4tov4 listenport=80 listenaddress=$hostIp connectport=8080 connectaddress=$hostIp
netsh interface portproxy add v4tov4 listenport=443 listenaddress=$hostIp connectport=8080 connectaddress=$hostIp

# 3. Start mitmproxy (using python module mode)
Write-Host "[*] Starting mitmdump with addon: $addonPath" -ForegroundColor Yellow
Write-Host "    Press Ctrl+C to stop."
Write-Host "----------------------------------------------------"

# We run as a separate process to avoid blocking this shell
& $pythonPath -u -m mitmproxy.tools.dump -s $addonPath --mode regular
