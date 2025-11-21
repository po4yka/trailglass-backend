package com.trailglass.backend.sync

import com.trailglass.backend.plugins.SyncRateLimit
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.rateLimit
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.syncRoutes() {
    val syncService by inject<SyncService>()

    rateLimit(SyncRateLimit) {
        route("/sync") {
            get("/status") {
                val deviceId = call.request.queryParameters["deviceId"]?.let(UUID::fromString)
                val userId = call.request.queryParameters["userId"]?.let(UUID::fromString)

                if (deviceId == null || userId == null) {
                    call.respond(HttpStatusCode.BadRequest, "deviceId and userId are required")
                    return@get
                }

                call.respond(syncService.getStatus(deviceId, userId))
            }

            post("/delta") {
                val request = call.receive<SyncDeltaRequest>()
                call.respond(syncService.applyDelta(request))
            }

            post("/resolve-conflict") {
                val request = call.receive<ConflictResolutionRequest>()
                call.respond(syncService.resolveConflict(request))
            }
        }
    }
}
