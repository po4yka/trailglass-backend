package com.trailglass.backend

import com.trailglass.backend.config.ConfigLoader
import com.trailglass.backend.di.appModule
import com.trailglass.backend.persistence.DatabaseFactory
import com.trailglass.backend.persistence.FlywayMigrator
import com.trailglass.backend.plugins.configureAuthentication
import com.trailglass.backend.plugins.configureMonitoring
import com.trailglass.backend.plugins.configureRouting
import com.trailglass.backend.plugins.configureSerialization
import com.trailglass.backend.plugins.configureStatusHandling
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main() {
    val config = ConfigLoader.fromEnv()
    val dataSource = DatabaseFactory.dataSource(config)
    val flyway = FlywayMigrator.migrate(dataSource)

    embeddedServer(Netty, port = config.port, host = config.host) {
        install(Koin) {
            slf4jLogger()
            modules(appModule(config, dataSource, flyway))
        }

        configureSerialization()
        configureStatusHandling()
        configureMonitoring(config)
        configureAuthentication()
        configureRouting()
    }.start(wait = true)
}

fun Application.onApplicationReady() {
    log.info("Trailglass backend started")
}
