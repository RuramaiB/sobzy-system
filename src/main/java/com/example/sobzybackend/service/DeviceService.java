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
    public List<DeviceResponse> scanNetwork() {
        log.info("Starting network scan simulation...");
        // In a real scenario, this would use nmap or similar tools
        // We simulate finding 2 new devices
        User admin = userRepository.findByUsername("admin").orElse(null);
        if (admin == null)
            admin = userRepository.findAll().get(0);

        Device d1 = Device.builder()
                .user(admin)
                .deviceName("Laboratory-PC-09")
                .macAddress("DE:AD:BE:EF:01:23")
                .ipAddress("192.168.1.109")
                .deviceType("Desktop")
                .status(DeviceStatus.ACTIVE)
                .lastSeen(LocalDateTime.now())
                .build();

        Device d2 = Device.builder()
                .user(admin)
                .deviceName("Unknown-Android-Device")
                .macAddress("CA:FE:BA:BE:44:55")
                .ipAddress("192.168.1.144")
                .deviceType("Mobile")
                .status(DeviceStatus.ACTIVE)
                .lastSeen(LocalDateTime.now())
                .build();

        deviceRepository.save(d1);
        deviceRepository.save(d2);

        log.info("Network scan complete. Discovered 2 new devices.");
        return List.of(convertToResponse(d1), convertToResponse(d2));
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
