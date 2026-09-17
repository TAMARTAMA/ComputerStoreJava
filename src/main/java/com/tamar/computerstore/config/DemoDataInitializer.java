package com.tamar.computerstore.config;

import com.tamar.computerstore.dto.CreateOrderRequest;
import com.tamar.computerstore.dto.OrderLineRequest;
import com.tamar.computerstore.dto.OrderResponse;
import com.tamar.computerstore.dto.RegisterRequest;
import com.tamar.computerstore.dto.UpdateOrderStatusRequest;
import com.tamar.computerstore.entity.OrderStatus;
import com.tamar.computerstore.entity.Product;
import com.tamar.computerstore.entity.ProductCategory;
import com.tamar.computerstore.entity.UserAccount;
import com.tamar.computerstore.entity.UserRole;
import com.tamar.computerstore.repository.CustomerRepository;
import com.tamar.computerstore.repository.ProductRepository;
import com.tamar.computerstore.repository.PurchaseOrderRepository;
import com.tamar.computerstore.repository.UserAccountRepository;
import com.tamar.computerstore.security.AuthenticatedUser;
import com.tamar.computerstore.service.AuthService;
import com.tamar.computerstore.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/**
 * Seeds fictional local-only catalog, customer, and order data. Restricted to the {@code local}
 * profile and {@code APP_DEMO_DATA_ENABLED=true}. Never runs in tests, CI, or production.
 * All names, emails, and phone numbers are reserved demo fiction — not real people.
 */
