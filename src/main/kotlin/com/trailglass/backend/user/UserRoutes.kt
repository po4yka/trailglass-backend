package com.trailglass.backend.user

import com.trailglass.backend.plugins.DefaultFeatureRateLimit
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
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

fun Route.userProfileRoutes() {
    val service by inject<UserProfileService>()

    rateLimit(DefaultFeatureRateLimit) {
        route("/profile") {
            get {
                val userId = call.request.queryParameters["userId"]?.let(UUID::fromString)

                if (userId == null) {
                    call.respond(HttpStatusCode.BadRequest, "userId is required")
                    return@get
                }

                val profile = service.getProfile(userId)
                if (profile == null) {
                    call.respond(HttpStatusCode.NotFound, "Profile not found")
                    return@get
                }

                // Calculate user statistics
                val statistics = try {
                    service.getUserStatistics(userId)
                } catch (e: Exception) {
                    // Log the error and return default statistics on failure
                    call.application.environment.log.error("Failed to calculate statistics for user $userId", e)
                    UserProfileStatistics()
                }

                val response = UserProfileResponse(
                    userId = profile.id,
                    email = profile.email,
                    displayName = profile.displayName,
                    profilePhotoUrl = null,
                    createdAt = profile.updatedAt,
                    statistics = statistics
                )
                call.respond(response)
            }

            put {
                val profile = call.receive<UserProfile>()
                call.respond(service.upsertProfile(profile))
            }
        }

        route("/devices") {
            post {
                val device = call.receive<DeviceProfile>()
                call.respond(HttpStatusCode.Created, service.registerDevice(device))
            }

            get {
                val userId = call.request.queryParameters["userId"]?.let(UUID::fromString)
                val updatedAfter = call.request.queryParameters["updatedAfter"]?.let(Instant::parse)
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100

                if (userId == null) {
                    call.respond(HttpStatusCode.BadRequest, "userId is required")
                    return@get
                }

                call.respond(service.listDevices(userId, updatedAfter, limit))
            }

            delete("/{id}") {
                val userId = call.request.queryParameters["userId"]?.let(UUID::fromString)
                val id = call.parameters["id"]?.let(UUID::fromString)

                if (userId == null || id == null) {
                    call.respond(HttpStatusCode.BadRequest, "userId and id are required")
                    return@delete
                }

                val result = service.deleteDevice(userId, id)
                if (result == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(result)
                }
            }
        }
    }
}
