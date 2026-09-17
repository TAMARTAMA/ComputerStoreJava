package com.tamar.computerstore.persistence;

import com.tamar.computerstore.entity.Customer;
import com.tamar.computerstore.entity.OrderItem;
import com.tamar.computerstore.entity.OrderStatus;
import com.tamar.computerstore.entity.Product;
import com.tamar.computerstore.entity.ProductCategory;
import com.tamar.computerstore.entity.PurchaseOrder;
import com.tamar.computerstore.entity.UserAccount;
import com.tamar.computerstore.entity.UserRole;
import com.tamar.computerstore.repository.CustomerRepository;
import com.tamar.computerstore.repository.ProductRepository;
import com.tamar.computerstore.repository.PurchaseOrderRepository;
import com.tamar.computerstore.repository.UserAccountRepository;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class PurchaseOrderRepositoryIntegrationTests extends AbstractMySqlIntegrationTests {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Test
    void savesOrderWithItems() {
        Customer customer = givenCustomer("orders@example.com");
        Product product = productRepository.saveAndFlush(
                new Product("SKU-RAM-001", "Memory Module", new BigDecimal("249.50"), 20, ProductCategory.HARDWARE)
        );

        PurchaseOrder order = new PurchaseOrder(customer);
        order.addItem(new OrderItem(product, 2, product.getPrice()));
        order.setTotalAmount(new BigDecimal("499.00"));
        Long orderId = purchaseOrderRepository.saveAndFlush(order).getId();

        Optional<PurchaseOrder> reloaded = purchaseOrderRepository.findById(orderId);

        assertThat(reloaded).isPresent().get().satisfies(found -> {
            assertThat(found.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(found.getTotalAmount()).isEqualByComparingTo("499.00");
            assertThat(found.getCreatedAt()).isNotNull();
            assertThat(found.getCustomer().getId()).isEqualTo(customer.getId());
            assertThat(found.getItems()).singleElement().satisfies(item -> {
                assertThat(item.getQuantity()).isEqualTo(2);
                assertThat(item.getUnitPrice()).isEqualByComparingTo("249.50");
                assertThat(item.getLineTotal()).isEqualByComparingTo("499.00");
            });
        });
    }

    @Test
    void rejectsNonPositiveItemQuantity() {
        Customer customer = givenCustomer("bad-quantity@example.com");
        Product product = productRepository.saveAndFlush(
                new Product("SKU-SSD-001", "Solid State Drive", new BigDecimal("399.00"), 10, ProductCategory.HARDWARE)
        );

        PurchaseOrder order = new PurchaseOrder(customer);
        order.addItem(new OrderItem(product, 0, product.getPrice()));

        assertThatThrownBy(() -> purchaseOrderRepository.saveAndFlush(order))
                .isInstanceOf(ConstraintViolationException.class);
    }

    private Customer givenCustomer(String email) {
        UserAccount account = userAccountRepository.saveAndFlush(
                new UserAccount(email, "not-a-real-hash", UserRole.CUSTOMER)
        );
        return customerRepository.saveAndFlush(new Customer(account, "Order", "Owner"));
    }
}
