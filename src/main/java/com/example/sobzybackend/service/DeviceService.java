package com.example.sobzybackend.service;

import com.example.sobzybackend.dtos.DeviceRegistrationRequest;
import com.example.sobzybackend.dtos.DeviceResponse;
import com.example.sobzybackend.enums.DeviceStatus;
import com.example.sobzybackend.exceptions.ResourceNotFoundException;
import com.example.sobzybackend.models.Device;
import com.example.sobzybackend.repository.DeviceRepository;
import com.example.sobzybackend.repository.UserRepository;
import com.example.sobzybackend.users.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;

    @Transactional
    public DeviceResponse registerDevice(DeviceRegistrationRequest request) {
        log.info("Registering device with MAC: {}", request.getMacAddress());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));

        // Check if device already exists
        Device device = deviceRepository.findByMacAddress(request.getMacAddress())
                .orElseGet(() -> {
                    Device newDevice = Device.builder()
                            .user(user)
                            .macAddress(request.getMacAddress())
                            .ipAddress(request.getIpAddress())
                            .deviceName(request.getDeviceName())
                            .deviceType(request.getDeviceType())
                            .osInfo(request.getOsInfo())
                            .browserInfo(request.getBrowserInfo())
                            .status(DeviceStatus.ACTIVE)
                            .build();
                    return deviceRepository.save(newDevice);
                });

        // Update if exists
        if (device.getId() != null) {
            device.setIpAddress(request.getIpAddress());
            device.setLastSeen(LocalDateTime.now());
            device = deviceRepository.save(device);
        }

        log.info("Device registered: {}", device.getId());
        return convertToResponse(device);
    }

    @Transactional(readOnly = true)
    public DeviceResponse getDeviceById(Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device", "id", id));
        return convertToResponse(device);
    }

    @Transactional(readOnly = true)
    public DeviceResponse getDeviceByMacAddress(String macAddress) {
        Device device = deviceRepository.findByMacAddress(macAddress)
                .orElseThrow(() -> new ResourceNotFoundException("Device", "macAddress", macAddress));
        return convertToResponse(device);
    }

    @Transactional(readOnly = true)
    public List<DeviceResponse> getDevicesByUserId(Long userId) {
        return deviceRepository.findByUserId(userId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DeviceResponse> getActiveDevices() {
        return deviceRepository.findByStatus(DeviceStatus.ACTIVE).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void blockDevice(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device", "id", deviceId));
        device.setStatus(DeviceStatus.BLOCKED);
        deviceRepository.save(device);
        log.info("Device blocked: {}", deviceId);
    }

    @Transactional
    public void unblockDevice(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device", "id", deviceId));
        device.setStatus(DeviceStatus.ACTIVE);
        deviceRepository.save(device);
        log.info("Device unblocked: {}", deviceId);
    }

    @Transactional
    public void deleteDevice(Long deviceId) {
        if (!deviceRepository.existsById(deviceId)) {
            throw new ResourceNotFoundException("Device", "id", deviceId);
        }
        deviceRepository.deleteById(deviceId);
        log.info("Device deleted: {}", deviceId);
    }

    @Transactional
    public void markAllDevicesInactive() {
        log.info("Marking all devices as INACTIVE (Syncing with Hotspot state)");
        List<Device> activeDevices = deviceRepository.findByStatus(DeviceStatus.ACTIVE);
        for (Device device : activeDevices) {
            device.setStatus(DeviceStatus.INACTIVE);
        }
        deviceRepository.saveAll(activeDevices);
    }

    @Transactional
    public void updateDeviceStatus(String macAddress, com.example.sobzybackend.enums.DeviceStatus status) {
        deviceRepository.findByMacAddress(macAddress).ifPresent(device -> {
            log.info("Updating device {} status to {}", macAddress, status);
            device.setStatus(status);
            device.setLastSeen(LocalDateTime.now());
            deviceRepository.save(device);
        });
    }

    @Transactional(readOnly = true)
    public List<DeviceResponse> scanNetwork() {
        log.info("Starting actual network scan via hotspot clients (non-persistent)...");

        String scriptPath = "scripts/get_clients.ps1";
        String output = runPowerShell("-File " + scriptPath);

        if (output.startsWith("ERROR:")) {
            log.error("Failed to fetch hotspot clients: {}", output);
            return List.of();
        }

        if (output.contains("INFO:No clients connected")) {
            log.info("No clients currently connected to hotspot.");
            return List.of();
        }

        List<DeviceResponse> discoveredDevices = new java.util.ArrayList<>();
        String[] lines = output.split("\\r?\\n");

        for (String line : lines) {
            if (line.startsWith("DEVICE|")) {
                try {
                    String mac = null;
                    String ip = null;
                    String hostname = "Unknown Device";

                    String[] parts = line.split("\\|");
                    for (String part : parts) {
                        if (part.startsWith("MAC:"))
                            mac = part.substring(4).trim();
                        else if (part.startsWith("IP:"))
                            ip = part.substring(3).trim();
                        else if (part.startsWith("HOSTNAME:"))
                            hostname = part.substring(9).trim();
                    }

                    if (mac != null) {
                        DeviceResponse response = DeviceResponse.builder()
                                .macAddress(mac)
                                .ipAddress(ip)
                                .deviceName(hostname)
                                .deviceType(hostname.toLowerCase().contains("android") ||
                                        hostname.toLowerCase().contains("iphone") ? "Mobile" : "Desktop")
                                .status("ACTIVE")
                                .lastSeen(LocalDateTime.now())
                                .build();
                        discoveredDevices.add(response);
                    }
                } catch (Exception e) {
                    log.error("Error parsing device line: {}", line, e);
                }
            }
        }

        log.info("Network scan complete. Discovered {} devices.", discoveredDevices.size());
        return discoveredDevices;
    }

    private String runPowerShell(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", command);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            p.waitFor();
            return output.toString().trim();
        } catch (Exception e) {
            log.error("Failed to execute PowerShell: {}", command, e);
            return "ERROR: " + e.getMessage();
        }
    }

    private DeviceResponse convertToResponse(Device device) {
        return DeviceResponse.builder()
                .id(device.getId())
                .userId(device.getUser().getId())
                .username(device.getUser().getUsername())
                .deviceName(device.getDeviceName())
                .macAddress(device.getMacAddress())
                .ipAddress(device.getIpAddress())
                .deviceType(device.getDeviceType())
                .osInfo(device.getOsInfo())
                .browserInfo(device.getBrowserInfo())
                .status(device.getStatus().name())
                .totalBandwidthUsed(device.getTotalBandwidthUsed())
                .firstSeen(device.getFirstSeen())
                .lastSeen(device.getLastSeen())
                .build();
    }
}
