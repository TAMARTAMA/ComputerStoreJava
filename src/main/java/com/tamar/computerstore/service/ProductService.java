package com.tamar.computerstore.service;

import com.tamar.computerstore.dto.PageResponse;
import com.tamar.computerstore.dto.ProductCreateRequest;
import com.tamar.computerstore.dto.ProductResponse;
import com.tamar.computerstore.dto.ProductUpdateRequest;
import com.tamar.computerstore.entity.Product;
import com.tamar.computerstore.entity.ProductCategory;
import com.tamar.computerstore.exception.DuplicateSkuException;
import com.tamar.computerstore.exception.InvalidPageRequestException;
import com.tamar.computerstore.exception.ProductInUseException;
import com.tamar.computerstore.exception.ProductNotFoundException;
import com.tamar.computerstore.exception.StaleProductVersionException;
import com.tamar.computerstore.mapper.ProductMapper;
import com.tamar.computerstore.repository.OrderItemRepository;
import com.tamar.computerstore.repository.ProductRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private static final Sort PRODUCT_SORT = Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"));

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository,
                          OrderItemRepository orderItemRepository,
                          ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
        this.productMapper = productMapper;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public PageResponse<ProductResponse> list(ProductCategory category, String q, int page, Integer size) {
        int pageSize = size == null ? DEFAULT_PAGE_SIZE : size;
        if (page < 0) {
            throw new InvalidPageRequestException("page must be greater than or equal to 0");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new InvalidPageRequestException(
                    "size must be between 1 and " + MAX_PAGE_SIZE + " (default " + DEFAULT_PAGE_SIZE + ")"
            );
        }

        String search = normalizeSearch(q);
        PageRequest pageable = PageRequest.of(page, pageSize, PRODUCT_SORT);
        Page<Product> results = productRepository.search(category, search, pageable);
        return PageResponse.from(results, productMapper::toResponse);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ProductResponse getById(Long id) {
        return productMapper.toResponse(requireProduct(id));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse create(ProductCreateRequest request) {
        String sku = request.sku().trim();
        if (productRepository.existsBySku(sku)) {
            throw new DuplicateSkuException(sku);
        }

        Product product = productMapper.toEntity(request);
        try {
            return productMapper.toResponse(productRepository.saveAndFlush(product));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateSkuException(sku);
        }
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse update(Long id, ProductUpdateRequest request) {
        Product product = requireProduct(id);
        long expectedVersion = request.version();
        if (product.getVersion() != expectedVersion) {
            throw new StaleProductVersionException(id, expectedVersion, product.getVersion());
        }

        String sku = request.sku().trim();
        if (productRepository.existsBySkuAndIdNot(sku, id)) {
            throw new DuplicateSkuException(sku);
        }

        productMapper.applyUpdate(product, request);
        try {
            return productMapper.toResponse(productRepository.saveAndFlush(product));
        } catch (OptimisticLockingFailureException exception) {
            throw new StaleProductVersionException(id, expectedVersion, product.getVersion());
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateSkuException(sku);
        }
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long id) {
        Product product = requireProduct(id);
        if (orderItemRepository.existsByProductId(id)) {
            throw new ProductInUseException(id);
        }
        try {
            productRepository.delete(product);
            productRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ProductInUseException(id);
        }
    }

    private Product requireProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    private static String normalizeSearch(String q) {
        if (q == null) {
            return null;
        }
        String trimmed = q.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return escapeLike(trimmed);
    }

    private static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
