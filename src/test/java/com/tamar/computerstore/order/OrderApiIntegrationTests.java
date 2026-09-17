package com.tamar.computerstore.order;

import com.tamar.computerstore.entity.Product;
import com.tamar.computerstore.entity.ProductCategory;
import com.tamar.computerstore.entity.UserAccount;
import com.tamar.computerstore.entity.UserRole;
import com.tamar.computerstore.persistence.MySqlContainerConfiguration;
import com.tamar.computerstore.repository.CustomerRepository;
import com.tamar.computerstore.repository.ProductRepository;
import com.tamar.computerstore.repository.PurchaseOrderRepository;
import com.tamar.computerstore.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(MySqlContainerConfiguration.class)
class OrderApiIntegrationTests {

    private static final String PASSWORD = "Str0ng-Passw0rd!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String customerToken;
    private String otherCustomerToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        purchaseOrderRepository.deleteAll();
        productRepository.deleteAll();
        customerRepository.deleteAll();
        userAccountRepository.deleteAll();

        customerToken = registerCustomer("buyer@example.com", "Buyer", "One");
        otherCustomerToken = registerCustomer("other@example.com", "Buyer", "Two");
        adminToken = createAdminAndLogin("admin@example.com");
    }

    @Test
    void customerCanPlaceOrderWithServerCalculatedTotalAndDecrementedStock() throws Exception {
        Product product = saveProduct("SKU-ORD-001", "Laptop", "999.50", 10);

        MvcResult result = mockMvc.perform(authorized(post("/api/orders"), customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(product.getId(), 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(1999.00))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].unitPrice").value(999.50))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("totalAmount").decimalValue()).isEqualByComparingTo("1999.00");

        Product reloaded = productRepository.findById(product.getId()).orElseThrow();
        assertThat(reloaded.getStockQuantity()).isEqualTo(8);
        assertThat(purchaseOrderRepository.count()).isEqualTo(1);
    }

    @Test
    void insufficientStockReturnsConflictAndLeavesInventoryUnchanged() throws Exception {
        Product product = saveProduct("SKU-ORD-002", "GPU", "1500.00", 1);

        mockMvc.perform(authorized(post("/api/orders"), customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(product.getId(), 5)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        assertThat(purchaseOrderRepository.count()).isZero();
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(1);
    }

    @Test
    void customerCannotAccessAnotherCustomersOrder() throws Exception {
        Product product = saveProduct("SKU-ORD-003", "Mouse", "50.00", 5);

        MvcResult created = mockMvc.perform(authorized(post("/api/orders"), customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(product.getId(), 1)))
                .andExpect(status().isCreated())
                .andReturn();
        long orderId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(authorized(get("/api/orders/" + orderId), otherCustomerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(authorized(get("/api/orders/" + orderId), customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId));
    }

    @Test
    void adminCanListAndRetrieveAllOrders() throws Exception {
        Product product = saveProduct("SKU-ORD-004", "Keyboard", "120.00", 8);

        mockMvc.perform(authorized(post("/api/orders"), customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(product.getId(), 1)))
                .andExpect(status().isCreated());

        mockMvc.perform(authorized(post("/api/orders"), otherCustomerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(product.getId(), 1)))
                .andExpect(status().isCreated());

        mockMvc.perform(authorized(get("/api/orders"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(authorized(get("/api/orders"), customerToken))
                .andExpect(status().isForbidden());

        long orderId = purchaseOrderRepository.findAll().getFirst().getId();
        mockMvc.perform(authorized(get("/api/orders/" + orderId), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId));
    }

    @Test
    void onlyAdminCanUpdateStatusAndCancellationRestoresStockOnce() throws Exception {
        Product product = saveProduct("SKU-ORD-005", "SSD", "200.00", 10);

        MvcResult created = mockMvc.perform(authorized(post("/api/orders"), customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(product.getId(), 3)))
                .andExpect(status().isCreated())
                .andReturn();
        long orderId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(7);

        mockMvc.perform(authorized(patch("/api/orders/" + orderId + "/status"), customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(authorized(patch("/api/orders/" + orderId + "/status"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(10);

        mockMvc.perform(authorized(patch("/api/orders/" + orderId + "/status"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(10);
    }

    @Test
    void completingPendingOrderDoesNotChangeStockAgain() throws Exception {
        Product product = saveProduct("SKU-ORD-006", "Monitor", "800.00", 4);

        MvcResult created = mockMvc.perform(authorized(post("/api/orders"), customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(product.getId(), 1)))
                .andExpect(status().isCreated())
                .andReturn();
        long orderId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(3);

        mockMvc.perform(authorized(patch("/api/orders/" + orderId + "/status"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(3);
    }

    @Test
    void orderRequestCannotOverrideUnitPriceOrTotal() throws Exception {
        Product product = saveProduct("SKU-ORD-007", "Antivirus", "99.00", 20);

        String forged = """
                {
                  "totalAmount": 1.00,
                  "unitPrice": 1.00,
                  "status": "COMPLETED",
                  "customerId": 999,
                  "items": [{"productId": %d, "quantity": 2, "unitPrice": 1.00, "price": 1.00}]
                }
                """.formatted(product.getId());

        mockMvc.perform(authorized(post("/api/orders"), customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(forged))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(198.00))
                .andExpect(jsonPath("$.items[0].unitPrice").value(99.00));
    }

    @Test
    void customerCanListOwnOrdersNewestFirst() throws Exception {
        Product product = saveProduct("SKU-ORD-008", "RAM", "300.00", 15);

        mockMvc.perform(authorized(post("/api/orders"), customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(product.getId(), 1)))
                .andExpect(status().isCreated());
        Thread.sleep(20);
        MvcResult second = mockMvc.perform(authorized(post("/api/orders"), customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(product.getId(), 1)))
                .andExpect(status().isCreated())
                .andReturn();
        long newestId = objectMapper.readTree(second.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(authorized(get("/api/orders/me"), customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value(newestId));
    }

    @Test
    void duplicateProductIdsInRequestReturnBadRequest() throws Exception {
        Product product = saveProduct("SKU-ORD-009", "PSU", "400.00", 9);
        String body = objectMapper.writeValueAsString(Map.of(
                "items", List.of(
                        Map.of("productId", product.getId(), "quantity", 1),
                        Map.of("productId", product.getId(), "quantity", 2)
                )
        ));

        mockMvc.perform(authorized(post("/api/orders"), customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    private Product saveProduct(String sku, String name, String price, int stock) {
        return productRepository.saveAndFlush(
                new Product(sku, name, new BigDecimal(price), stock, ProductCategory.HARDWARE)
        );
    }

    private String orderBody(long productId, int quantity) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("items", List.of(Map.of("productId", productId, "quantity", quantity)));
        return objectMapper.writeValueAsString(payload);
    }

    private String registerCustomer(String email, String firstName, String lastName) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", PASSWORD,
                "firstName", firstName,
                "lastName", lastName
        ));
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").stringValue();
    }

    private String createAdminAndLogin(String email) throws Exception {
        userAccountRepository.save(new UserAccount(email, passwordEncoder.encode(PASSWORD), UserRole.ADMIN));
        String body = objectMapper.writeValueAsString(Map.of("email", email, "password", PASSWORD));
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").stringValue();
    }

    private static MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder builder,
                                                            String token) {
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
}
