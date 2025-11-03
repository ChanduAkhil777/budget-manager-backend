package com.budgetmanager.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    // Existing fields
    private String username;
    private String password;

    // --- NEW FIELDS ---
    private String fullName;
    private String email;
    private String village;
    private String phoneNumber;
    // No need for confirmPassword here, that's frontend only
}