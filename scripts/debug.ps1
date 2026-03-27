Get-Service icssvc, WlanSvc, SharedAccess
Get-NetAdapter | Select Name, Status, InterfaceDescription

[void][Windows.Networking.Connectivity.NetworkInformation, Windows.Networking.Connectivity, ContentType=WindowsRuntime]
[void][Windows.Networking.NetworkOperators.NetworkOperatorTetheringManager, Windows.Networking.NetworkOperators, ContentType=WindowsRuntime]

$profile = [Windows.Networking.Connectivity.NetworkInformation]::GetInternetConnectionProfile()
if ($profile) {
    Write-Host "Internet Profile: " $profile.ProfileName
    $mgr = [Windows.Networking.NetworkOperators.NetworkOperatorTetheringManager]::CreateFromConnectionProfile($profile)
    Write-Host "Capabilities: " $mgr.GetTetheringCapability($profile)
    Write-Host "State: " $mgr.TetheringOperationalState
} else {
    Write-Host "No Internet Profile!"
}
