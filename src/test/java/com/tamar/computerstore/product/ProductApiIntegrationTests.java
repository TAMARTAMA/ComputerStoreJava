package com.tamar.computerstore.product;

import com.tamar.computerstore.entity.Customer;
import com.tamar.computerstore.entity.OrderItem;
import com.tamar.computerstore.entity.Product;
import com.tamar.computerstore.entity.ProductCategory;
import com.tamar.computerstore.entity.PurchaseOrder;
import com.tamar.computerstore.entity.UserAccount;
import com.tamar.computerstore.entity.UserRole;
import com.tamar.computerstore.persistence.MySqlContainerConfiguration;
import com.tamar.computerstore.repository.CustomerRepository;
import com.tamar.computerstore.repository.ProductRepository;
import com.tamar.computerstore.repository.PurchaseOrderRepository;
import com.tamar.computerstore.repository.UserAccountRepository;
import com.tamar.computerstore.service.ProductService;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(MySqlContainerConfiguration.class)
class ProductApiIntegrationTests {

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
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        purchaseOrderRepository.deleteAll();
        productRepository.deleteAll();
        customerRepository.deleteAll();
        userAccountRepository.deleteAll();

        customerToken = registerCustomer("customer@example.com");
        adminToken = createAdminAndLogin("admin@example.com");
    }

    @Test
    void unauthenticatedProductAccessReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void customerCanReadProductsButCannotMutateThem() throws Exception {
        Long productId = createProductAsAdmin("SKU-READ-001", "Laptop", ProductCategory.HARDWARE);

        mockMvc.perform(authorized(get("/api/products"), customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value("SKU-READ-001"));

        mockMvc.perform(authorized(get("/api/products/" + productId), customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop"));

        mockMvc.perform(authorized(post("/api/products"), customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("SKU-BLOCKED", "Blocked", ProductCategory.SOFTWARE)))
                .andExpect(status().isForbidden());

        mockMvc.perform(authorized(put("/api/products/" + productId), customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("SKU-READ-001", "Laptop", ProductCategory.HARDWARE, 0L)))
                .andExpect(status().isForbidden());

        mockMvc.perform(authorized(delete("/api/products/" + productId), customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateUpdateAndDeleteProducts() throws Exception {
        MvcResult created = mockMvc.perform(authorized(post("/api/products"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("SKU-ADMIN-001", "Monitor", ProductCategory.HARDWARE)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("SKU-ADMIN-001"))
                .andExpect(jsonPath("$.version").value(0))
                .andReturn();

        JsonNode product = objectMapper.readTree(created.getResponse().getContentAsString());
        long id = product.get("id").asLong();
        long version = product.get("version").asLong();

        mockMvc.perform(authorized(put("/api/products/" + id), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("SKU-ADMIN-001", "4K Monitor", ProductCategory.HARDWARE, version)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("4K Monitor"))
                .andExpect(jsonPath("$.version").value(version + 1));

        mockMvc.perform(authorized(delete("/api/products/" + id), adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(authorized(get("/api/products/" + id), adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void createWithInvalidPayloadReturnsBadRequest() throws Exception {
        String body = """
                {"sku":"","name":"","price":-1,"stockQuantity":-5,"category":null}
                """;

        mockMvc.perform(authorized(post("/api/products"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.sku").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.name").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.price").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.stockQuantity").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.category").isNotEmpty());
    }

    @Test
    void missingProductReturnsNotFound() throws Exception {
        mockMvc.perform(authorized(get("/api/products/999999"), adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void duplicateSkuReturnsConflict() throws Exception {
        createProductAsAdmin("SKU-DUP-001", "Keyboard", ProductCategory.HARDWARE);

        mockMvc.perform(authorized(post("/api/products"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("SKU-DUP-001", "Other Keyboard", ProductCategory.HARDWARE)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void staleVersionUpdateReturnsConflict() throws Exception {
        Long id = createProductAsAdmin("SKU-LOCK-001", "Mouse", ProductCategory.HARDWARE);

        mockMvc.perform(authorized(put("/api/products/" + id), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("SKU-LOCK-001", "Wireless Mouse", ProductCategory.HARDWARE, 99L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void searchFilterAndPaginationAreDeterministic() throws Exception {
        createProductAsAdmin("SKU-HW-B", "Beta Board", ProductCategory.HARDWARE);
        createProductAsAdmin("SKU-HW-A", "Alpha Board", ProductCategory.HARDWARE);
        createProductAsAdmin("SKU-SW-1", "Office Suite", ProductCategory.SOFTWARE);

        mockMvc.perform(authorized(get("/api/products"), adminToken)
                        .param("category", "HARDWARE")
                        .param("q", "board")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].sku").value("SKU-HW-A"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.page").value(0));

        mockMvc.perform(authorized(get("/api/products"), adminToken)
                        .param("category", "HARDWARE")
                        .param("q", "board")
                        .param("page", "1")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value("SKU-HW-B"))
                .andExpect(jsonPath("$.last").value(true));

        mockMvc.perform(authorized(get("/api/products"), adminToken)
                        .param("size", String.valueOf(ProductService.MAX_PAGE_SIZE + 1)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteReturnsConflictWhenProductIsReferencedByAnOrder() throws Exception {
        Long productId = createProductAsAdmin("SKU-USED-001", "SSD", ProductCategory.HARDWARE);
        Product product = productRepository.findById(productId).orElseThrow();

        UserAccount customerAccount = userAccountRepository.findByEmail("customer@example.com").orElseThrow();
        Customer customer = customerRepository.findByUserAccountId(customerAccount.getId()).orElseThrow();
        PurchaseOrder order = new PurchaseOrder(customer);
        order.addItem(new OrderItem(product, 1, product.getPrice()));
        order.setTotalAmount(product.getPrice());
        purchaseOrderRepository.saveAndFlush(order);

        mockMvc.perform(authorized(delete("/api/products/" + productId), adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        assertThat(productRepository.existsById(productId)).isTrue();
    }

    private Long createProductAsAdmin(String sku, String name, ProductCategory category) throws Exception {
        MvcResult result = mockMvc.perform(authorized(post("/api/products"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(sku, name, category)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String registerCustomer(String email) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", PASSWORD,
                "firstName", "Noa",
                "lastName", "Levi"
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

    private String createBody(String sku, String name, ProductCategory category) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sku", sku);
        payload.put("name", name);
        payload.put("description", "Catalog item");
        payload.put("price", new BigDecimal("199.99"));
        payload.put("stockQuantity", 5);
        payload.put("category", category.name());
        return objectMapper.writeValueAsString(payload);
    }

    private String updateBody(String sku, String name, ProductCategory category, long version) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sku", sku);
        payload.put("name", name);
        payload.put("description", "Updated catalog item");
        payload.put("price", new BigDecimal("249.99"));
        payload.put("stockQuantity", 8);
        payload.put("category", category.name());
        payload.put("version", version);
        return objectMapper.writeValueAsString(payload);
    }
}
