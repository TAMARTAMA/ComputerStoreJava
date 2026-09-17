package com.tamar.computerstore.controller;

import com.tamar.computerstore.config.OpenApiConfig;
import com.tamar.computerstore.dto.CreateOrderRequest;
import com.tamar.computerstore.dto.OrderResponse;
import com.tamar.computerstore.dto.PageResponse;
import com.tamar.computerstore.dto.UpdateOrderStatusRequest;
import com.tamar.computerstore.entity.OrderStatus;
import com.tamar.computerstore.exception.ApiErrorResponse;
import com.tamar.computerstore.security.AuthenticatedUser;
import com.tamar.computerstore.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Orders", description = "Transactional order placement and status updates")
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT_SCHEME)
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Place order", description = "Role: CUSTOMER. Prices and totals are calculated server-side; stock decremented in one transaction.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order created",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid order request",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Not CUSTOMER",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Insufficient stock or inventory conflict",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<OrderResponse> placeOrder(@AuthenticationPrincipal AuthenticatedUser principal,
                                                    @Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(principal, request));
    }

    @GetMapping("/me")
    @Operation(summary = "My orders", description = "Role: CUSTOMER. Newest first, paginated.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of orders"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Not CUSTOMER",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<OrderResponse>> listMyOrders(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size
    ) {
        return ResponseEntity.ok(orderService.listMyOrders(principal, page, size));
    }

    @GetMapping
    @Operation(summary = "List all orders", description = "Role: ADMIN. Optional status filter; newest first.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of orders"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Not ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<OrderResponse>> listAll(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size
    ) {
        return ResponseEntity.ok(orderService.listAll(status, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by id", description = "CUSTOMER may read own orders only (other customers' orders return 404). ADMIN may read any.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Order not found or not visible",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<OrderResponse> getById(@AuthenticationPrincipal AuthenticatedUser principal,
                                                 @PathVariable Long id) {
        return ResponseEntity.ok(orderService.getById(principal, id));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update order status", description = "Role: ADMIN. Allowed: PENDING→COMPLETED or PENDING→CANCELLED. Cancel restores stock.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Not ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Invalid status transition",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(orderService.updateStatus(id, request));
    }
}
