package com.tamar.computerstore.repository;

import com.tamar.computerstore.entity.Product;
import com.tamar.computerstore.entity.ProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    Page<Product> findByCategory(ProductCategory category, Pageable pageable);

    @Query("""
            SELECT p FROM Product p
            WHERE (:category IS NULL OR p.category = :category)
              AND (
                   :q IS NULL
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '\\'
                   OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '\\'
              )
            """)
    Page<Product> search(@Param("category") ProductCategory category,
                         @Param("q") String q,
                         Pageable pageable);
}
