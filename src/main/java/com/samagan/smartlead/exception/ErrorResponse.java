package com.samagan.smartlead.exception;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        int status,
        String error,
        String message,
        String path,
        Instant timestamp,
        Map<String, String> validationErrors
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(status, error, message, path, Instant.now(), null);
    }

    public static ErrorResponse validation(int status, String error, String message, String path, Map<String, String> errors) {
        return new ErrorResponse(status, error, message, path, Instant.now(), errors);
    }
}