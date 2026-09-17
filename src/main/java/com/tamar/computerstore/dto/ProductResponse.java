package com.tamar.computerstore.dto;

import com.tamar.computerstore.entity.ProductCategory;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        int stockQuantity,
        ProductCategory category,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
}
