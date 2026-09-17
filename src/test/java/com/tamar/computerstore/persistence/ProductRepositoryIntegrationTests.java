package com.tamar.computerstore.persistence;

import com.tamar.computerstore.entity.Product;
import com.tamar.computerstore.entity.ProductCategory;
import com.tamar.computerstore.repository.ProductRepository;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class ProductRepositoryIntegrationTests extends AbstractMySqlIntegrationTests {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void savesAndReloadsProduct() {
        Product saved = productRepository.saveAndFlush(
                new Product("SKU-GPU-001", "Graphics Card", new BigDecimal("1299.99"), 7, ProductCategory.HARDWARE)
        );

        Optional<Product> reloaded = productRepository.findBySku("SKU-GPU-001");

        assertThat(saved.getId()).isNotNull();
        assertThat(reloaded).isPresent().get().satisfies(product -> {
            assertThat(product.getName()).isEqualTo("Graphics Card");
            assertThat(product.getPrice()).isEqualByComparingTo("1299.99");
            assertThat(product.getStockQuantity()).isEqualTo(7);
            assertThat(product.getCategory()).isEqualTo(ProductCategory.HARDWARE);
            assertThat(product.getCreatedAt()).isNotNull();
            assertThat(product.getUpdatedAt()).isNotNull();
        });
    }

    @Test
    void rejectsDuplicateSku() {
        productRepository.saveAndFlush(
                new Product("SKU-CPU-001", "Processor", new BigDecimal("899.00"), 3, ProductCategory.HARDWARE)
        );

        assertThatThrownBy(() -> productRepository.saveAndFlush(
                new Product("SKU-CPU-001", "Another Processor", new BigDecimal("799.00"), 1, ProductCategory.HARDWARE)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsNegativePrice() {
        assertThatThrownBy(() -> productRepository.saveAndFlush(
                new Product("SKU-OS-001", "Operating System", new BigDecimal("-1.00"), 5, ProductCategory.SOFTWARE)
        )).isInstanceOf(ConstraintViolationException.class);
    }
}
