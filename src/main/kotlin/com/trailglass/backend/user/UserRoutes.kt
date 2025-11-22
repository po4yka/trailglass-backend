package com.trailglass.backend.user

import com.trailglass.backend.plugins.ApiException
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
                val userId = try {
                    call.request.queryParameters["userId"]?.let(UUID::fromString)
                } catch (e: IllegalArgumentException) {
                    throw ApiException(
                        status = HttpStatusCode.BadRequest,
                        code = "invalid_uuid",
                        message = "Invalid userId format",
                        details = mapOf("field" to "userId")
                    )
                }

                if (userId == null) {
                    throw ApiException(
                        status = HttpStatusCode.BadRequest,
                        code = "missing_parameter",
                        message = "userId is required",
                        details = mapOf("field" to "userId")
                    )
                }

                val profile = service.getProfile(userId)
                if (profile == null) {
                    throw ApiException(
                        status = HttpStatusCode.NotFound,
                        code = "profile_not_found",
                        message = "Profile not found",
                        details = mapOf("userId" to userId.toString())
                    )
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
                val userId = try {
                    call.request.queryParameters["userId"]?.let(UUID::fromString)
                } catch (e: IllegalArgumentException) {
                    throw ApiException(
                        status = HttpStatusCode.BadRequest,
                        code = "invalid_uuid",
                        message = "Invalid userId format",
                        details = mapOf("field" to "userId")
                    )
                }
                val updatedAfter = try {
                    call.request.queryParameters["updatedAfter"]?.let(Instant::parse)
                } catch (e: Exception) {
                    throw ApiException(
                        status = HttpStatusCode.BadRequest,
                        code = "invalid_timestamp",
                        message = "Invalid updatedAfter format",
                        details = mapOf("field" to "updatedAfter")
                    )
                }
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100

                if (userId == null) {
                    throw ApiException(
                        status = HttpStatusCode.BadRequest,
                        code = "missing_parameter",
                        message = "userId is required",
                        details = mapOf("field" to "userId")
                    )
                }

                call.respond(service.listDevices(userId, updatedAfter, limit))
            }

            delete("/{id}") {
                val userId = try {
                    call.request.queryParameters["userId"]?.let(UUID::fromString)
                } catch (e: IllegalArgumentException) {
                    throw ApiException(
                        status = HttpStatusCode.BadRequest,
                        code = "invalid_uuid",
                        message = "Invalid userId format",
                        details = mapOf("field" to "userId")
                    )
                }
                val id = try {
                    call.parameters["id"]?.let(UUID::fromString)
                } catch (e: IllegalArgumentException) {
                    throw ApiException(
                        status = HttpStatusCode.BadRequest,
                        code = "invalid_uuid",
                        message = "Invalid id format",
                        details = mapOf("field" to "id")
                    )
                }

                if (userId == null || id == null) {
                    val missingFields = mutableListOf<String>()
                    if (userId == null) missingFields.add("userId")
                    if (id == null) missingFields.add("id")
                    throw ApiException(
                        status = HttpStatusCode.BadRequest,
                        code = "missing_parameters",
                        message = "Missing required parameters: ${missingFields.joinToString(", ")}",
                        details = mapOf("missingFields" to missingFields.joinToString(", "))
                    )
                }

                val result = service.deleteDevice(userId, id)
                if (result == null) {
                    throw ApiException(
                        status = HttpStatusCode.NotFound,
                        code = "device_not_found",
                        message = "Device not found",
                        details = mapOf("userId" to userId.toString(), "deviceId" to id.toString())
                    )
                } else {
                    call.respond(result)
                }
            }
        }
    }
}
