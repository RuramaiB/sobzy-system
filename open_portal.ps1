# PowerShell script to manually open the captive portal for testing
# This script identifies your hotspot IP and gives you the exact link for your phone

$hostIp = (Get-NetIPAddress | Where-Object {$_.AddressFamily -eq 'IPv4' -and $_.InterfaceAlias -notlike 'Loopback*'}).IPAddress[0]

if (-not $hostIp) {
    $hostIp = "192.168.137.1"
}

$portalUrl = "http://$($hostIp):3000/login"

Clear-Host
Write-Host "====================================================" -ForegroundColor Cyan
Write-Host "   IWACS CAPTIVE PORTAL - CONNECTION ASSIGNMENT     " -ForegroundColor Cyan -BackgroundColor DarkBlue
Write-Host "====================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "2. ON YOUR PHONE / TABLET:" -ForegroundColor Yellow
Write-Host "   Connect to the Wi-Fi. The portal should POP UP AUTOMATICALLY."
Write-Host "   (Zero-Config mode enabled via DNS Hijacking)"
Write-Host ""
Write-Host "   Backup Link / QR Code:" -ForegroundColor Gray
python -c "import qrcode; qr = qrcode.QRCode(); qr.add_data('$portalUrl'); qr.make(); qr.print_ascii(invert=True)"
Write-Host ""
Write-Host "   Link: $portalUrl" -ForegroundColor Green -BackgroundColor Black
Write-Host ""
Write-Host "3. NO INTERNET? (CRITICAL):" -ForegroundColor Red
Write-Host "   Ensure 'Internet Connection Sharing' is enabled in Windows."
Write-Host "   I have attempted to automate this, but you can check in 'ncpa.cpl'."
Write-Host "====================================================" -ForegroundColor Cyan

Start-Process $portalUrl
