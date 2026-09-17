package com.tamar.computerstore.controller;

import com.tamar.computerstore.dto.CreateOrderRequest;
import com.tamar.computerstore.dto.OrderResponse;
import com.tamar.computerstore.dto.PageResponse;
import com.tamar.computerstore.dto.UpdateOrderStatusRequest;
import com.tamar.computerstore.entity.OrderStatus;
import com.tamar.computerstore.security.AuthenticatedUser;
import com.tamar.computerstore.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@AuthenticationPrincipal AuthenticatedUser principal,
                                                    @Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(principal, request));
    }

    @GetMapping("/me")
    public ResponseEntity<PageResponse<OrderResponse>> listMyOrders(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size
    ) {
        return ResponseEntity.ok(orderService.listMyOrders(principal, page, size));
    }

    @GetMapping
    public ResponseEntity<PageResponse<OrderResponse>> listAll(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size
    ) {
        return ResponseEntity.ok(orderService.listAll(status, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(@AuthenticationPrincipal AuthenticatedUser principal,
                                                 @PathVariable Long id) {
        return ResponseEntity.ok(orderService.getById(principal, id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(orderService.updateStatus(id, request));
    }
}
