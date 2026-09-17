package com.tamar.computerstore.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FlywaySchemaIntegrationTests extends AbstractMySqlIntegrationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayAppliesBaselineAndCoreSchemaMigrations() {
        List<String> appliedVersions = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success = TRUE ORDER BY installed_rank",
                String.class
        );

        assertThat(appliedVersions).contains("1", "2");
    }

    @Test
    void coreTablesExist() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()",
                String.class
        );

        assertThat(tables).contains("user_accounts", "customers", "products", "purchase_orders", "order_items");
    }

    @Test
    void uniqueConstraintsAndForeignKeysAreInPlace() {
        List<String> constraints = jdbcTemplate.queryForList(
                "SELECT constraint_name FROM information_schema.table_constraints WHERE constraint_schema = DATABASE()",
                String.class
        );

        assertThat(constraints).contains(
                "uk_user_accounts_email",
                "uk_products_sku",
                "uk_customers_user_account",
                "fk_customers_user_account",
                "fk_purchase_orders_customer",
                "fk_order_items_purchase_order",
                "fk_order_items_product"
        );
    }

    @Test
    void monetaryColumnsUseDecimal() {
        List<String> monetaryColumnTypes = jdbcTemplate.queryForList(
                """
                SELECT data_type FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND (table_name, column_name) IN (('products', 'price'),
                                                    ('purchase_orders', 'total_amount'),
                                                    ('order_items', 'unit_price'))
                """,
                String.class
        );

        assertThat(monetaryColumnTypes).hasSize(3).containsOnly("decimal");
    }
}
