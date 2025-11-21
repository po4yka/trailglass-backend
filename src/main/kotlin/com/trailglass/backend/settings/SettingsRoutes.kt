package com.trailglass.backend.settings

import com.trailglass.backend.plugins.DefaultFeatureRateLimit
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.rateLimit
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.settingsRoutes() {
    val settingsService by inject<SettingsService>()

    rateLimit(DefaultFeatureRateLimit) {
        get("/settings") {
            val userId = call.request.queryParameters["userId"]?.let(UUID::fromString)

            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, "userId is required")
                return@get
            }

            call.respond(settingsService.getSettings(userId))
        }

        put("/settings") {
            val request = call.receive<SettingsUpdateRequest>()
            call.respond(settingsService.updateSettings(request))
        }
    }
}
