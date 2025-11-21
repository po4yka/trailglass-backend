package com.trailglass.backend.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

private const val DEVICE_HEADER = "X-Device-ID"
private const val APP_VERSION_HEADER = "X-App-Version"

@Serializable
data class HealthStatus(val status: String = "ok")

fun Route.apiRoutes() {
    intercept(ApplicationCallPipeline.Plugins) {
        val deviceId = call.request.headers[DEVICE_HEADER]
        val appVersion = call.request.headers[APP_VERSION_HEADER]

        if (deviceId.isNullOrBlank() || appVersion.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing required headers"))
            finish()
            return@intercept
        }
    }

    get("/health") {
        call.respond(HttpStatusCode.OK, HealthStatus())
    }
}
