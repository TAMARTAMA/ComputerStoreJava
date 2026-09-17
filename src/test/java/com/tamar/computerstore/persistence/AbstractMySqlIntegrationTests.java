package com.tamar.computerstore.persistence;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Base class for persistence tests. Every subclass shares one MySQL container and one
 * Spring context, so Flyway migrates the container database exactly once per build.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(MySqlContainerConfiguration.class)
abstract class AbstractMySqlIntegrationTests {
}
