$connectionProfile = [Windows.Networking.Connectivity.NetworkInformation, Windows.Networking.Connectivity, ContentType=WindowsRuntime]::GetInternetConnectionProfile()
$tetheringManager = [Windows.Networking.NetworkOperators.NetworkOperatorTetheringManager, Windows.Networking.NetworkOperators, ContentType=WindowsRuntime]::CreateFromConnectionProfile($connectionProfile)

if ($tetheringManager) {
    # Populating ARP cache
    arp -a | Out-Null
    $clients = $tetheringManager.GetTetheringClients()
    if ($clients -and $clients.Count -gt 0) {
        $arpTable = arp -a
        foreach ($client in $clients) {
            $mac = $client.MacAddress
            $hostname = "Unknown"
            if ($client.HostNames -and $client.HostNames.Count -gt 0) {
                $hostname = $client.HostNames[0].CanonicalName
            }
            
            # Try to find IP in ARP table
            # format:  192.168.137.123      00-11-22-33-44-55     dynamic
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
            
            Write-Output "DEVICE|MAC:$mac|IP:$ip|HOSTNAME:$hostname"
        }
    } else {
        Write-Output "INFO:No clients connected"
    }
} else {
    Write-Output "ERROR:Failed to get tethering manager"
}
