package com.tamar.computerstore.mapper;

import com.tamar.computerstore.dto.ProductCreateRequest;
import com.tamar.computerstore.dto.ProductResponse;
import com.tamar.computerstore.dto.ProductUpdateRequest;
import com.tamar.computerstore.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toEntity(ProductCreateRequest request) {
        Product product = new Product(
                normalizeSku(request.sku()),
                request.name().trim(),
                request.price(),
                request.stockQuantity(),
                request.category()
        );
        product.setDescription(trimToNull(request.description()));
        return product;
    }

    public void applyUpdate(Product product, ProductUpdateRequest request) {
        product.setSku(normalizeSku(request.sku()));
        product.setName(request.name().trim());
        product.setDescription(trimToNull(request.description()));
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setCategory(request.category());
    }

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getCategory(),
                product.getVersion(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    private static String normalizeSku(String sku) {
        return sku.trim();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
