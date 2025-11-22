@file:UseSerializers(UUIDSerializer::class, InstantSerializer::class)

package com.trailglass.backend.export

import com.trailglass.backend.plugins.DefaultFeatureRateLimit
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.plugins.ratelimit.rateLimit
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.koin.ktor.ext.inject
import java.util.UUID
import com.trailglass.backend.common.InstantSerializer
import com.trailglass.backend.common.UUIDSerializer

fun Route.exportRoutes() {
    val exportService by inject<ExportService>()

    rateLimit(DefaultFeatureRateLimit) {
        route("/exports") {
            post("/request") {
                val request = call.receive<ExportRequest>()
                val job = exportService.requestExport(request)
                call.respond(HttpStatusCode.Accepted, mapOf(
                    "exportId" to job.id,
                    "status" to job.status.name,
                    "estimatedCompletionTime" to (job.updatedAt.plusSeconds(300)).toString()
                ))
            }

            post {
                val request = call.receive<ExportRequest>()
                call.respond(HttpStatusCode.Accepted, exportService.requestExport(request))
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

