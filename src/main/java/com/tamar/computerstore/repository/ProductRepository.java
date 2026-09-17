package com.tamar.computerstore.repository;

import com.tamar.computerstore.entity.Product;
import com.tamar.computerstore.entity.ProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    Page<Product> findByCategory(ProductCategory category, Pageable pageable);
}
