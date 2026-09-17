package com.tamar.computerstore.controller;

import com.tamar.computerstore.config.SecurityConfig;
import com.tamar.computerstore.dto.HealthResponse;
import com.tamar.computerstore.security.JwtService;
import com.tamar.computerstore.service.HealthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The real security configuration is imported so the slice reflects the deployed rules rather
// than Boot's default "authenticate everything" chain. The JWT collaborators are mocked because
// this test never sends an Authorization header.
@WebMvcTest(HealthController.class)
@Import(SecurityConfig.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthService healthService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void healthReturnsUpStatus() throws Exception {
        when(healthService.getHealth()).thenReturn(
                new HealthResponse("UP", "Computer Store Management API", Instant.parse("2026-01-01T00:00:00Z"))
        );

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("Computer Store Management API"));
    }
}
