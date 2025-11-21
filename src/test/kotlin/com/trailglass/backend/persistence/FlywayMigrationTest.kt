package com.trailglass.backend.persistence

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FlywayMigrationTest {

    @Container
    private val postgres = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
        withDatabaseName("trailglass")
        withUsername("trailglass")
        withPassword("trailglass")
    }

    @Test
    fun `flyway migrations apply cleanly`() {
        val flyway = Flyway.configure()
            .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
            .locations("classpath:db/migration")
            .cleanDisabled(false)
            .load()

        flyway.clean()
        val result = flyway.migrate()

        assertEquals(0, result.failedMigrations.size, "Migrations should apply without errors")
    }
}
