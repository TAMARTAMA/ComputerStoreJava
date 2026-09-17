package com.tamar.computerstore.auth;

import com.tamar.computerstore.entity.UserAccount;
import com.tamar.computerstore.entity.UserRole;
import com.tamar.computerstore.persistence.MySqlContainerConfiguration;
import com.tamar.computerstore.repository.CustomerRepository;
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
import org.springframework.test.web.servlet.RequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(MySqlContainerConfiguration.class)
class AuthIntegrationTests {

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
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void resetAccounts() {
        customerRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    void registerCreatesCustomerAccountAndStoresOnlyABcryptHash() throws Exception {
        mockMvc.perform(register("noa@example.com"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.email").value("noa@example.com"))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.user.firstName").value("Noa"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());

        Optional<UserAccount> stored = userAccountRepository.findByEmail("noa@example.com");
        assertThat(stored).isPresent().get().satisfies(account -> {
            assertThat(account.getPasswordHash()).isNotEqualTo(PASSWORD);
            assertThat(account.getPasswordHash()).startsWith("$2b$12$");
            assertThat(passwordEncoder.matches(PASSWORD, account.getPasswordHash())).isTrue();
            assertThat(account.getRole()).isEqualTo(UserRole.CUSTOMER);
        });
        assertThat(customerRepository.findByUserAccountEmail("noa@example.com")).isPresent();
    }

    @Test
    void registerWithAnAlreadyUsedEmailReturnsConflict() throws Exception {
        mockMvc.perform(register("taken@example.com")).andExpect(status().isCreated());

        mockMvc.perform(register("taken@example.com"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        assertThat(userAccountRepository.count()).isEqualTo(1);
    }

    @Test
    void registerWithAnInvalidPayloadReturnsBadRequestWithFieldErrors() throws Exception {
        String body = """
                {"email":"not-an-email","password":"short","firstName":"","lastName":"Levi"}
                """;

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.email").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.password").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.firstName").isNotEmpty());

        assertThat(userAccountRepository.count()).isZero();
    }

    @Test
    void loginWithValidCredentialsReturnsASignedJwt() throws Exception {
        mockMvc.perform(register("dana@example.com")).andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(login("dana@example.com", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("dana@example.com"))
                .andReturn();

        String token = readToken(result);
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void loginFailuresAreIndistinguishableForWrongPasswordAndUnknownEmail() throws Exception {
        mockMvc.perform(register("known@example.com")).andExpect(status().isCreated());

        String wrongPassword = bodyOf(mockMvc.perform(login("known@example.com", "Wr0ng-Passw0rd!"))
                .andExpect(status().isUnauthorized())
                .andReturn());
        String unknownEmail = bodyOf(mockMvc.perform(login("nobody@example.com", PASSWORD))
                .andExpect(status().isUnauthorized())
                .andReturn());

        JsonNode first = objectMapper.readTree(wrongPassword);
        JsonNode second = objectMapper.readTree(unknownEmail);
        assertThat(first.get("message").stringValue()).isEqualTo("Invalid email or password");
        assertThat(second.get("message").stringValue()).isEqualTo(first.get("message").stringValue());
        assertThat(second.get("status").asInt()).isEqualTo(first.get("status").asInt());
    }

    @Test
    void currentUserRequiresATokenAndReturnsJsonNotALoginPage() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/auth/me"));
    }

    @Test
    void currentUserRejectsATamperedToken() throws Exception {
        mockMvc.perform(register("mallory@example.com")).andExpect(status().isCreated());
        String token = readToken(mockMvc.perform(login("mallory@example.com", PASSWORD)).andReturn());

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token + "tampered"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void currentUserReturnsTheAuthenticatedAccountForAValidToken() throws Exception {
        mockMvc.perform(register("yael@example.com")).andExpect(status().isCreated());
        String token = readToken(mockMvc.perform(login("yael@example.com", PASSWORD)).andReturn());

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("yael@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.firstName").value("Noa"))
                .andExpect(jsonPath("$.phone").value("050-1234567"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    /**
     * There is no role-protected endpoint yet, so this checks the role that reaches the
     * SecurityContext through the token rather than an authorisation decision.
     */
    @Test
    void adminAccountAuthenticatesAsAdminAndHasNoCustomerProfile() throws Exception {
        userAccountRepository.save(new UserAccount(
                "admin@example.com", passwordEncoder.encode(PASSWORD), UserRole.ADMIN
        ));

        String token = readToken(mockMvc.perform(login("admin@example.com", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andReturn());

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.firstName").doesNotExist())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void healthStaysPublicWithSecurityEnabled() throws Exception {
        mockMvc.perform(get("/api/health")).andExpect(status().isOk());
    }

    private RequestBuilder register(String email) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", PASSWORD,
                "firstName", "Noa",
                "lastName", "Levi",
                "phone", "050-1234567",
                "address", "12 Herzl St, Tel Aviv"
        ));
        return post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private RequestBuilder login(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", email, "password", password));
        return post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private String readToken(MvcResult result) throws Exception {
        return objectMapper.readTree(bodyOf(result)).get("accessToken").stringValue();
    }

    private static String bodyOf(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString();
    }
}
