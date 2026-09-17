package com.tamar.computerstore.config;

import com.tamar.computerstore.persistence.MySqlContainerConfiguration;
import com.tamar.computerstore.repository.ProductRepository;
import com.tamar.computerstore.repository.PurchaseOrderRepository;
import com.tamar.computerstore.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS;

@SpringBootTest
@Import(MySqlContainerConfiguration.class)
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "APP_DEMO_DATA_ENABLED=true",
        "DEMO_CUSTOMER_PASSWORD=Demo-Only-Passw0rd!",
        "LOCAL_ADMIN_EMAIL=demo.admin@example.test",
        "LOCAL_ADMIN_PASSWORD=Demo-Admin-Passw0rd!"
})
@DirtiesContext(classMode = AFTER_CLASS)
class DemoDataInitializerIdempotencyTest {

    @Autowired
    private DemoDataInitializer demoDataInitializer;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Test
    void seedingIsIdempotentWhenEnabledUnderLocalProfile() {
        long productsAfterStartup = productRepository.count();
        long usersAfterStartup = userAccountRepository.count();
        long ordersAfterStartup = purchaseOrderRepository.count();

        assertThat(productsAfterStartup).isGreaterThanOrEqualTo(15);
        assertThat(userAccountRepository.existsByEmail(DemoDataInitializer.DEMO_CUSTOMER_1_EMAIL)).isTrue();
        assertThat(userAccountRepository.existsByEmail(DemoDataInitializer.DEMO_CUSTOMER_2_EMAIL)).isTrue();
        assertThat(ordersAfterStartup).isGreaterThanOrEqualTo(3);

        demoDataInitializer.run(new DefaultApplicationArguments());

        assertThat(productRepository.count()).isEqualTo(productsAfterStartup);
        assertThat(userAccountRepository.count()).isEqualTo(usersAfterStartup);
        assertThat(purchaseOrderRepository.count()).isEqualTo(ordersAfterStartup);
    }
}
