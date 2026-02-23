package com.example.sobzybackend.controllers;

import com.example.sobzybackend.dtos.*;
import com.example.sobzybackend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.auth.InvalidCredentialsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.security.auth.login.AccountLockedException;

/**
 * REST Controller for authentication operations with JWT
 * Handles user registration, login, logout, token refresh, and profile management
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new user
     * POST /api/v1/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for username: {}", request.getUsername());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Login user and get JWT tokens
     * POST /api/v1/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) throws AccountLockedException, InvalidCredentialsException {
        log.info("Login request received for username: {}", request.getUsername());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh access token using refresh token
     * POST /api/v1/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestHeader("Authorization") String refreshToken) throws InvalidCredentialsException {
        log.info("Token refresh request received");
        String token = refreshToken.replace("Bearer ", "");
        AuthResponse response = authService.refreshToken(token);
        return ResponseEntity.ok(response);
    }

    /**
     * Logout user (client should discard tokens)
     * POST /api/v1/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout() {
        log.info("Logout request received");
        MessageResponse response = authService.logout();
        return ResponseEntity.ok(response);
    }

    /**
     * Get current authenticated user profile
     * GET /api/v1/auth/profile
     */
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getCurrentProfile() {
        log.info("Get current user profile request");
        UserProfileResponse response = authService.getCurrentUserProfile();
        return ResponseEntity.ok(response);
    }

    /**
     * Update current user profile
     * PUT /api/v1/auth/profile
     */
    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        log.info("Profile update request");
        UserProfileResponse response = authService.updateCurrentUserProfile(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Change password for current user
     * POST /api/v1/auth/change-password
     */
    @PostMapping("/change-password")
    public ResponseEntity<MessageResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        log.info("Password change request");
        MessageResponse response = authService.changePassword(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint
     * GET /api/v1/auth/health
     */
    @GetMapping("/health")
    public ResponseEntity<MessageResponse> health() {
        return ResponseEntity.ok(MessageResponse.success("Authentication service is running"));
    }
}