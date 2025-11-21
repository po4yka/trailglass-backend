package com.trailglass.backend.export

import com.trailglass.backend.plugins.DefaultFeatureRateLimit
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.rateLimit
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.exportRoutes() {
    val exportService by inject<ExportService>()

    rateLimit(DefaultFeatureRateLimit) {
        route("/export") {
            post {
                val request = call.receive<ExportRequest>()
                call.respond(HttpStatusCode.Accepted, exportService.requestExport(request.userId, request.deviceId, request.email))
            }

            get("/{id}/status") {
                val exportId = call.parameters["id"]?.let(UUID::fromString)
                val userId = call.request.queryParameters["userId"]?.let(UUID::fromString)

                if (exportId == null || userId == null) {
                    call.respond(HttpStatusCode.BadRequest, "export id and userId are required")
                    return@get
                }

                call.respond(exportService.getStatus(exportId, userId))
            }
        }
    }
}

@Serializable
private data class ExportRequest(
    val userId: UUID,
    val deviceId: UUID,
    val email: String? = null,
)
