package com.hms.controller;

import com.hms.dto.request.CreateUserRequest;
import com.hms.dto.request.UpdateUserRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.dto.response.CreateUserResponse;
import com.hms.dto.response.UserListResponse;
import com.hms.dto.response.UserResponse;
import com.hms.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementController {

    private final UserManagementService userManagementService;

    /**
     * Create a new user account (Admin only)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CreateUserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        ApiResponse<CreateUserResponse> response = userManagementService.createUser(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get all users with pagination, search, filter, and sort (Admin only)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<UserListResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        ApiResponse<UserListResponse> response = userManagementService.getAllUsers(
                page, size, username, role, status, sortBy, sortDir);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user details by ID (Admin only)
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable String userId) {
        ApiResponse<UserResponse> response = userManagementService.getUserById(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update user account (Admin only)
     */
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserRequest request) {
        ApiResponse<UserResponse> response = userManagementService.updateUser(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Reset user password (Admin only)
     */
    @PostMapping("/{userId}/reset-password")
    public ResponseEntity<ApiResponse<Map<String, String>>> resetPassword(@PathVariable String userId) {
        ApiResponse<Map<String, String>> response = userManagementService.resetPassword(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Deactivate user account (Admin only)
     */
    @PutMapping("/{userId}/deactivate")
    public ResponseEntity<ApiResponse<UserResponse>> deactivateUser(@PathVariable String userId) {
        ApiResponse<UserResponse> response = userManagementService.deactivateUser(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Activate user account (Admin only)
     */
    @PutMapping("/{userId}/activate")
    public ResponseEntity<ApiResponse<UserResponse>> activateUser(@PathVariable String userId) {
        ApiResponse<UserResponse> response = userManagementService.activateUser(userId);
        return ResponseEntity.ok(response);
    }
}
