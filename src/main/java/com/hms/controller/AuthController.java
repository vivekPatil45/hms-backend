package com.hms.controller;

import com.hms.dto.request.LoginRequest;
import com.hms.dto.request.RegisterRequest;
import com.hms.dto.response.ApiResponse;
import com.hms.dto.response.AuthResponse;
import com.hms.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/check")
    public String hello() {
        return new String("Hello");
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, String>>> register(@Valid @RequestBody RegisterRequest request) {
        ApiResponse<Map<String, String>> response = authService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        ApiResponse<AuthResponse> response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        ApiResponse<Void> response = authService.logout();
        return ResponseEntity.ok(response);
    }

    /**
     * Create demo admin user for testing
     * Username: admin
     * Email: admin@hotel.com
     * Password: admin123
     */
    @PostMapping("/create-demo-admin")
    public ResponseEntity<ApiResponse<Map<String, String>>> createDemoAdmin() {
        ApiResponse<Map<String, String>> response = authService.createDemoAdmin();
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
