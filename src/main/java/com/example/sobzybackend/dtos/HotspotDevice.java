package com.example.sobzybackend.dtos;


public class HotspotDevice {
    private String mac;
    private String ip;
    private String hostname;
    private String deviceType;

    public HotspotDevice() {}

    public HotspotDevice(String mac, String ip, String hostname, String deviceType) {
        this.mac = mac;
        this.ip = ip;
        this.hostname = hostname;
        this.deviceType = deviceType;
    }

    public static HotspotDeviceBuilder builder() {
        return new HotspotDeviceBuilder();
    }

    public String getMac() { return mac; }
    public void setMac(String mac) { this.mac = mac; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }

    public static class HotspotDeviceBuilder {
        private String mac, ip, hostname, deviceType;
        public HotspotDeviceBuilder mac(String mac) { this.mac = mac; return this; }
        public HotspotDeviceBuilder ip(String ip) { this.ip = ip; return this; }
        public HotspotDeviceBuilder hostname(String hostname) { this.hostname = hostname; return this; }
        public HotspotDeviceBuilder deviceType(String deviceType) { this.deviceType = deviceType; return this; }
        public HotspotDevice build() {
            return new HotspotDevice(mac, ip, hostname, deviceType);
        }
    }
}
