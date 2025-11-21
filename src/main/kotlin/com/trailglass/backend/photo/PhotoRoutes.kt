@file:UseSerializers(UUIDSerializer::class, InstantSerializer::class)

package com.trailglass.backend.photo

import com.trailglass.backend.plugins.DefaultFeatureRateLimit
import com.trailglass.backend.plugins.PhotoUploadRateLimit
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.plugins.ratelimit.rateLimit
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.koin.ktor.ext.inject
import java.time.Instant
import java.util.UUID
import com.trailglass.backend.common.InstantSerializer
import com.trailglass.backend.common.UUIDSerializer

fun Route.photoRoutes() {
    val photoService by inject<PhotoService>()

    route("/photos") {
        rateLimit(PhotoUploadRateLimit) {
            post("/upload") {
                val request = call.receive<PhotoUploadRequest>()
                call.respond(HttpStatusCode.Created, photoService.requestUpload(request))
            }
        }

        rateLimit(DefaultFeatureRateLimit) {
            post("/{id}/confirm") {
                val photoId = call.parameters["id"]?.let(UUID::fromString)
                val confirmation = call.receive<ConfirmUploadRequest>()

                if (photoId == null) {
                    call.respond(HttpStatusCode.BadRequest, "photo id is required")
                    return@post
                }

                call.respond(photoService.confirmUpload(photoId, confirmation.userId, confirmation.deviceId))
            }

            get {
                val userId = call.request.queryParameters["userId"]?.let(UUID::fromString)
                val updatedAfter = call.request.queryParameters["updatedAfter"]?.let(Instant::parse)
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100

                if (userId == null) {
                    call.respond(HttpStatusCode.BadRequest, "userId is required")
                    return@get
                }

                call.respond(photoService.fetchMetadata(userId, updatedAfter, limit))
            }

            get("/{id}") {
                val photoId = call.parameters["id"]?.let(UUID::fromString)
                val userId = call.request.queryParameters["userId"]?.let(UUID::fromString)
                val deviceId = call.request.queryParameters["deviceId"]?.let(UUID::fromString)

                if (photoId == null || userId == null || deviceId == null) {
                    call.respond(HttpStatusCode.BadRequest, "photo id, userId, and deviceId are required")
                    return@get
                }

                call.respond(photoService.confirmUpload(photoId, userId, deviceId))
            }

            delete("/{id}") {
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

@Serializable
private data class ConfirmUploadRequest(
    val userId: UUID,
    val deviceId: UUID,
)
