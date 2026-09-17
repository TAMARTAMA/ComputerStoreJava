package com.tamar.computerstore.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderLineRequest(

        @NotNull
        Long productId,

        @Positive
        int quantity
) {
}
