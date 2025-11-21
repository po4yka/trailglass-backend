package com.trailglass.backend.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.ApplicationStarted
import io.ktor.util.AttributeKey
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callId
import io.ktor.server.plugins.callid.generate
import io.ktor.server.plugins.callid.header
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.rateLimiter
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.respond
import org.slf4j.event.Level
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import com.trailglass.backend.plugins.MissingHeaderException

internal val StartupTimeKey = AttributeKey<Long>("StartupTime")

internal data class ErrorResponse(val message: String)
internal data class HealthResponse(
    val status: String,
    val uptimeSeconds: Long,
    val version: String = "0.1.0"
)

fun Application.configureServerFeatures(startTimestamp: Long) {
    attributes.put(StartupTimeKey, startTimestamp)

    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/api") }
    }

    install(CallId) {
        header("X-Request-ID")
        generate { UUID.randomUUID().toString() }
        verify { it.isNotBlank() }
    }

    install(RateLimit) {
        global {
            rateLimiter(limit = 100, refillPeriod = 1.minutes)
        }
    }

    install(StatusPages) {
        exception<MissingHeaderException> { cause, call ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: "Missing required header"))
        }
        exception<Throwable> { cause, call ->
            application.log.error("Unhandled exception", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Internal server error"))
        }
    }

    environment.monitor.subscribe(ApplicationStarted) {
        environment.log.info("Trailglass backend started")
    }
}
