package com.tamar.computerstore.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(

        @NotBlank(message = "JWT_SECRET must be set; generate one with: openssl rand -base64 48")
        @Size(min = 32, message = "JWT_SECRET must be at least 32 characters (256 bits) for HS256")
        String secret,

        @NotNull(message = "JWT_EXPIRATION must be an ISO-8601 duration such as PT1H")
        Duration expiration,

        @NotBlank
        String issuer
) {
}
