package com.tamar.computerstore.dto;

import com.tamar.computerstore.validation.MaxUtf8Bytes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param password BCrypt only considers the first 72 UTF-8 bytes, so the upper bound is enforced
 *                 on byte length (not Java character count) and longer inputs are rejected rather
 *                 than silently truncated.
 */
@Schema(example = "{\"email\":\"dana@example.com\",\"password\":\"Str0ng-Passw0rd!\",\"firstName\":\"Dana\",\"lastName\":\"Levi\"}")
public record RegisterRequest(

        @NotBlank
        @Email
        @Size(max = 255)
        @Schema(example = "dana@example.com")
        String email,

        @NotBlank
        @Size(min = 12, message = "Password must be at least 12 characters")
        @MaxUtf8Bytes(value = 72, message = "Password must not exceed 72 UTF-8 bytes")
        @Schema(example = "Str0ng-Passw0rd!", minLength = 12)
        String password,

        @NotBlank
        @Size(max = 100)
        @Schema(example = "Dana")
        String firstName,

        @NotBlank
        @Size(max = 100)
        @Schema(example = "Levi")
        String lastName,

        @Size(max = 30)
        String phone,

        @Size(max = 255)
        String address
) {
}
