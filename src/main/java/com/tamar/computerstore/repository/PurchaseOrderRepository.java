package com.tamar.computerstore.repository;

import com.tamar.computerstore.entity.OrderStatus;
import com.tamar.computerstore.entity.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @EntityGraph(attributePaths = {"items", "items.product", "customer"})
    @Query("SELECT o FROM PurchaseOrder o WHERE o.id = :id")
    Optional<PurchaseOrder> findDetailedById(@Param("id") Long id);

    Page<PurchaseOrder> findByCustomerId(Long customerId, Pageable pageable);

    Page<PurchaseOrder> findByStatus(OrderStatus status, Pageable pageable);

    boolean existsByCustomerId(Long customerId);
}
