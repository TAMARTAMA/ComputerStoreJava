package com.tamar.computerstore.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API error envelope")
public record ApiErrorResponse(
        Instant timestamp,
        @Schema(example = "401") int status,
        @Schema(example = "Unauthorized") String error,
        @Schema(example = "Invalid credentials") String message,
        @Schema(example = "/api/auth/login") String path,
        Map<String, String> fieldErrors
) {

    public ApiErrorResponse(Instant timestamp, int status, String error, String message, String path) {
        this(timestamp, status, error, message, path, null);
    }
}
