package com.mentormatching.booking;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "EUREKA_ENABLED=false")
@Testcontainers(disabledWithoutDocker = true)
class BookingMssqlSchemaTest {
    @Container
    static final MSSQLServerContainer<?> SQL_SERVER = new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest")
            .acceptLicense()
            .withPassword("Mssql-Test!Passw0rd");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", SQL_SERVER::getJdbcUrl);
        properties.add("spring.datasource.username", SQL_SERVER::getUsername);
        properties.add("spring.datasource.password", SQL_SERVER::getPassword);
    }

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void flywayCreatesBookingAndAvailabilityTablesOnSqlServer() {
        assertThat(tableExists("bookings")).isTrue();
        assertThat(tableExists("mentor_availabilities")).isTrue();
        assertThat(columnExists("bookings", "hold_expires_at")).isTrue();
    }

    private boolean tableExists(String name) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'dbo' AND TABLE_NAME = ?",
                Integer.class, name);
        return count != null && count == 1;
    }

    private boolean columnExists(String table, String column) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'dbo' AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class, table, column);
        return count != null && count == 1;
    }
}
