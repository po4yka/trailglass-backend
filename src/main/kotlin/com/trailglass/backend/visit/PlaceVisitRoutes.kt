package com.trailglass.backend.visit

import com.trailglass.backend.plugins.DefaultFeatureRateLimit
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.principal
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
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
                val startTime = call.request.queryParameters["startTime"]?.let(Instant::parse)
                val endTime = call.request.queryParameters["endTime"]?.let(Instant::parse)
                val category = call.request.queryParameters["category"]
                val isFavorite = call.request.queryParameters["isFavorite"]?.toBooleanStrictOrNull()
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 200
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

                if (userId == null) {
                    call.respond(HttpStatusCode.BadRequest, "userId is required")
                    return@get
                }

                call.respond(service.listVisits(userId, updatedAfter, startTime, endTime, category, isFavorite, limit, offset))
            }

            get("/{id}") {
                val userId = call.request.queryParameters["userId"]?.let(UUID::fromString)
                val id = call.parameters["id"]?.let(UUID::fromString)

                if (userId == null || id == null) {
                    call.respond(HttpStatusCode.BadRequest, "userId and id are required")
                    return@get
                }

                try {
                    call.respond(service.getVisit(userId, id))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.NotFound, e.message ?: "Place visit not found")
                }
            }

            put("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized)
                val userId = UUID.fromString(principal.subject!!)
                val id = call.parameters["id"]?.let(UUID::fromString)
                    ?: return@put call.respond(HttpStatusCode.BadRequest, "id is required")

                val request = call.receive<PlaceVisitUpdateRequest>()
                val expectedVersion = request.expectedVersion

                try {
                    val updated = service.updateVisit(userId, id, request, expectedVersion)
                    call.respond(mapOf(
                        "id" to updated.id,
                        "serverVersion" to updated.serverVersion,
                        "syncTimestamp" to updated.updatedAt.toString()
                    ))
                } catch (e: IllegalArgumentException) {
                    if (e.message?.contains("Entity was modified") == true) {
                        val currentVersion = service.getVisit(userId, id).serverVersion
                        call.respond(HttpStatusCode.Conflict, mapOf(
                            "error" to "VERSION_CONFLICT",
                            "message" to "Entity was modified by another device",
                            "currentVersion" to currentVersion,
                            "expectedVersion" to (expectedVersion ?: -1)
                        ))
                    } else {
                        call.respond(HttpStatusCode.NotFound, e.message ?: "Place visit not found")
                    }
                }
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
