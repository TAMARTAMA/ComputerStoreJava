package com.tamar.computerstore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * BCrypt is the only password encoder in this application. It lives outside
 * {@link SecurityConfig} so that non-web contexts (persistence tests) can encode passwords
 * without bootstrapping the servlet security filter chain.
 */
@Configuration(proxyBeanMethods = false)
public class PasswordEncoderConfig {

    private static final int BCRYPT_STRENGTH = 12;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCryptPasswordEncoder.BCryptVersion.$2B, BCRYPT_STRENGTH);
    }
}
