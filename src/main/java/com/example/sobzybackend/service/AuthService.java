package com.example.sobzybackend.service;

import com.example.sobzybackend.config.JwtService;

import com.example.sobzybackend.dtos.*;
import com.example.sobzybackend.enums.*;
import com.example.sobzybackend.exceptions.*;
import com.example.sobzybackend.repository.*;
import com.example.sobzybackend.users.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.auth.InvalidCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.security.auth.login.AccountLockedException;

/**
 * Service class for authentication operations with JWT
 * Handles user registration, login, logout, and profile management
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    private static final int MAX_LOGIN_ATTEMPTS = 5;

    /**
     * Register a new user
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Attempting to register user: {}", request.getUsername());

        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed: Username already exists - {}", request.getUsername());
            throw new UserAlreadyExistsException("username", request.getUsername());
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: Email already exists - {}", request.getEmail());
            throw new UserAlreadyExistsException("email", request.getEmail());
        }

        // Hash password
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // Create user entity
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(hashedPassword)
                .fullName(request.getFullName())
                .role(Role.USER)
                .isActive(true)
                .isLocked(false)
                .failedLoginAttempts(0)
                .build();

        // Save user
        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getUsername());

        // Generate JWT tokens
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Build response
        return AuthResponse.builder()
                .success(true)
                .message("User registered successfully")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime())
                .user(buildUserResponse(user))
                .build();
    }

    /**
     * Login user with JWT authentication
     */
    @Transactional
    public AuthResponse login(LoginRequest request) throws AccountLockedException, InvalidCredentialsException {
        log.info("Login attempt for user: {}", request.getUsername());

        // Find user
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.warn("Login failed: User not found - {}", request.getUsername());
                    return new InvalidCredentialsException();
                });

        // Check if account is locked
        if (user.getIsLocked()) {
            log.warn("Login failed: Account locked - {}", request.getUsername());
            throw new AccountLockedException();
        }

        // Check if account is active
        if (!user.getIsActive()) {
            log.warn("Login failed: Account inactive - {}", request.getUsername());
            throw new AccountInactiveException();
        }

        try {
            // Authenticate user
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            // Successful login
            handleSuccessfulLogin(user);
            log.info("User logged in successfully: {}", user.getUsername());

            // Generate JWT tokens
            String accessToken = jwtService.generateToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            // Build response
            return AuthResponse.builder()
                    .success(true)
                    .message("Login successful")
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtService.getExpirationTime())
                    .user(buildUserResponse(user))
                    .build();

        } catch (org.springframework.security.core.AuthenticationException e) {
            handleFailedLogin(user);
            log.warn("Login failed: Invalid password for user - {}", request.getUsername());
            throw new InvalidCredentialsException();
        }
    }

    /**
     * Refresh access token
     */
    public AuthResponse refreshToken(String refreshToken) throws InvalidCredentialsException {
        log.info("Refresh token request");

        try {
            // Extract username from refresh token
            String username = jwtService.extractUsername(refreshToken);

            // Load user
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));

            // Validate refresh token
            if (!jwtService.isTokenValid(refreshToken, user)) {
                throw new InvalidCredentialsException("Invalid or expired refresh token");
            }

            // Generate new access token
            String newAccessToken = jwtService.generateToken(user);
            String newRefreshToken = jwtService.generateRefreshToken(user);

            log.info("Token refreshed successfully for user: {}", username);

            return AuthResponse.builder()
                    .success(true)
                    .message("Token refreshed successfully")
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtService.getExpirationTime())
                    .user(buildUserResponse(user))
                    .build();

        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            throw new InvalidCredentialsException("Token refresh failed");
        }
    }

    /**
     * Get current authenticated user profile
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile() {
        String username = getCurrentUsername();
        log.info("Fetching profile for current user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        return buildProfileResponse(user);
    }

    /**
     * Update current user profile
     */
    @Transactional
    public UserProfileResponse updateCurrentUserProfile(UpdateProfileRequest request) {
        String username = getCurrentUsername();
        log.info("Updating profile for current user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        // Update email if provided and different
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new UserAlreadyExistsException("email", request.getEmail());
            }
            user.setEmail(request.getEmail());
        }

        // Update full name if provided
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        user = userRepository.save(user);
        log.info("Profile updated successfully for user: {}", username);

        return buildProfileResponse(user);
    }

    /**
     * Change password for current user
     */
    @Transactional
    public MessageResponse changePassword(ChangePasswordRequest request) {
        String username = getCurrentUsername();
        log.info("Password change attempt for user: {}", username);

        // Validate passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new PasswordMismatchException();
        }

        // Find user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("Password change failed: Incorrect current password for user - {}", username);
            throw new IncorrectPasswordException();
        }

        // Hash and set new password
        String hashedPassword = passwordEncoder.encode(request.getNewPassword());
        user.setPassword(hashedPassword);
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", username);
        return MessageResponse.success("Password changed successfully");
    }

    /**
     * Logout user (stateless - client should discard token)
     */
    public MessageResponse logout() {
        String username = getCurrentUsername();
        log.info("User logged out: {}", username);
        return MessageResponse.success("Logged out successfully");
    }

    // ============================================================================
    // Helper Methods
    // ============================================================================

    private void handleFailedLogin(User user) {
        user.incrementFailedLoginAttempts();

        if (user.getFailedLoginAttempts() >= MAX_LOGIN_ATTEMPTS) {
            user.lock();
            log.warn("Account locked due to too many failed attempts: {}", user.getUsername());
        }

        userRepository.save(user);
    }

    private void handleSuccessfulLogin(User user) {
        user.resetFailedLoginAttempts();
        user.updateLastLogin();
        userRepository.save(user);
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }
        return authentication.getName();
    }

    private UserResponse buildUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .isActive(user.getIsActive())
                .build();
    }

    private UserProfileResponse buildProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .isActive(user.getIsActive())
                .isLocked(user.getIsLocked())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}