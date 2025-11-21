package com.trailglass.backend.persistence

import org.flywaydb.core.Flyway
import javax.sql.DataSource

object FlywayMigrator {
    fun migrate(dataSource: DataSource): Flyway {
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .cleanDisabled(true)
            .load()

        flyway.migrate()
        return flyway
    }
}
