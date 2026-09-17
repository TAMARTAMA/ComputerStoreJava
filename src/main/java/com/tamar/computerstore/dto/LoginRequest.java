package com.tamar.computerstore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(example = "{\"email\":\"dana@example.com\",\"password\":\"Str0ng-Passw0rd!\"}")
public record LoginRequest(

        @NotBlank
        @Size(max = 255)
        @Schema(example = "dana@example.com")
        String email,

        @NotBlank
        @Size(max = 72)
        @Schema(example = "Str0ng-Passw0rd!")
        String password
) {
}
