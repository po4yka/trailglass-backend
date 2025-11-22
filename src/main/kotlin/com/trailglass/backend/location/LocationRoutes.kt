package com.trailglass.backend.location

import com.trailglass.backend.plugins.DefaultFeatureRateLimit
import com.trailglass.backend.plugins.LocationBatchRateLimit
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject
import java.time.Instant
import java.util.UUID

fun Route.locationRoutes() {
    val locationService by inject<LocationService>()

    route("/locations") {
        rateLimit(LocationBatchRateLimit) {
            post("/batch") {
                val request = call.receive<LocationBatchRequest>()
                call.respond(locationService.upsertBatch(request))
            }
        }

        rateLimit(DefaultFeatureRateLimit) {
            get {
                val userId = call.request.queryParameters["userId"]?.let(UUID::fromString)
                val since = call.request.queryParameters["since"]?.let(Instant::parse)
                val startTime = call.request.queryParameters["startTime"]?.let(Instant::parse)
                val endTime = call.request.queryParameters["endTime"]?.let(Instant::parse)
                val minAccuracy = call.request.queryParameters["minAccuracy"]?.toFloatOrNull()
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 200
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

                if (userId == null) {
                    call.respond(HttpStatusCode.BadRequest, "userId is required")
                    return@get
                }

                call.respond(locationService.getLocations(userId, since, startTime, endTime, minAccuracy, limit, offset))
            }

            get("/{id}") {
                val userId = call.request.queryParameters["userId"]?.let(UUID::fromString)
                val id = call.parameters["id"]?.let(UUID::fromString)

                if (userId == null || id == null) {
                    call.respond(HttpStatusCode.BadRequest, "userId and id are required")
                    return@get
                }

                try {
                    call.respond(locationService.getLocation(userId, id))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.NotFound, e.message ?: "Location not found")
                }
            }

            delete("/{id}") {
                val userId = call.request.queryParameters["userId"]?.let(UUID::fromString)
                val id = call.parameters["id"]?.let(UUID::fromString)

                if (userId == null || id == null) {
                    call.respond(HttpStatusCode.BadRequest, "userId and id are required")
                    return@delete
                }

                call.respond(locationService.deleteLocations(userId, listOf(id)))
            }
        }
    }
}
