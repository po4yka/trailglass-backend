package com.trailglass.backend.routes

import com.trailglass.backend.plugins.ApiException
import com.trailglass.backend.plugins.ErrorResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private const val DEVICE_HEADER = "X-Device-ID"
private const val APP_VERSION_HEADER = "X-App-Version"

@kotlinx.serialization.Serializable
data class HealthStatus(val status: String = "ok")

fun Route.apiRoutes() {
    intercept(ApplicationCallPipeline.Plugins) {
        val deviceId = call.request.headers[DEVICE_HEADER]
        val appVersion = call.request.headers[APP_VERSION_HEADER]

        if (deviceId.isNullOrBlank() || appVersion.isNullOrBlank()) {
            val missingHeaders = mutableListOf<String>()
            if (deviceId.isNullOrBlank()) missingHeaders.add(DEVICE_HEADER)
            if (appVersion.isNullOrBlank()) missingHeaders.add(APP_VERSION_HEADER)

            throw ApiException(
                status = HttpStatusCode.BadRequest,
                code = "missing_headers",
                message = "Missing required headers: ${missingHeaders.joinToString(", ")}",
                details = mapOf("missingHeaders" to missingHeaders.joinToString(", "))
            )
        }
    }

    get("/health") {
        call.respond(HttpStatusCode.OK, HealthStatus())
    }
}
