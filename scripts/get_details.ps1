$ErrorActionPreference = 'Stop'
$result = @{
    SSID = $null
    PASS = $null
    STATUS = $null
    HOSTIP = $null
    UPSTREAM = $null
    DEVICES = @()
}

try {
    # 1. Get Tethering Config and Status
    try {
        $cp = [Windows.Networking.Connectivity.NetworkInformation, Windows.Networking.Connectivity, ContentType=WindowsRuntime]::GetInternetConnectionProfile()
        if (-not $cp) {
            $cp = [Windows.Networking.Connectivity.NetworkInformation, Windows.Networking.Connectivity, ContentType=WindowsRuntime]::GetConnectionProfiles() | Select-Object -First 1
        }
        
        if ($cp) {
            $tm = [Windows.Networking.NetworkOperators.NetworkOperatorTetheringManager, Windows.Networking.NetworkOperators, ContentType=WindowsRuntime]::CreateFromConnectionProfile($cp)
            if ($tm) {
                $status = $tm.TetheringOperationalState
                $result.STATUS = $status.ToString()
                
                $cfg = $tm.GetCurrentAccessPointConfiguration()
                $result.SSID = $cfg.Ssid
                $result.PASS = $cfg.Passphrase
                
                # Get Clients
                arp -a | Out-Null # Populating ARP cache
                $clients = $tm.GetTetheringClients()
                if ($clients -and $clients.Count -gt 0) {
                    $arpTable = arp -a
                    foreach ($client in $clients) {
                        $mac = $client.MacAddress
                        $hostname = "Unknown"
                        if ($client.HostNames -and $client.HostNames.Count -gt 0) {
                            $hostname = $client.HostNames[0].CanonicalName
                        }
                        
                        $macHyphen = $mac.Replace(":", "-")
                        $ip = "Unknown"
                        foreach ($line in $arpTable) {
                            if ($line -like "*$macHyphen*") {
                                $parts = $line.Trim() -split "\s+"
                                if ($parts.Count -ge 1) {
                                    $ip = $parts[0]
                                    break
                                }
                            }
                        }
                        $result.DEVICES += "DEVICE|MAC:$mac|IP:$ip|HOSTNAME:$hostname"
                    }
                }
            }
        }
    } catch {
        Write-Output "ERROR:Tethering Manager failure: $($_.Exception.Message)"
    }

    # 2. Get IP Info
    $ips = Get-NetIPAddress -AddressFamily IPv4
    $hip = ($ips | Where-Object { $_.InterfaceAlias -like '*Hotspot*' -or $_.InterfaceAlias -like '*Local Area Connection*' }).IPAddress[0]
    if (-not $hip) {
        $hip = ($ips | Where-Object { $_.InterfaceAlias -notlike 'Loopback*' }).IPAddress[0]
    }
    $result.HOSTIP = $hip

    $up = ($ips | Where-Object { 
        $_.InterfaceAlias -notlike 'Loopback*' -and 
        $_.InterfaceAlias -notlike '*Hotspot*' -and 
        $_.InterfaceAlias -notlike '*Virtual*' -and 
        $_.InterfaceAlias -notlike '*Local Area*' -and 
        $_.InterfaceAlias -notlike '*Pseudo*' 
    }).InterfaceAlias[0]
    $result.UPSTREAM = $up

    # Output results in a format the Java code can parse
    if ($result.SSID) { Write-Output "SSID:$($result.SSID)" }
    if ($result.PASS) { Write-Output "PASS:$($result.PASS)" }
    if ($result.STATUS) { Write-Output "STATUS:$($result.STATUS)" }
    if ($result.HOSTIP) { Write-Output "HOSTIP:$($result.HOSTIP)" }
    if ($result.UPSTREAM) { Write-Output "UPSTREAM:$($result.UPSTREAM)" }
    foreach ($dev in $result.DEVICES) { Write-Output $dev }

} catch {
    Write-Output "ERROR:Global script failure: $($_.Exception.Message)"
}
