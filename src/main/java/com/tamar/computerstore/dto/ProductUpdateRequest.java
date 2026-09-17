package com.tamar.computerstore.dto;

import com.tamar.computerstore.entity.ProductCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * @param version the {@code Product.version} the client last observed; a mismatch yields HTTP 409
 */
public record ProductUpdateRequest(

        @NotBlank
        @Size(max = 64)
        String sku,

        @NotBlank
        @Size(max = 150)
        String name,

        @Size(max = 1000)
        String description,

        @NotNull
        @DecimalMin(value = "0.00", inclusive = true)
        @Digits(integer = 10, fraction = 2)
        BigDecimal price,

        @PositiveOrZero
        int stockQuantity,

        @NotNull
        ProductCategory category,

        @NotNull
        @PositiveOrZero
        Long version
) {
}
