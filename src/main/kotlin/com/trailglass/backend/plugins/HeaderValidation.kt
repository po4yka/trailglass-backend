package com.trailglass.backend.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.routing.Route
import io.ktor.server.routing.createRouteScopedPlugin

class MissingHeaderException(val header: String) : RuntimeException("Missing required header: $header")

private val requiredHeaders = listOf("X-Device-ID", "X-App-Version")

fun Application.configureHeaderValidation() {
    install(HeaderValidationPlugin)
}

val HeaderValidationPlugin = createRouteScopedPlugin(name = "HeaderValidation") {
    onCall { call ->
        requiredHeaders.forEach { header ->
            if (call.request.headers[header].isNullOrBlank()) {
                throw MissingHeaderException(header)
            }
        }
    }
}

fun Route.requiresClientHeaders(build: Route.() -> Unit): Route =
    createChild(this.selector).apply {
        install(HeaderValidationPlugin)
        build()
    }
