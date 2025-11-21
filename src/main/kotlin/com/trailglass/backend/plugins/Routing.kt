package com.trailglass.backend.plugins

import com.trailglass.backend.auth.authRoutes
import com.trailglass.backend.plugins.AuthJwt
import com.trailglass.backend.plugins.AuthRateLimit
import com.trailglass.backend.export.exportRoutes
import com.trailglass.backend.location.locationRoutes
import com.trailglass.backend.photo.photoRoutes
import com.trailglass.backend.settings.settingsRoutes
import com.trailglass.backend.sync.syncRoutes
import com.trailglass.backend.trip.tripRoutes
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.rateLimit
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        rateLimit(GlobalRateLimit) {
            route("/api/v1") {
                get("/health") {
                    call.respondText("ok")
                }

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
                }
            }
        }
    }
}
