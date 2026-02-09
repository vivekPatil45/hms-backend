package com.hms.controller;

import com.hms.dto.request.ChangePasswordRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final com.hms.repository.UserRepository userRepository;
    private final UserManagementService userManagementService;

    /**
     * Change password (authenticated users)
     * Users can change their own password, especially required after first login
     * with auto-generated password
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {

        // Get username from authentication
        String username = authentication.getName();

        // Find user to get the correct UserId
        com.hms.entity.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.hms.exception.ResourceNotFoundException("User not found"));

        ApiResponse<Void> response = userManagementService.changePassword(user.getUserId(), request);
        return ResponseEntity.ok(response);
    }
}
