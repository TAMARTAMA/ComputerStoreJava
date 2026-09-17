package com.tamar.computerstore.dto;

import com.tamar.computerstore.entity.ProductCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(example = "{\"sku\":\"SKU-GPU-001\",\"name\":\"Graphics Card\",\"description\":\"High-end GPU\",\"price\":1299.99,\"stockQuantity\":7,\"category\":\"HARDWARE\"}")
public record ProductCreateRequest(

        @NotBlank
        @Size(max = 64)
        @Schema(example = "SKU-GPU-001")
        String sku,

        @NotBlank
        @Size(max = 150)
        @Schema(example = "Graphics Card")
        String name,

        @Size(max = 1000)
        String description,

        @NotNull
        @DecimalMin(value = "0.00", inclusive = true)
        @Digits(integer = 10, fraction = 2)
        @Schema(example = "1299.99")
        BigDecimal price,

        @PositiveOrZero
        @Schema(example = "7")
        int stockQuantity,

        @NotNull
        @Schema(example = "HARDWARE")
        ProductCategory category
) {
}
