package com.example.sobzybackend.controllers;

import com.example.sobzybackend.dtos.DeviceRegistrationRequest;
import com.example.sobzybackend.service.DeviceService;
import com.example.sobzybackend.service.PortalService;
import com.example.sobzybackend.repository.UserRepository;
import com.example.sobzybackend.users.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getHeader("X-Real-IP");
        }
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }
        
        // Debug Log all headers to find real client IP
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        StringBuilder sb = new StringBuilder();
        while (headerNames.hasMoreElements()) {
            String h = headerNames.nextElement();
            sb.append(h).append("=").append(request.getHeader(h)).append(", ");
        }

        String email = payload.getOrDefault("email", payload.getOrDefault("username", "guest@hit.ac.zw"));
        
        log.info("AUTH_DEBUG: Portal Login. ResolvedIp={}, RemoteAddr={}, Headers=[{}], Email={}", 
            clientIp, request.getRemoteAddr(), sb.toString(), email);

        // 1. Ensure User exists in DB for this email
        User user = userRepository.findByUsername(email).orElse(null);
        if (user == null) {
            user = User.builder()
                    .username(email)
                    .email(email)
                    .password(java.util.UUID.randomUUID().toString())
                    .fullName(email.split("@")[0])
                    .role(com.example.sobzybackend.enums.Role.USER)
                    .build();
            userRepository.save(user);
            log.info("Created new dummy user for Captive Portal email: {}", email);
        }

        // 2. Authenticate IP in PortalService
        portalService.authenticateIp(clientIp, email);

        // 3. Automated Device Registration (Phone details linkage)
        try {
            String mac = portalService.getMacForIp(clientIp);
            if (mac != null) {
                DeviceRegistrationRequest regRequest = DeviceRegistrationRequest.builder()
                        .userId(user.getId())
                        .macAddress(mac)
                        .ipAddress(clientIp)
                        .deviceName("Mobile Device (" + email + ")")
                        .deviceType("Mobile")
                        .osInfo(request.getHeader("User-Agent"))
                        .browserInfo("Captive Portal Browser")
                        .build();
                deviceService.registerDevice(regRequest);
                log.info("Auto-registered device for MAC: {}", mac);
            }
        } catch (Exception e) {
            log.error("Failed to auto-register device: {}", e.getMessage());
        }

        return ResponseEntity.ok(Map.of(
                "message", "IP authenticated successfully",
                "ip", clientIp,
                "user", email,
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

    @GetMapping(value = "/ca-cert", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadCaCert() {
        Resource resource = new ClassPathResource("ca.crt");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"iwacs-ca.crt\"")
                .body(resource);
    }
}
