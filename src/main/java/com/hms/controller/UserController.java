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

        // Get userId from authentication (username is userId in our system)
        String userId = authentication.getName();

        ApiResponse<Void> response = userManagementService.changePassword(userId, request);
        return ResponseEntity.ok(response);
    }
}
