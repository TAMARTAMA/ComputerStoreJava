package com.tamar.computerstore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderLineRequest(

        @NotNull
        @Schema(example = "1")
        Long productId,

        @Positive
        @Schema(example = "2")
        int quantity
) {
}
