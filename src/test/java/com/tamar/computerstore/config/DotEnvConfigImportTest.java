package com.tamar.computerstore.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class DotEnvConfigImportTest {

    @Test
    void applicationYmlImportsOptionalDotEnvAsProperties() throws Exception {
        String yaml = Files.readString(Path.of("src/main/resources/application.yml"));
        assertThat(yaml).contains("optional:file:.env[.properties]");
    }

    @Test
    void envStyleFileIsReadableAsSpringProperties(@TempDir Path tempDir) throws Exception {
        Path envFile = tempDir.resolve(".env");
        String secret = "dotenv-file-secret-value-0123456789abcdef";
        Files.writeString(envFile, """
                # comment should be ignored
                JWT_SECRET=%s
                JWT_EXPIRATION=PT30M
                """.formatted(secret));

        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(envFile)) {
            properties.load(reader);
        }

        assertThat(properties.getProperty("JWT_SECRET")).isEqualTo(secret);
        assertThat(properties.getProperty("JWT_EXPIRATION")).isEqualTo("PT30M");
    }

    @Test
    void springConfigImportLoadsJwtSecretFromEnvStyleFile(@TempDir Path tempDir) throws Exception {
        Path envFile = tempDir.resolve("local.env");
        String secret = "imported-dotenv-secret-0123456789abcdef";
        Files.writeString(envFile, "JWT_SECRET=" + secret + "\n");

        String importLocation = "optional:file:" + envFile.toAbsolutePath().toUri().getSchemeSpecificPart() + "[.properties]";

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(DotEnvProbeConfig.class)
                .web(WebApplicationType.NONE)
                .properties(
                        "spring.main.banner-mode=off",
                        "spring.main.web-application-type=none",
                        "spring.autoconfigure.exclude="
                                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
                                + "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,"
                                + "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration",
                        "spring.config.import=" + importLocation,
                        "app.security.jwt.secret=${JWT_SECRET:}",
                        "app.security.jwt.expiration=${JWT_EXPIRATION:PT1H}",
                        "app.security.jwt.issuer=dotenv-probe"
                )
                .run()) {
            ConfigurableEnvironment environment = context.getEnvironment();
            boolean fileContributedSecret = false;
            for (org.springframework.core.env.PropertySource<?> source : environment.getPropertySources()) {
                if (source.getName() != null && source.getName().contains("local.env")) {
                    fileContributedSecret = secret.equals(source.getProperty("JWT_SECRET"));
                }
                if (source instanceof EnumerablePropertySource<?> enumerable) {
                    for (String name : enumerable.getPropertyNames()) {
                        if ("JWT_SECRET".equals(name) && secret.equals(String.valueOf(enumerable.getProperty(name)))) {
                            fileContributedSecret = true;
                        }
                    }
                }
            }

            String effective = environment.getProperty("app.security.jwt.secret");
            assertThat(effective).isNotBlank();
            if (System.getenv("JWT_SECRET") == null || System.getenv("JWT_SECRET").isBlank()) {
                assertThat(effective).isEqualTo(secret);
            }
            assertThat(fileContributedSecret || secret.equals(effective) || secret.equals(environment.getProperty("JWT_SECRET")))
                    .as("imported .env-style file should contribute JWT_SECRET when not overridden")
                    .isTrue();
        }
    }

    @Configuration
    @EnableConfigurationProperties(JwtProperties.class)
    static class DotEnvProbeConfig {
    }
}
