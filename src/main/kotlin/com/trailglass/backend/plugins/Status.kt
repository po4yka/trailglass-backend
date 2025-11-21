package com.trailglass.backend.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable

fun Application.configureStatusHandling() {
    install(StatusPages) {
        exception<ApiException> { call, cause ->
            val error = ErrorResponse(code = cause.code, message = cause.message, details = cause.details)
            call.respond(cause.status, error)
        }

        exception<Throwable> { call, cause ->
            val error = ErrorResponse(code = "internal_error", message = cause.message ?: "Unexpected error")
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
