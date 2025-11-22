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
import com.trailglass.backend.health.HealthCheckService
import com.trailglass.backend.health.HealthStatus
import io.ktor.http.HttpStatusCode

fun Application.configureRouting() {
    val storage by inject<ObjectStorageService>()
    val healthCheckService by inject<HealthCheckService>()

    routing {
        metricsRoutes()

        route("/api/v1") {
            install(HeaderValidationPlugin)

            get("/health") {
                val uptime = (System.currentTimeMillis() - attributes[StartupTimeKey]) / 1000
                val components = healthCheckService.checkAllComponents()
                val overallStatus = healthCheckService.determineOverallStatus(components)

                val statusCode = when (overallStatus) {
                    HealthStatus.HEALTHY -> HttpStatusCode.OK
                    HealthStatus.DEGRADED -> HttpStatusCode.OK
                    HealthStatus.UNHEALTHY -> HttpStatusCode.ServiceUnavailable
                    else -> HttpStatusCode.OK
                }

                call.respond(
                    statusCode,
                    HealthResponse(
                        status = when (overallStatus) {
                            HealthStatus.HEALTHY -> "healthy"
                            HealthStatus.DEGRADED -> "degraded"
                            HealthStatus.UNHEALTHY -> "unhealthy"
                            else -> "ok"
                        },
                        uptimeSeconds = uptime,
                        version = environment.config.propertyOrNull("ktor.deployment.version")?.getString() ?: "dev",
                        components = components.mapValues { it.value.toResponse() }
                    )
                )
            }

            get("/health/live") {
                // Liveness probe - just check if the application is running
                // This should always return 200 unless the app is completely dead
                call.respond(
                    HttpStatusCode.OK,
                    mapOf("status" to "alive")
                )
            }

            get("/health/ready") {
                // Readiness probe - check if the app is ready to serve traffic
                // Database and storage must be UP for the app to be ready
                val isReady = healthCheckService.isReady()

                if (isReady) {
                    call.respond(
                        HttpStatusCode.OK,
                        mapOf("status" to "ready")
                    )
                } else {
                    call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        mapOf("status" to "not ready")
                    )
                }
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
