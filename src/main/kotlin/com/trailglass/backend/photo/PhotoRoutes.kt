@file:UseSerializers(UUIDSerializer::class, InstantSerializer::class)

package com.trailglass.backend.photo

import com.trailglass.backend.plugins.DefaultFeatureRateLimit
import com.trailglass.backend.plugins.PhotoUploadRateLimit
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
import io.ktor.server.routing.route
import io.ktor.server.plugins.ratelimit.rateLimit
import org.koin.ktor.ext.inject
import java.time.Instant
import java.util.UUID
import com.trailglass.backend.common.InstantSerializer
import com.trailglass.backend.common.UUIDSerializer

fun Route.photoRoutes() {
    val photoService by inject<PhotoService>()

    route("/photos") {
        rateLimit(PhotoUploadRateLimit) {
            post {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val request = call.receive<PhotoUploadRequest>()
                val userId = UUID.fromString(principal.subject!!)
                val deviceId = principal.payload.getClaim("deviceId").asString()?.let(UUID::fromString)
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "deviceId missing from token")

                call.respond(HttpStatusCode.Created, photoService.createUpload(userId, deviceId, request))
            }
        }

        rateLimit(DefaultFeatureRateLimit) {
            post("/{id}/confirm") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val userId = UUID.fromString(principal.subject!!)
                val photoId = call.parameters["id"]?.let(UUID::fromString)
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "photo id is required")

                call.respond(photoService.confirmUpload(photoId, userId))
            }

            get {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val userId = UUID.fromString(principal.subject!!)
                val updatedAfter = call.request.queryParameters["updatedAfter"]?.let(Instant::parse)
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100

                call.respond(photoService.fetchMetadata(userId, updatedAfter, limit))
            }

            get("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val userId = UUID.fromString(principal.subject!!)
                val photoId = call.parameters["id"]?.let(UUID::fromString)
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "photo id is required")

                call.respond(photoService.getPhoto(photoId, userId))
            }

            delete("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized)
                val userId = UUID.fromString(principal.subject!!)
                val photoId = call.parameters["id"]?.let(UUID::fromString)
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "photo id is required")

                photoService.deletePhoto(photoId, userId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
