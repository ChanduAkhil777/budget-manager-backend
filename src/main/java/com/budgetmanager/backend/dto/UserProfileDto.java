package com.budgetmanager.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder // Make sure @Builder is present
public class UserProfileDto {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String village; // Include village
    private String phoneNumber; // Include phone number
    private String profilePhotoUrl;
}