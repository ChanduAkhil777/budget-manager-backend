package com.budgetmanager.backend.dto;

import lombok.Data;

@Data // Make sure @Data is present
public class UpdateProfileRequest {
    private String fullName;
    private String email;
    private String village;
    private String phoneNumber;
    // Add village and phoneNumber here if you want them editable
    // private String village;
    // private String phoneNumber;
}