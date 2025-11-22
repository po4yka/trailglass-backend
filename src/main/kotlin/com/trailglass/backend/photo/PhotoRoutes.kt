@file:UseSerializers(UUIDSerializer::class, InstantSerializer::class)

package com.trailglass.backend.photo

import com.trailglass.backend.plugins.DefaultFeatureRateLimit
import com.trailglass.backend.plugins.PhotoUploadRateLimit
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.call
import io.ktor.server.auth.principal
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.plugins.ratelimit.rateLimit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.UUID
import com.trailglass.backend.common.InstantSerializer
import com.trailglass.backend.common.UUIDSerializer

fun Route.photoRoutes() {
    val photoService by inject<PhotoService>()
    val json = Json { ignoreUnknownKeys = true }

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

            post("/upload") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val userId = UUID.fromString(principal.subject!!)
                val deviceId = principal.payload.getClaim("deviceId").asString()?.let(UUID::fromString)
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "deviceId missing from token")

                val multipart = call.receiveMultipart()
                var fileBytes: ByteArray? = null
                var fileName: String? = null
                var mimeType: String? = null
                var metadata: PhotoMetadataRequest? = null

                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            if (part.name == "file") {
                                fileName = part.originalFileName ?: "photo.jpg"
                                mimeType = part.contentType?.toString() ?: "image/jpeg"
                                fileBytes = withContext(Dispatchers.IO) {
                                    part.streamProvider().use { stream ->
                                        val buffer = ByteArrayOutputStream()
                                        stream.copyTo(buffer)
                                        buffer.toByteArray()
                                    }
                                }
                            }
                            part.dispose()
                        }
                        is PartData.FormItem -> {
                            if (part.name == "metadata") {
                                metadata = json.decodeFromString<PhotoMetadataRequest>(part.value)
                            }
                        }
                        else -> part.dispose()
                    }
                }

                if (fileBytes == null || fileName == null || mimeType == null) {
                    call.respond(HttpStatusCode.BadRequest, "File is required")
                    return@post
                }

                val result = photoService.uploadPhoto(userId, deviceId, fileBytes!!, fileName!!, mimeType!!, metadata)
                call.respond(HttpStatusCode.Created, mapOf(
                    "id" to result.photo.id,
                    "url" to result.download?.url?.toString(),
                    "thumbnailUrl" to result.thumbnailUrl?.url?.toString(),
                    "serverVersion" to result.photo.serverVersion,
                    "syncTimestamp" to result.photo.uploadedAt?.toString() ?: Instant.now().toString()
                ))
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
