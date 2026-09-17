package com.tamar.computerstore.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 metadata for Swagger UI. The bearer scheme lets clients paste a JWT once
 * (Authorize) and call protected endpoints without repeating the header.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    public static final String BEARER_JWT_SCHEME = "bearer-jwt";

    @Bean
    public OpenAPI computerStoreOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Computer Store Management API")
                        .version("0.0.1")
                        .description("""
                                Stateless Spring Boot API for a computer store catalog and orders. \
                                Authenticate via POST /api/auth/login or /register, then use \
                                Authorize in Swagger UI with the returned access token \
                                (Bearer JWT). Roles: CUSTOMER and ADMIN."""))
                .components(new Components()
                        .addSecuritySchemes(BEARER_JWT_SCHEME, new SecurityScheme()
                                .name(BEARER_JWT_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the accessToken from /api/auth/login or /register.")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_JWT_SCHEME));
    }
}
