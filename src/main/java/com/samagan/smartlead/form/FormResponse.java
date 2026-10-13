package com.samagan.smartlead.form;

import java.time.Instant;
import java.util.UUID;

public record FormResponse(
        UUID id,
        String name,
        String formKey,
        Instant createdAt
) {
    public static FormResponse fromEntity(Form form) {
        return new FormResponse(form.getId(), form.getName(), form.getFormKey(), form.getCreatedAt());
    }
}
