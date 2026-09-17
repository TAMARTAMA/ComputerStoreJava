package com.tamar.computerstore.repository;

import com.tamar.computerstore.entity.OrderStatus;
import com.tamar.computerstore.entity.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    Page<PurchaseOrder> findByCustomerId(Long customerId, Pageable pageable);

    List<PurchaseOrder> findByStatus(OrderStatus status);
}
