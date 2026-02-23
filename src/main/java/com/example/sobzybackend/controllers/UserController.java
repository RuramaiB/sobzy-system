package com.example.sobzybackend.controllers;


import com.example.sobzybackend.dtos.UserProfileResponse;
import com.example.sobzybackend.enums.Role;
import com.example.sobzybackend.dtos.MessageResponse;
import com.example.sobzybackend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for user management operations
 * Handles user CRUD operations and queries (Admin operations)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {

    private final UserService userService;

    /**
     * Get all users with pagination
     * GET /api/v1/users
     */
    @GetMapping
    public ResponseEntity<Page<UserProfileResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        log.info("Get all users request - page: {}, size: {}", page, size);

        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC") ?
                Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<UserProfileResponse> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    /**
     * Get user by ID
     * GET /api/v1/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResponse> getUserById(@PathVariable Long id) {
        log.info("Get user by ID request: {}", id);
        UserProfileResponse user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * Get user by username
     * GET /api/v1/users/username/{username}
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<UserProfileResponse> getUserByUsername(@PathVariable String username) {
        log.info("Get user by username request: {}", username);
        UserProfileResponse user = userService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    /**
     * Get users by role
     * GET /api/v1/users/role/{role}
     */
    @GetMapping("/role/{role}")
    public ResponseEntity<List<UserProfileResponse>> getUsersByRole(@PathVariable String role) {
        log.info("Get users by role request: {}", role);
        Role userRole = Role.valueOf(role.toUpperCase());
        List<UserProfileResponse> users = userService.getUsersByRole(userRole);
        return ResponseEntity.ok(users);
    }

    /**
     * Get active users
     * GET /api/v1/users/active
     */
    @GetMapping("/active")
    public ResponseEntity<List<UserProfileResponse>> getActiveUsers() {
        log.info("Get active users request");
        List<UserProfileResponse> users = userService.getActiveUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Get locked users
     * GET /api/v1/users/locked
     */
    @GetMapping("/locked")
    public ResponseEntity<List<UserProfileResponse>> getLockedUsers() {
        log.info("Get locked users request");
        List<UserProfileResponse> users = userService.getLockedUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Search users
     * GET /api/v1/users/search
     */
    @GetMapping("/search")
    public ResponseEntity<List<UserProfileResponse>> searchUsers(@RequestParam String query) {
        log.info("Search users request with query: {}", query);
        List<UserProfileResponse> users = userService.searchUsers(query);
        return ResponseEntity.ok(users);
    }

    /**
     * Lock user account
     * POST /api/v1/users/{id}/lock
     */
    @PostMapping("/{id}/lock")
    public ResponseEntity<MessageResponse> lockUser(@PathVariable Long id) {
        log.info("Lock user request: {}", id);
        userService.lockUser(id);
        return ResponseEntity.ok(MessageResponse.success("User locked successfully"));
    }

    /**
     * Unlock user account
     * POST /api/v1/users/{id}/unlock
     */
    @PostMapping("/{id}/unlock")
    public ResponseEntity<MessageResponse> unlockUser(@PathVariable Long id) {
        log.info("Unlock user request: {}", id);
        userService.unlockUser(id);
        return ResponseEntity.ok(MessageResponse.success("User unlocked successfully"));
    }

    /**
     * Activate user account
     * POST /api/v1/users/{id}/activate
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<MessageResponse> activateUser(@PathVariable Long id) {
        log.info("Activate user request: {}", id);
        userService.activateUser(id);
        return ResponseEntity.ok(MessageResponse.success("User activated successfully"));
    }

    /**
     * Deactivate user account
     * POST /api/v1/users/{id}/deactivate
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<MessageResponse> deactivateUser(@PathVariable Long id) {
        log.info("Deactivate user request: {}", id);
        userService.deactivateUser(id);
        return ResponseEntity.ok(MessageResponse.success("User deactivated successfully"));
    }

    /**
     * Delete user
     * DELETE /api/v1/users/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable Long id) {
        log.info("Delete user request: {}", id);
        userService.deleteUser(id);
        return ResponseEntity.ok(MessageResponse.success("User deleted successfully"));
    }

    /**
     * Get user count
     * GET /api/v1/users/count
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getUserCount() {
        log.info("Get user count request");
        long count = userService.getUserCount();
        return ResponseEntity.ok(count);
    }

    /**
     * Get active user count
     * GET /api/v1/users/count/active
     */
    @GetMapping("/count/active")
    public ResponseEntity<Long> getActiveUserCount() {
        log.info("Get active user count request");
        long count = userService.getActiveUserCount();
        return ResponseEntity.ok(count);
    }

    /**
     * Check if username exists
     * GET /api/v1/users/exists/username/{username}
     */
    @GetMapping("/exists/username/{username}")
    public ResponseEntity<Boolean> existsByUsername(@PathVariable String username) {
        log.info("Check username exists: {}", username);
        boolean exists = userService.existsByUsername(username);
        return ResponseEntity.ok(exists);
    }

    /**
     * Check if email exists
     * GET /api/v1/users/exists/email/{email}
     */
    @GetMapping("/exists/email/{email}")
    public ResponseEntity<Boolean> existsByEmail(@PathVariable String email) {
        log.info("Check email exists: {}", email);
        boolean exists = userService.existsByEmail(email);
        return ResponseEntity.ok(exists);
    }
}
