package com.trailglass.backend.plugins

import com.trailglass.backend.location.locationRoutes
import com.trailglass.backend.settings.settingsRoutes
import com.trailglass.backend.trip.tripRoutes
import com.trailglass.backend.user.userProfileRoutes
import com.trailglass.backend.visit.placeVisitRoutes
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.plugins.ratelimit.rateLimit

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

            locationRoutes()
            tripRoutes()
            settingsRoutes()
            placeVisitRoutes()
            userProfileRoutes()
        }
    }
}
