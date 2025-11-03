package com.budgetmanager.backend.controller;

import com.budgetmanager.backend.dto.AuthResponse;
import com.budgetmanager.backend.dto.ChangePasswordRequest;
import com.budgetmanager.backend.dto.LoginRequest;
import com.budgetmanager.backend.dto.RegisterRequest;
import com.budgetmanager.backend.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map; // <-- Add this import

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    // Constructor injection
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Endpoint for user registration
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.ok(authService.register(request));
    }

    // Endpoint for user login
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(authService.login(request));
    }

    // Endpoint for changing password
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody ChangePasswordRequest request
    ) {
        try {
            authService.changePassword(request);
            // Return success message
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (IllegalArgumentException e) {
            // Return specific error for bad requests (e.g., wrong current password, passwords don't match)
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
             // Return unauthorized if the user somehow isn't properly authenticated
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // Generic server error for other unexpected issues
             return ResponseEntity.status(500).body(Map.of("error", "An unexpected error occurred"));
        }
    }
}