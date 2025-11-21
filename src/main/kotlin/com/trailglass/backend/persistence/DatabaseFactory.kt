package com.trailglass.backend.persistence

import com.trailglass.backend.config.AppConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import kotlin.system.exitProcess
import javax.sql.DataSource

object DatabaseFactory {
    private const val DEFAULT_POOL_SIZE = 5

    fun dataSource(config: AppConfig): HikariDataSource {
        val jdbcUrlValue = config.databaseUrl?.takeIf { it.isNotBlank() }
            ?: run {
                println("[fatal] DATABASE_URL is required for persistence configuration")
                exitProcess(1)
            }

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = jdbcUrlValue
            username = config.databaseUser
            password = config.databasePassword
            maximumPoolSize = DEFAULT_POOL_SIZE
            isAutoCommit = false
            validate()
        }

        return HikariDataSource(hikariConfig)
    }

    fun connect(dataSource: DataSource): Database = Database.connect(dataSource)
}
