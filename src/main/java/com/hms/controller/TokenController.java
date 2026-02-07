package com.hms.controller;

import com.hms.dto.response.ApiResponse;
import com.hms.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class TokenController {

    private final JwtUtil jwtUtil;

    @GetMapping("/verify-token")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyToken(
            @RequestHeader("Authorization") String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid authorization header"));
        }

        String token = authHeader.substring(7);

        try {
            String username = jwtUtil.extractUsername(token);
            String role = jwtUtil.extractRole(token);

            Map<String, Object> data = new HashMap<>();
            data.put("username", username);
            data.put("role", role);
            data.put("valid", true);

            return ResponseEntity.ok(ApiResponse.success("Token is valid", data));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid or expired token"));
        }
    }
}
