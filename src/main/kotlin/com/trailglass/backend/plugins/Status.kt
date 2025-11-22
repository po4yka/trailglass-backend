package com.trailglass.backend.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callid.callId
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable

fun Application.configureStatusHandling() {
    install(StatusPages) {
        exception<ApiException> { call, cause ->
            val error = ErrorResponse(code = cause.code, message = cause.message, details = cause.details)
            call.respond(cause.status, error)
        }

        exception<MissingHeaderException> { call, cause ->
            val requestId = call.callId
            application.log.warn("Missing required header: ${cause.header}", cause)
            val error = ErrorResponse(
                code = "missing_header",
                message = "Missing required header: ${cause.header}",
                details = mapOf("header" to cause.header, "requestId" to (requestId ?: ""))
            )
            call.respond(HttpStatusCode.BadRequest, error)
        }

        exception<Throwable> { call, cause ->
            val requestId = call.callId
            application.log.error("Unhandled exception [requestId: $requestId]", cause)
            val error = ErrorResponse(
                code = "internal_error",
                message = "An unexpected error occurred",
                details = mapOf("requestId" to (requestId ?: ""))
            )
            call.respond(HttpStatusCode.InternalServerError, error)
        }
    }
}

@Serializable
data class ErrorResponse(
    val code: String,
    val message: String,
    val details: Map<String, String?> = emptyMap()
)
