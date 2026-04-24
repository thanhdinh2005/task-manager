package com.thanh.taskmanager.dto.request.auth;

import jakarta.validation.constraints.*;
import lombok.Getter;

@Getter
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "New password must be at least 8 characters")
    private String newPassword;
}
