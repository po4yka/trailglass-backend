package com.trailglass.backend.visit

import com.trailglass.backend.plugins.DefaultFeatureRateLimit
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.rateLimit
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject
import java.time.Instant
import java.util.UUID

fun Route.placeVisitRoutes() {
    val service by inject<PlaceVisitService>()

    rateLimit(DefaultFeatureRateLimit) {
        route("/place-visits") {
            post("/batch") {
                val visits = call.receive<List<PlaceVisit>>()
                call.respond(service.upsertVisits(visits))
            }

            get {
                val userId = call.request.queryParameters["userId"]?.let(UUID::fromString)
                val updatedAfter = call.request.queryParameters["updatedAfter"]?.let(Instant::parse)
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 200

                if (userId == null) {
                    call.respond(HttpStatusCode.BadRequest, "userId is required")
                    return@get
                }

                call.respond(service.listVisits(userId, updatedAfter, limit))
            }

            delete("/{id}") {
                val userId = call.request.queryParameters["userId"]?.let(UUID::fromString)
                val id = call.parameters["id"]?.let(UUID::fromString)

                if (userId == null || id == null) {
                    call.respond(HttpStatusCode.BadRequest, "userId and id are required")
                    return@delete
                }

                call.respond(service.deleteVisits(userId, listOf(id)))
            }
        }
    }
}
