package com.example.sobzybackend.controllers;

import com.example.sobzybackend.dtos.DeviceRegistrationRequest;
import com.example.sobzybackend.service.DeviceService;
import com.example.sobzybackend.service.PortalService;
import com.example.sobzybackend.repository.UserRepository;
import com.example.sobzybackend.users.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/portal")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PortalController {

    private final PortalService portalService;
    private final DeviceService deviceService;
    private final UserRepository userRepository;

    @PostMapping("/login-success")
    public ResponseEntity<Map<String, String>> loginSuccess(
            @RequestBody Map<String, String> payload,
            HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        String username = payload.getOrDefault("username", "guest");
        log.info("Portal login success for User: {}, IP: {}", username, clientIp);

        // 1. Authenticate IP in PortalService
        portalService.authenticateIp(clientIp, username);

        // 2. Automated Device Registration (Phone details linkage)
        try {
            String mac = portalService.getMacForIp(clientIp);
            if (mac != null) {
                User user = userRepository.findByUsername(username).orElse(null);
                if (user != null) {
                    DeviceRegistrationRequest regRequest = DeviceRegistrationRequest.builder()
                            .userId(user.getId())
                            .macAddress(mac)
                            .ipAddress(clientIp)
                            .deviceName("Mobile Device (" + username + ")")
                            .deviceType("Mobile")
                            .osInfo(request.getHeader("User-Agent"))
                            .browserInfo("Captive Portal Browser")
                            .build();
                    deviceService.registerDevice(regRequest);
                    log.info("Auto-registered device for MAC: {}", mac);
                }
            }
        } catch (Exception e) {
            log.error("Failed to auto-register device: {}", e.getMessage());
        }

        return ResponseEntity.ok(Map.of(
                "message", "IP authenticated successfully",
                "ip", clientIp,
                "user", username,
                "certUrl", "http://google.com/____iwacs_cert",
                "setupInstructions",
                "For full security monitoring, please download and INSTALL the certificate from the link below, then 'Trust' it in your device settings."));
    }

    @GetMapping("/check-auth")
    public ResponseEntity<Map<String, Object>> checkAuth(HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        boolean authenticated = portalService.isIpAuthenticated(clientIp);
        return ResponseEntity.ok(Map.of("authenticated", authenticated, "ip", clientIp));
    }
}