@Component
@Profile("local")
@ConditionalOnProperty(name = "APP_DEMO_DATA_ENABLED", havingValue = "true")
@Order(200)
public class DemoDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);
    private static final int MINIMUM_PASSWORD_LENGTH = 12;

    static final String DEMO_CUSTOMER_1_EMAIL = "noa.levi@example.test";
    static final String DEMO_CUSTOMER_2_EMAIL = "yonatan.cohen@example.test";

    private final ProductRepository productRepository;
    private final UserAccountRepository userAccountRepository;
    private final CustomerRepository customerRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final AuthService authService;
    private final OrderService orderService;
    private final String demoCustomerPassword;

    public DemoDataInitializer(ProductRepository productRepository,
                               UserAccountRepository userAccountRepository,
                               CustomerRepository customerRepository,
                               PurchaseOrderRepository purchaseOrderRepository,
                               AuthService authService,
                               OrderService orderService,
                               @Value("${DEMO_CUSTOMER_PASSWORD:}") String demoCustomerPassword) {
        this.productRepository = productRepository;
        this.userAccountRepository = userAccountRepository;
        this.customerRepository = customerRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.authService = authService;
        this.orderService = orderService;
        this.demoCustomerPassword = demoCustomerPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (demoCustomerPassword == null || demoCustomerPassword.isBlank()) {
            log.warn("Demo data skipped: DEMO_CUSTOMER_PASSWORD is not set");
            return;
        }
        if (demoCustomerPassword.length() < MINIMUM_PASSWORD_LENGTH) {
            log.warn("Demo data skipped: DEMO_CUSTOMER_PASSWORD must be at least {} characters",
                    MINIMUM_PASSWORD_LENGTH);
            return;
        }

        log.info("Seeding fictional local demo data (idempotent)");
        seedProducts();
        seedCustomers();
        seedOrders();
        log.info("Fictional local demo data seed finished");
    }

    private void seedProducts() {
        for (DemoProduct seed : DEMO_PRODUCTS) {
            if (productRepository.existsBySku(seed.sku())) {
                continue;
            }
            Product product = new Product(
                    seed.sku(),
                    seed.name(),
                    seed.price(),
                    seed.stockQuantity(),
                    seed.category()
            );
            product.setDescription(seed.description());
            productRepository.save(product);
        }
        productRepository.flush();
    }

    private void seedCustomers() {
        ensureCustomer(
                DEMO_CUSTOMER_1_EMAIL,
                "Noa",
                "Levi",
                "+972-50-555-0101",
                "12 Demo Street, Tel Aviv"
        );
        ensureCustomer(
                DEMO_CUSTOMER_2_EMAIL,
                "Yonatan",
                "Cohen",
                "+972-50-555-0102",
                "34 Sample Avenue, Haifa"
        );
    }

    private void ensureCustomer(String email, String firstName, String lastName, String phone, String address) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (userAccountRepository.existsByEmail(normalized)) {
            return;
        }
        authService.register(new RegisterRequest(
                normalized,
                demoCustomerPassword,
                firstName,
                lastName,
                phone,
                address
        ));
    }

    private void seedOrders() {
        UserAccount customer1 = userAccountRepository.findByEmail(DEMO_CUSTOMER_1_EMAIL).orElseThrow();
        var customer1Profile = customerRepository.findByUserAccountId(customer1.getId()).orElseThrow();
        if (purchaseOrderRepository.existsByCustomerId(customer1Profile.getId())) {
            log.info("Demo orders skipped: fictional customer {} already has orders", DEMO_CUSTOMER_1_EMAIL);
            return;
        }

        Product laptop = requireSku("DEMO-LAPTOP-014");
        Product monitor = requireSku("DEMO-MONITOR-027");
        Product ssd = requireSku("DEMO-SSD-512");
        Product keyboard = requireSku("DEMO-KB-MECH");
        Product mouse = requireSku("DEMO-MOUSE-WL");
        Product gpu = requireSku("DEMO-GPU-4070");
        Product os = requireSku("DEMO-OS-WIN11");
        Product antivirus = requireSku("DEMO-SEC-AV");

        AuthenticatedUser customerPrincipal = new AuthenticatedUser(customer1);
        UserAccount adminAccount = userAccountRepository.findAll().stream()
                .filter(account -> account.getRole() == UserRole.ADMIN)
                .findFirst()
                .orElse(null);

        OrderResponse completed = asPrincipal(customerPrincipal, () -> orderService.placeOrder(
                customerPrincipal,
                new CreateOrderRequest(List.of(
                        new OrderLineRequest(laptop.getId(), 1),
                        new OrderLineRequest(os.getId(), 1)
                ))
        ));

        OrderResponse pending = asPrincipal(customerPrincipal, () -> orderService.placeOrder(
                customerPrincipal,
                new CreateOrderRequest(List.of(
                        new OrderLineRequest(monitor.getId(), 1),
                        new OrderLineRequest(keyboard.getId(), 1),
                        new OrderLineRequest(mouse.getId(), 1)
                ))
        ));

        OrderResponse cancelled = asPrincipal(customerPrincipal, () -> orderService.placeOrder(
                customerPrincipal,
                new CreateOrderRequest(List.of(
                        new OrderLineRequest(ssd.getId(), 2),
                        new OrderLineRequest(antivirus.getId(), 1)
                ))
        ));

        // Extra realistic history for the second fictional customer.
        UserAccount customer2 = userAccountRepository.findByEmail(DEMO_CUSTOMER_2_EMAIL).orElseThrow();
        AuthenticatedUser customer2Principal = new AuthenticatedUser(customer2);
        asPrincipal(customer2Principal, () -> orderService.placeOrder(
                customer2Principal,
                new CreateOrderRequest(List.of(
                        new OrderLineRequest(gpu.getId(), 1),
                        new OrderLineRequest(ssd.getId(), 1)
                ))
        ));

        if (adminAccount == null) {
            log.warn("Demo order status transitions skipped: no ADMIN account found "
                    + "(set LOCAL_ADMIN_EMAIL/PASSWORD under the local profile)");
            return;
        }

        AuthenticatedUser adminPrincipal = new AuthenticatedUser(adminAccount);
        asPrincipal(adminPrincipal, () -> {
            orderService.updateStatus(completed.id(), new UpdateOrderStatusRequest(OrderStatus.COMPLETED));
            orderService.updateStatus(cancelled.id(), new UpdateOrderStatusRequest(OrderStatus.CANCELLED));
            return null;
        });

        log.info("Demo orders ready: completed={}, pending={}, cancelled={}",
                completed.id(), pending.id(), cancelled.id());
    }

    private Product requireSku(String sku) {
        return productRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalStateException("Expected demo product missing: " + sku));
    }

    private static <T> T asPrincipal(AuthenticatedUser principal, java.util.function.Supplier<T> action) {
        SecurityContext previous = SecurityContextHolder.getContext();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        ));
        SecurityContextHolder.setContext(context);
        try {
            return action.get();
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }

    private record DemoProduct(
            String sku,
            String name,
            String description,
            BigDecimal price,
            int stockQuantity,
            ProductCategory category
    ) {
    }

    private static final List<DemoProduct> DEMO_PRODUCTS = List.of(
            new DemoProduct("DEMO-LAPTOP-014", "Aurora 14 Ultrabook",
                    "14-inch lightweight laptop with 16GB RAM and 512GB NVMe storage.",
                    new BigDecimal("4299.00"), 12, ProductCategory.HARDWARE),
            new DemoProduct("DEMO-LAPTOP-16P", "Nova 16 Pro Workstation",
                    "16-inch creator laptop with dedicated graphics and 32GB RAM.",
                    new BigDecimal("7899.00"), 6, ProductCategory.HARDWARE),
            new DemoProduct("DEMO-MONITOR-027", "Clarity 27 IPS Display",
                    "27-inch 1440p IPS monitor with 165Hz refresh rate.",
                    new BigDecimal("1299.00"), 18, ProductCategory.HARDWARE),
            new DemoProduct("DEMO-MONITOR-034", "Vista 34 Ultrawide",
                    "34-inch ultrawide curved monitor for productivity layouts.",
                    new BigDecimal("2499.00"), 8, ProductCategory.HARDWARE),
            new DemoProduct("DEMO-SSD-512", "FlashCore 512GB NVMe",
                    "PCIe 4.0 NVMe SSD with DRAM cache for fast boot and load times.",
                    new BigDecimal("289.00"), 40, ProductCategory.HARDWARE),
            new DemoProduct("DEMO-SSD-1TB", "FlashCore 1TB NVMe",
                    "1TB PCIe 4.0 SSD suited for game libraries and media projects.",
                    new BigDecimal("449.00"), 28, ProductCategory.HARDWARE),
            new DemoProduct("DEMO-KB-MECH", "ClickForge Mechanical Keyboard",
                    "Hot-swappable mechanical keyboard with RGB backlight.",
                    new BigDecimal("399.00"), 25, ProductCategory.HARDWARE),
            new DemoProduct("DEMO-KB-COMP", "QuietType Compact Keyboard",
                    "Low-profile compact keyboard for travel and small desks.",
                    new BigDecimal("249.00"), 30, ProductCategory.HARDWARE),
            new DemoProduct("DEMO-MOUSE-WL", "Orbit Wireless Mouse",
                    "Ergonomic wireless mouse with multi-device pairing.",
                    new BigDecimal("179.00"), 35, ProductCategory.HARDWARE),
            new DemoProduct("DEMO-MOUSE-GM", "Pulse Gaming Mouse",
                    "Lightweight gaming mouse with adjustable DPI and PTFE feet.",
                    new BigDecimal("229.00"), 22, ProductCategory.HARDWARE),
            new DemoProduct("DEMO-GPU-4070", "Spectra RTX 4070 12GB",
                    "Mid-high graphics card for 1440p gaming and creative apps.",
                    new BigDecimal("3299.00"), 7, ProductCategory.HARDWARE),
            new DemoProduct("DEMO-GPU-4060", "Spectra RTX 4060 8GB",
                    "Efficient graphics card for 1080p/1440p gaming builds.",
                    new BigDecimal("1899.00"), 10, ProductCategory.HARDWARE),
            new DemoProduct("DEMO-OS-WIN11", "Windows 11 Pro OEM",
                    "OEM license for Windows 11 Pro on a single workstation.",
                    new BigDecimal("699.00"), 50, ProductCategory.SOFTWARE),
            new DemoProduct("DEMO-OS-OFFICE", "Office Suite Home & Business",
                    "Word, Excel, PowerPoint, and Outlook for one PC.",
                    new BigDecimal("899.00"), 45, ProductCategory.SOFTWARE),
            new DemoProduct("DEMO-SEC-AV", "ShieldPro Antivirus 1-Year",
                    "Endpoint antivirus with ransomware protection for one device.",
                    new BigDecimal("149.00"), 60, ProductCategory.SOFTWARE),
            new DemoProduct("DEMO-SEC-VPN", "ShieldPro VPN 1-Year",
                    "Encrypted VPN subscription with multi-platform apps.",
                    new BigDecimal("199.00"), 55, ProductCategory.SOFTWARE),
            new DemoProduct("DEMO-RAM-32", "Velocity 32GB DDR5 Kit",
                    "2x16GB DDR5-5600 memory kit for modern motherboards.",
                    new BigDecimal("549.00"), 20, ProductCategory.HARDWARE),
            new DemoProduct("DEMO-PSU-750", "PowerStable 750W Gold",
                    "750W 80 Plus Gold modular power supply.",
                    new BigDecimal("479.00"), 15, ProductCategory.HARDWARE)
    );
}
