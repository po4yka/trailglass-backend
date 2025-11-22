package com.trailglass.backend.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.ApplicationStarted
import io.ktor.util.AttributeKey
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callId
import io.ktor.server.plugins.callid.generate
import io.ktor.server.plugins.callid.header
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.rateLimiter
import io.ktor.server.request.path
import org.slf4j.event.Level
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import com.trailglass.backend.health.ComponentHealth
import com.trailglass.backend.health.HealthStatus
import kotlinx.serialization.Serializable

internal val StartupTimeKey = AttributeKey<Long>("StartupTime")

@Serializable
internal data class HealthResponse(
    val status: String,
    val uptimeSeconds: Long,
    val version: String = "0.1.0",
    val components: Map<String, ComponentHealthResponse>? = null
)

@Serializable
internal data class ComponentHealthResponse(
    val status: String,
    val responseTime: String? = null,
    val error: String? = null
)

internal fun ComponentHealth.toResponse(): ComponentHealthResponse {
    return ComponentHealthResponse(
        status = when (status) {
            HealthStatus.UP -> "up"
            HealthStatus.DOWN -> "down"
            HealthStatus.DEGRADED -> "degraded"
            HealthStatus.DISABLED -> "disabled"
            HealthStatus.HEALTHY -> "healthy"
            HealthStatus.UNHEALTHY -> "unhealthy"
        },
        responseTime = responseTime,
        error = error
    )
}

fun Application.configureServerFeatures(startTimestamp: Long, rateLimitPerMinute: Long) {
    attributes.put(StartupTimeKey, startTimestamp)

    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader("X-Device-ID")
        allowHeader("X-App-Version")
        allowHeader("X-Request-ID")
        allowCredentials = true

        // Allow localhost for development
        anyHost()
    }

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
            rateLimiter(limit = rateLimitPerMinute.toInt(), refillPeriod = 1.minutes)
        }
    }

    environment.monitor.subscribe(ApplicationStarted) {
        environment.log.info("Trailglass backend started")
    }
}
