package com.example.sobzybackend.service;


import com.example.sobzybackend.dtos.UserProfileResponse;
import com.example.sobzybackend.enums.Role;
import com.example.sobzybackend.repository.UserRepository;
import com.example.sobzybackend.exceptions.ResourceNotFoundException;
import com.example.sobzybackend.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for user management operations
 * Handles CRUD operations and user queries
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Get all users with pagination
     */
    @Transactional(readOnly = true)
    public Page<UserProfileResponse> getAllUsers(Pageable pageable) {
        log.info("Fetching all users with pagination");
        return userRepository.findAll(pageable)
                .map(this::convertToProfileResponse);
    }

    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserById(Long id) {
        log.info("Fetching user by ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return convertToProfileResponse(user);
    }

    /**
     * Get user by username
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserByUsername(String username) {
        log.info("Fetching user by username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return convertToProfileResponse(user);
    }

    /**
     * Get all users by role
     */
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getUsersByRole(Role role) {
        log.info("Fetching users by role: {}", role);
        return userRepository.findAllByRole(role).stream()
                .map(this::convertToProfileResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all active users
     */
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getActiveUsers() {
        log.info("Fetching all active users");
        return userRepository.findAllByIsActiveTrue().stream()
                .map(this::convertToProfileResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all locked users
     */
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getLockedUsers() {
        log.info("Fetching all locked users");
        return userRepository.findAllByIsLockedTrue().stream()
                .map(this::convertToProfileResponse)
                .collect(Collectors.toList());
    }

    /**
     * Search users by term
     */
    @Transactional(readOnly = true)
    public List<UserProfileResponse> searchUsers(String searchTerm) {
        log.info("Searching users with term: {}", searchTerm);
        return userRepository.searchUsers(searchTerm).stream()
                .map(this::convertToProfileResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lock user account
     */
    @Transactional
    public void lockUser(Long userId) {
        log.info("Locking user account: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.lock();
        userRepository.save(user);
        log.info("User account locked: {}", user.getUsername());
    }

    /**
     * Unlock user account
     */
    @Transactional
    public void unlockUser(Long userId) {
        log.info("Unlocking user account: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.unlock();
        userRepository.save(user);
        log.info("User account unlocked: {}", user.getUsername());
    }

    /**
     * Activate user account
     */
    @Transactional
    public void activateUser(Long userId) {
        log.info("Activating user account: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setIsActive(true);
        userRepository.save(user);
        log.info("User account activated: {}", user.getUsername());
    }

    /**
     * Deactivate user account
     */
    @Transactional
    public void deactivateUser(Long userId) {
        log.info("Deactivating user account: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setIsActive(false);
        userRepository.save(user);
        log.info("User account deactivated: {}", user.getUsername());
    }

    /**
     * Delete user
     */
    @Transactional
    public void deleteUser(Long userId) {
        log.info("Deleting user: {}", userId);
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        userRepository.deleteById(userId);
        log.info("User deleted: {}", userId);
    }

    /**
     * Get user count
     */
    @Transactional(readOnly = true)
    public long getUserCount() {
        return userRepository.count();
    }

    /**
     * Get active user count
     */
    @Transactional(readOnly = true)
    public long getActiveUserCount() {
        return userRepository.countByIsActiveTrue();
    }

    /**
     * Get user count by role
     */
    @Transactional(readOnly = true)
    public long getUserCountByRole(Role role) {
        return userRepository.countByRole(role);
    }

    /**
     * Get inactive users since date
     */
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getInactiveUsersSince(LocalDateTime date) {
        log.info("Fetching inactive users since: {}", date);
        return userRepository.findInactiveUsersSince(date).stream()
                .map(this::convertToProfileResponse)
                .collect(Collectors.toList());
    }

    /**
     * Check if user exists by username
     */
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Check if user exists by email
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // Helper method to convert User to UserProfileResponse
    private UserProfileResponse convertToProfileResponse(User user) {
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