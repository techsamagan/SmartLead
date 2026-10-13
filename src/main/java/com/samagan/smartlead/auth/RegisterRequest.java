package com.samagan.smartlead.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Must provide a valid email address")
        @Size(max = 254, message = "Email cannot exceed 254 characters")
        String email,

        // BCrypt ignores everything past 72 bytes
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password
) {}
