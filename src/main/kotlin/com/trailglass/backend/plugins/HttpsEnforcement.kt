package com.trailglass.backend.plugins

import com.trailglass.backend.config.AppConfig
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.response.respond
import io.ktor.server.routing.createRouteScopedPlugin

class HttpsRequiredException : RuntimeException("HTTPS is required")

class HttpsEnforcementConfig {
    var allowPlainHttp: Boolean = true
}

val HttpsEnforcementPlugin = createRouteScopedPlugin(
    name = "HttpsEnforcement",
    createConfiguration = { HttpsEnforcementConfig() }
) {
    onCall { call ->
        if (!pluginConfig.allowPlainHttp) {
            val forwardedProto = call.request.headers["X-Forwarded-Proto"]
            val scheme = call.request.scheme

            // Check if the request is HTTPS (either directly or via X-Forwarded-Proto)
            val isHttps = scheme == "https" || forwardedProto == "https"

            if (!isHttps) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "error" to "HTTPS_REQUIRED",
                        "message" to "HTTPS is required. Ensure your reverse proxy sets X-Forwarded-Proto: https header.",
                    ),
                )
                return@onCall
            }
        }
    }
}

fun Application.configureHttpsEnforcement(config: AppConfig) {
    if (!config.allowPlainHttp) {
        // Install X-Forwarded-Headers plugin to properly handle forwarded headers
        // This is needed when behind a reverse proxy
        install(XForwardedHeaders) {
            // Trust forwarded headers from any proxy
            // In production, you may want to restrict this to specific IPs
            maxForwardsLimit = 10
        }
    }
}

