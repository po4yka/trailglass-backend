package com.trailglass.backend.storage

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant

fun Route.inlineStorageRoutes(storage: ObjectStorageService) {
    val postgresStorage = storage as? PostgresObjectStorageService ?: return

    route("/storage/inline") {
        put("/object") {
            val key = call.request.queryParameters["key"]?.let { URLDecoder.decode(it, StandardCharsets.UTF_8) }
                ?: return@put call.respond(HttpStatusCode.BadRequest, "key required")
            val token = call.request.queryParameters["token"]
                ?: return@put call.respond(HttpStatusCode.BadRequest, "token required")
            val contentType = call.request.headers["Content-Type"] ?: "application/octet-stream"

            if (!postgresStorage.verifyToken(key, token, InlineUrlSigner.Operation.UPLOAD, Instant.now())) {
                return@put call.respond(HttpStatusCode.Forbidden)
            }

            val channel = call.receiveChannel()
            val bytes = withContext(Dispatchers.IO) { channel.toByteArray() }
            postgresStorage.putBytes(key, contentType, bytes)
            call.respond(HttpStatusCode.NoContent)
        }

        get("/object") {
            val key = call.request.queryParameters["key"]?.let { URLDecoder.decode(it, StandardCharsets.UTF_8) }
                ?: return@get call.respond(HttpStatusCode.BadRequest, "key required")
            val token = call.request.queryParameters["token"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "token required")

            if (!postgresStorage.verifyToken(key, token, InlineUrlSigner.Operation.DOWNLOAD, Instant.now())) {
                return@get call.respond(HttpStatusCode.Forbidden)
            }

            val stream = postgresStorage.openStream(key)
            call.respondOutputStream(status = HttpStatusCode.OK) {
                stream.use { input ->
                    input.copyTo(this)
                }
            }
        }
    }
}

private suspend fun io.ktor.utils.io.ByteReadChannel.toByteArray(): ByteArray = withContext(Dispatchers.IO) {
    val packet = readRemaining()
    packet.readBytes()
}
