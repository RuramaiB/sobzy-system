package com.example.sobzybackend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HotspotInfoResponse {
    private String ssid;
    private String password;
    private String status;
    private String hostIp;
    private String gatewayIp;
    private String upstreamInterface;
    private List<HotspotDevice> connectedDevices;
}
