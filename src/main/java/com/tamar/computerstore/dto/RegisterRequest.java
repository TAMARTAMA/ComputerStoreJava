package com.tamar.computerstore.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param password BCrypt only considers the first 72 bytes, so longer inputs are rejected
 *                 rather than silently truncated.
 */
public record RegisterRequest(

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 12, max = 72, message = "Password must be between 12 and 72 characters")
        String password,

        @NotBlank
        @Size(max = 100)
        String firstName,

        @NotBlank
        @Size(max = 100)
        String lastName,

        @Size(max = 30)
        String phone,

        @Size(max = 255)
        String address
) {
}
