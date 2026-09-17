package com.tamar.computerstore.dto;

import com.tamar.computerstore.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param password BCrypt only considers the first 72 UTF-8 bytes, so the upper bound is enforced
 *                 on byte length (not Java character count) and longer inputs are rejected rather
 *                 than silently truncated.
 */
public record RegisterRequest(

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 12, message = "Password must be at least 12 characters")
        @MaxUtf8Bytes(value = 72, message = "Password must not exceed 72 UTF-8 bytes")
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
