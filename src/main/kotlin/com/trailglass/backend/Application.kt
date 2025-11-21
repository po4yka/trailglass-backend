package com.trailglass.backend

import com.trailglass.backend.config.ConfigLoader
import com.trailglass.backend.di.appModule
import com.trailglass.backend.persistence.DatabaseFactory
import com.trailglass.backend.persistence.FlywayMigrator
import com.trailglass.backend.plugins.configureHeaderValidation
import com.trailglass.backend.plugins.configureRouting
import com.trailglass.backend.plugins.configureSerialization
import com.trailglass.backend.plugins.configureServerFeatures
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import kotlin.time.Duration.Companion.seconds

private val startTimestamp = System.currentTimeMillis()

fun main() {
    val config = ConfigLoader.fromEnv()
    val dataSource = DatabaseFactory.dataSource(config)
    val flyway = FlywayMigrator.migrate(dataSource, config.autoMigrate)

    embeddedServer(Netty, port = config.port, host = config.host, module = Application::module) {
        connector {
            retryAttempts = 3
            requestQueueLimit = 16
            runningLimit = 100
            connectionGroupSize = 4
        }
        install(Koin) {
            slf4jLogger()
            modules(appModule(config, dataSource, flyway))
        }
        shutdownGracePeriod = 10.seconds
        shutdownTimeout = 5.seconds
    }.start(wait = true)
}

fun Application.module() {
    configureSerialization()
    configureServerFeatures(startTimestamp)
    configureHeaderValidation()
    configureRouting()
}
