package com.samagan.smartlead.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateFormRequest(
        @NotBlank(message = "Form name is required")
        @Size(max = 100, message = "Form name cannot exceed 100 characters")
        String name
) {}
