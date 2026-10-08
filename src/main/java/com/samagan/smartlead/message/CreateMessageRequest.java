package com.samagan.smartlead.message;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMessageRequest(
        @NotBlank(message = "Sender name is required")
        @Size(max = 100, message = "Sender name cannot exceed 100 characters")
        String senderName,

        @NotBlank(message = "Sender email is required")
        @Email(message = "Must provide a valid email address")
        @Size(max = 254, message = "Sender email cannot exceed 254 characters")
        String senderEmail,

        @NotBlank(message = "Content is required")
        @Size(min = 1, max = 5000, message = "Content must be between 1 and 5000 characters")
        String content
) {}
