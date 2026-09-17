package com.tamar.computerstore.config;

import com.tamar.computerstore.entity.UserAccount;
import com.tamar.computerstore.entity.UserRole;
import com.tamar.computerstore.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Creates the first ADMIN account for local development from LOCAL_ADMIN_EMAIL and
 * LOCAL_ADMIN_PASSWORD. Restricted to the {@code local} profile and skipped entirely when the
 * variables are unset, so no admin credential ever reaches the repository or a Flyway migration.
 * Re-running is safe: an existing account with the same email is left untouched.
 */
@Component
@Profile("local")
public class LocalAdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalAdminBootstrap.class);
    private static final int MINIMUM_PASSWORD_LENGTH = 12;

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public LocalAdminBootstrap(UserAccountRepository userAccountRepository,
                               PasswordEncoder passwordEncoder,
                               @Value("${LOCAL_ADMIN_EMAIL:}") String adminEmail,
                               @Value("${LOCAL_ADMIN_PASSWORD:}") String adminPassword) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.info("Local admin bootstrap skipped: LOCAL_ADMIN_EMAIL and LOCAL_ADMIN_PASSWORD are not both set");
            return;
        }
        if (adminPassword.length() < MINIMUM_PASSWORD_LENGTH) {
            log.warn("Local admin bootstrap skipped: LOCAL_ADMIN_PASSWORD must be at least {} characters",
                    MINIMUM_PASSWORD_LENGTH);
            return;
        }

        String email = adminEmail.trim().toLowerCase(Locale.ROOT);
        if (userAccountRepository.existsByEmail(email)) {
            log.info("Local admin bootstrap skipped: an account already exists for {}", email);
            return;
        }

        userAccountRepository.save(new UserAccount(email, passwordEncoder.encode(adminPassword), UserRole.ADMIN));
        log.info("Local admin bootstrap created ADMIN account for {}", email);
    }
}
