package com.example.sobzybackend.controllers;

import com.example.sobzybackend.dtos.DeviceRegistrationRequest;
import com.example.sobzybackend.dtos.DeviceResponse;
import com.example.sobzybackend.dtos.MessageResponse;
import com.example.sobzybackend.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping("/register")
    public ResponseEntity<DeviceResponse> registerDevice(
            @Valid @RequestBody DeviceRegistrationRequest request) {
        log.info("Device registration request for MAC: {}", request.getMacAddress());
        DeviceResponse response = deviceService.registerDevice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponse> getDeviceById(@PathVariable Long id) {
        log.info("Get device by ID: {}", id);
        DeviceResponse response = deviceService.getDeviceById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/mac/{macAddress}")
    public ResponseEntity<DeviceResponse> getDeviceByMacAddress(@PathVariable String macAddress) {
        log.info("Get device by MAC: {}", macAddress);
        DeviceResponse response = deviceService.getDeviceByMacAddress(macAddress);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DeviceResponse>> getDevicesByUserId(@PathVariable Long userId) {
        log.info("Get devices for user: {}", userId);
        List<DeviceResponse> devices = deviceService.getDevicesByUserId(userId);
        return ResponseEntity.ok(devices);
    }

    @GetMapping("/active")
    public ResponseEntity<List<DeviceResponse>> getActiveDevices() {
        log.info("Get active devices");
        List<DeviceResponse> devices = deviceService.getActiveDevices();
        return ResponseEntity.ok(devices);
    }

    @PostMapping("/{id}/block")
    public ResponseEntity<MessageResponse> blockDevice(@PathVariable Long id) {
        log.info("Block device: {}", id);
        deviceService.blockDevice(id);
        return ResponseEntity.ok(MessageResponse.success("Device blocked successfully"));
    }

    @PostMapping("/{id}/unblock")
    public ResponseEntity<MessageResponse> unblockDevice(@PathVariable Long id) {
        log.info("Unblock device: {}", id);
        deviceService.unblockDevice(id);
        return ResponseEntity.ok(MessageResponse.success("Device unblocked successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteDevice(@PathVariable Long id) {
        log.info("Delete device: {}", id);
        deviceService.deleteDevice(id);
        return ResponseEntity.ok(MessageResponse.success("Device deleted successfully"));
    }

    @PostMapping("/scan")
    public ResponseEntity<List<DeviceResponse>> scanNetwork() {
        log.info("Network scan requested");
        List<DeviceResponse> discovered = deviceService.scanNetwork();
        return ResponseEntity.ok(discovered);
    }
}
