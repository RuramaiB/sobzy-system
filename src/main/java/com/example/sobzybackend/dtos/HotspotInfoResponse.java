package com.example.sobzybackend.dtos;

import java.util.List;

public class HotspotInfoResponse {
    private String ssid;
    private String password;
    private String status;
    private String hostIp;
    private String gatewayIp;
    private String upstreamInterface;
    private List<HotspotDevice> connectedDevices;

    public HotspotInfoResponse() {}

    public HotspotInfoResponse(String ssid, String password, String status, String hostIp, String gatewayIp, String upstreamInterface, List<HotspotDevice> connectedDevices) {
        this.ssid = ssid;
        this.password = password;
        this.status = status;
        this.hostIp = hostIp;
        this.gatewayIp = gatewayIp;
        this.upstreamInterface = upstreamInterface;
        this.connectedDevices = connectedDevices;
    }

    public static HotspotInfoResponseBuilder builder() {
        return new HotspotInfoResponseBuilder();
    }

    public String getSsid() { return ssid; }
    public void setSsid(String ssid) { this.ssid = ssid; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getHostIp() { return hostIp; }
    public void setHostIp(String hostIp) { this.hostIp = hostIp; }
    public List<HotspotDevice> getConnectedDevices() { return connectedDevices; }
    public void setConnectedDevices(List<HotspotDevice> connectedDevices) { this.connectedDevices = connectedDevices; }

    public static class HotspotInfoResponseBuilder {
        private String ssid, password, status, hostIp, gatewayIp, upstreamInterface;
        private List<HotspotDevice> connectedDevices;

        public HotspotInfoResponseBuilder ssid(String ssid) { this.ssid = ssid; return this; }
        public HotspotInfoResponseBuilder password(String password) { this.password = password; return this; }
        public HotspotInfoResponseBuilder status(String status) { this.status = status; return this; }
        public HotspotInfoResponseBuilder hostIp(String hostIp) { this.hostIp = hostIp; return this; }
        public HotspotInfoResponseBuilder connectedDevices(List<HotspotDevice> devices) { this.connectedDevices = devices; return this; }
        public HotspotInfoResponse build() {
            return new HotspotInfoResponse(ssid, password, status, hostIp, gatewayIp, upstreamInterface, connectedDevices);
        }
    }
}
