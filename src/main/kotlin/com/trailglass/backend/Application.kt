package com.trailglass.backend

import com.trailglass.backend.plugins.configureHeaderValidation
import com.trailglass.backend.plugins.configureRouting
import com.trailglass.backend.plugins.configureSerialization
import com.trailglass.backend.plugins.configureServerFeatures
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlin.time.Duration.Companion.seconds

private val startTimestamp = System.currentTimeMillis()

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, module = Application::module) {
        connector {
            retryAttempts = 3
            requestQueueLimit = 16
            runningLimit = 100
            connectionGroupSize = 4
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
