package com.budgetmanager.backend.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Builder
@Getter
@Setter // Adding Getter/Setter explicitly for clarity with validation later
public class ChangePasswordRequest {
    private String currentPassword;
    private String newPassword;
    private String confirmationPassword; // For frontend validation
}