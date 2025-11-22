package com.trailglass.backend.plugins

import com.trailglass.backend.auth.authRoutes
import com.trailglass.backend.export.exportRoutes
import com.trailglass.backend.location.locationRoutes
import com.trailglass.backend.metrics.metricsRoutes
import com.trailglass.backend.photo.photoRoutes
import com.trailglass.backend.settings.settingsRoutes
import com.trailglass.backend.storage.inlineStorageRoutes
import com.trailglass.backend.sync.syncRoutes
import com.trailglass.backend.trip.tripRoutes
import com.trailglass.backend.user.userProfileRoutes
import com.trailglass.backend.visit.placeVisitRoutes
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.plugins.ratelimit.rateLimit
import org.koin.ktor.ext.inject
import com.trailglass.backend.plugins.AuthJwt
import com.trailglass.backend.plugins.AuthRateLimit
import com.trailglass.backend.storage.ObjectStorageService

fun Application.configureRouting() {
    val storage by inject<ObjectStorageService>()

    routing {
        metricsRoutes()

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

            inlineStorageRoutes(storage)

            rateLimit(AuthRateLimit) {
                authRoutes()
            }

            authenticate(AuthJwt) {
                syncRoutes()
                locationRoutes()
                tripRoutes()
                photoRoutes()
                settingsRoutes()
                exportRoutes()
                placeVisitRoutes()
                userProfileRoutes()
            }
        }
    }
}
