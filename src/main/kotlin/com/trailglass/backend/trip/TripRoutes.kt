package com.trailglass.backend.trip

import com.trailglass.backend.plugins.DefaultFeatureRateLimit
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject
import java.time.Instant
import java.util.UUID

fun Route.tripRoutes() {
    val tripService by inject<TripService>()

    rateLimit(DefaultFeatureRateLimit) {
        route("/trips") {
            post {
                val request = call.receive<TripUpsertRequest>()
                call.respond(HttpStatusCode.Created, tripService.upsertTrip(request))
            }

            get {
                val userId = call.request.queryParameters["userId"]?.let(UUID::fromString)
                val updatedAfter = call.request.queryParameters["updatedAfter"]?.let(Instant::parse)
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100

                if (userId == null) {
                    call.respond(HttpStatusCode.BadRequest, "userId is required")
                    return@get
                }

                call.respond(tripService.listTrips(userId, updatedAfter, limit))
            }

            delete("/{id}") {
                val userId = call.request.queryParameters["userId"]?.let(UUID::fromString)
                val id = call.parameters["id"]?.let(UUID::fromString)

                if (userId == null || id == null) {
                    call.respond(HttpStatusCode.BadRequest, "userId and id are required")
                    return@delete
                }

                call.respond(tripService.deleteTrip(userId, id))
            }
        }
    }
}
