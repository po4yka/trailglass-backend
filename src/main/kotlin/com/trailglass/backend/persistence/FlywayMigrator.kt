package com.trailglass.backend.persistence

import org.flywaydb.core.Flyway
import javax.sql.DataSource

object FlywayMigrator {
    fun migrate(dataSource: DataSource, autoMigrate: Boolean): Flyway {
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .cleanDisabled(true)
            .validateMigrationNaming(true)
            .load()

        if (autoMigrate) {
            flyway.migrate()
        } else {
            flyway.validate()
        }

        return flyway
    }
}
