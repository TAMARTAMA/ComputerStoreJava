package com.tamar.computerstore.mapper;

import com.tamar.computerstore.dto.OrderItemResponse;
import com.tamar.computerstore.dto.OrderResponse;
import com.tamar.computerstore.entity.OrderItem;
import com.tamar.computerstore.entity.PurchaseOrder;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class OrderMapper {

    public OrderResponse toResponse(PurchaseOrder order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomer().getId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getItems().stream()
                        .sorted(Comparator.comparing(OrderItem::getId, Comparator.nullsLast(Long::compareTo)))
                        .map(this::toItemResponse)
                        .toList()
        );
    }

    public OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getSku(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal()
        );
    }
}
