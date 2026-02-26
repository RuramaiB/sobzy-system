$connectionProfile = [Windows.Networking.Connectivity.NetworkInformation, Windows.Networking.Connectivity, ContentType=WindowsRuntime]::GetInternetConnectionProfile()
$tetheringManager = [Windows.Networking.NetworkOperators.NetworkOperatorTetheringManager, Windows.Networking.NetworkOperators, ContentType=WindowsRuntime]::CreateFromConnectionProfile($connectionProfile)

if ($tetheringManager) {
    $clients = $tetheringManager.GetTetheringClients()
    if ($clients -and $clients.Count -gt 0) {
        foreach ($client in $clients) {
            Write-Output "MAC:$($client.MacAddress)"
            foreach ($hostname in $client.HostNames) {
                Write-Output "HOSTNAME:$($hostname.CanonicalName)"
            }
        }
    } else {
        Write-Output "No clients connected"
    }
} else {
    Write-Output "Failed to get tethering manager"
}
