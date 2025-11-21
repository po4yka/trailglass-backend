package com.trailglass.backend.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        route("/api/v1") {
            install(HeaderValidationPlugin)

            get("/health") {
                val uptime = (System.currentTimeMillis() - attributes[StartupTimeKey]) / 1000
                call.respond(
                    HealthResponse(
                        status = "ok",
                        uptimeSeconds = uptime,
                        version = environment.config.propertyOrNull("ktor.deployment.version")?.getString() ?: "dev"
                    )
                )
            }
        }
    }
}
