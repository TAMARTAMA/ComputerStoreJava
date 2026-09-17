package com.tamar.computerstore.service;

import com.tamar.computerstore.dto.CreateOrderRequest;
import com.tamar.computerstore.dto.OrderLineRequest;
import com.tamar.computerstore.dto.OrderResponse;
import com.tamar.computerstore.dto.PageResponse;
import com.tamar.computerstore.dto.UpdateOrderStatusRequest;
import com.tamar.computerstore.entity.Customer;
import com.tamar.computerstore.entity.OrderItem;
import com.tamar.computerstore.entity.OrderStatus;
import com.tamar.computerstore.entity.Product;
import com.tamar.computerstore.entity.PurchaseOrder;
import com.tamar.computerstore.entity.UserRole;
import com.tamar.computerstore.exception.InsufficientStockException;
import com.tamar.computerstore.exception.InvalidOrderRequestException;
import com.tamar.computerstore.exception.InvalidOrderStatusTransitionException;
import com.tamar.computerstore.exception.InvalidPageRequestException;
import com.tamar.computerstore.exception.InventoryConflictException;
import com.tamar.computerstore.exception.OrderNotFoundException;
import com.tamar.computerstore.exception.ProductNotFoundException;
import com.tamar.computerstore.mapper.OrderMapper;
import com.tamar.computerstore.repository.CustomerRepository;
import com.tamar.computerstore.repository.ProductRepository;
import com.tamar.computerstore.repository.PurchaseOrderRepository;
import com.tamar.computerstore.security.AuthenticatedUser;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OrderService {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final OrderMapper orderMapper;

    public OrderService(PurchaseOrderRepository purchaseOrderRepository,
                        ProductRepository productRepository,
                        CustomerRepository customerRepository,
                        OrderMapper orderMapper) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.orderMapper = orderMapper;
    }

    @Transactional
    @PreAuthorize("hasRole('CUSTOMER')")
    public OrderResponse placeOrder(AuthenticatedUser principal, CreateOrderRequest request) {
        Customer customer = requireCustomerProfile(principal);
        Map<Long, Integer> quantitiesByProductId = validateAndCollapseLines(request.items());

        List<Product> products = productRepository.findAllById(quantitiesByProductId.keySet());
        if (products.size() != quantitiesByProductId.size()) {
            Set<Long> foundIds = new HashSet<>();
            products.forEach(product -> foundIds.add(product.getId()));
            Long missingId = quantitiesByProductId.keySet().stream()
                    .filter(id -> !foundIds.contains(id))
                    .findFirst()
                    .orElseThrow();
            throw new ProductNotFoundException(missingId);
        }

        PurchaseOrder order = new PurchaseOrder(customer);
        BigDecimal total = BigDecimal.ZERO;

        for (Product product : products) {
            int quantity = quantitiesByProductId.get(product.getId());
            if (product.getStockQuantity() < quantity) {
                throw new InsufficientStockException(product.getId(), quantity, product.getStockQuantity());
            }

            BigDecimal unitPrice = product.getPrice();
            order.addItem(new OrderItem(product, quantity, unitPrice));
            total = total.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));
            product.setStockQuantity(product.getStockQuantity() - quantity);
        }

        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(total);

        try {
            productRepository.saveAll(products);
            productRepository.flush();
            PurchaseOrder saved = purchaseOrderRepository.saveAndFlush(order);
            return orderMapper.toResponse(requireDetailedOrder(saved.getId()));
        } catch (OptimisticLockingFailureException exception) {
            throw new InventoryConflictException();
        }
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('CUSTOMER')")
    public PageResponse<OrderResponse> listMyOrders(AuthenticatedUser principal, int page, Integer size) {
        Customer customer = requireCustomerProfile(principal);
        PageRequest pageable = pageRequest(page, size);
        Page<PurchaseOrder> results = purchaseOrderRepository.findByCustomerId(customer.getId(), pageable);
        return PageResponse.from(results, orderMapper::toResponse);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public OrderResponse getById(AuthenticatedUser principal, Long id) {
        PurchaseOrder order = requireDetailedOrder(id);

        if (principal.getRole() == UserRole.ADMIN) {
            return orderMapper.toResponse(order);
        }

        Customer customer = requireCustomerProfile(principal);
        if (!order.getCustomer().getId().equals(customer.getId())) {
            // Hide existence of other customers' orders.
            throw new OrderNotFoundException(id);
        }
        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<OrderResponse> listAll(OrderStatus status, int page, Integer size) {
        PageRequest pageable = pageRequest(page, size);
        Page<PurchaseOrder> results = status == null
                ? purchaseOrderRepository.findAll(pageable)
                : purchaseOrderRepository.findByStatus(status, pageable);
        return PageResponse.from(results, orderMapper::toResponse);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public OrderResponse updateStatus(Long id, UpdateOrderStatusRequest request) {
        PurchaseOrder order = requireDetailedOrder(id);
        OrderStatus current = order.getStatus();
        OrderStatus target = request.status();

        if (current == target) {
            throw new InvalidOrderStatusTransitionException(current, target);
        }
        if (current != OrderStatus.PENDING
                || (target != OrderStatus.COMPLETED && target != OrderStatus.CANCELLED)) {
            throw new InvalidOrderStatusTransitionException(current, target);
        }

        if (target == OrderStatus.CANCELLED) {
            restoreStock(order);
        }

        order.setStatus(target);

        try {
            purchaseOrderRepository.saveAndFlush(order);
            return orderMapper.toResponse(requireDetailedOrder(order.getId()));
        } catch (OptimisticLockingFailureException exception) {
            throw new InventoryConflictException();
        }
    }

    private void restoreStock(PurchaseOrder order) {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
        }
        try {
            productRepository.saveAll(order.getItems().stream().map(OrderItem::getProduct).toList());
            productRepository.flush();
        } catch (OptimisticLockingFailureException exception) {
            throw new InventoryConflictException();
        }
    }

    private Customer requireCustomerProfile(AuthenticatedUser principal) {
        return customerRepository.findByUserAccountId(principal.getId())
                .orElseThrow(() -> new InvalidOrderRequestException(
                        "Authenticated user does not have a customer profile"
                ));
    }

    private PurchaseOrder requireDetailedOrder(Long id) {
        return purchaseOrderRepository.findDetailedById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    private static Map<Long, Integer> validateAndCollapseLines(List<OrderLineRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new InvalidOrderRequestException("Order must contain at least one item");
        }

        Map<Long, Integer> quantities = new LinkedHashMap<>();
        Set<Long> seen = new HashSet<>();
        for (OrderLineRequest line : items) {
            if (line.productId() == null) {
                throw new InvalidOrderRequestException("productId is required");
            }
            if (line.quantity() <= 0) {
                throw new InvalidOrderRequestException("quantity must be positive");
            }
            if (!seen.add(line.productId())) {
                throw new InvalidOrderRequestException(
                        "Duplicate productId in order request: " + line.productId()
                );
            }
            quantities.put(line.productId(), line.quantity());
        }
        return quantities;
    }

    private static PageRequest pageRequest(int page, Integer size) {
        int pageSize = size == null ? DEFAULT_PAGE_SIZE : size;
        if (page < 0) {
            throw new InvalidPageRequestException("page must be greater than or equal to 0");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new InvalidPageRequestException(
                    "size must be between 1 and " + MAX_PAGE_SIZE + " (default " + DEFAULT_PAGE_SIZE + ")"
            );
        }
        return PageRequest.of(page, pageSize, NEWEST_FIRST);
    }
}
