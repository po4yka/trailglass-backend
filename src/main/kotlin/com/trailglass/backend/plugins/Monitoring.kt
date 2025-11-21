package com.trailglass.backend.plugins

import com.trailglass.backend.config.AppConfig
import io.ktor.http.HttpHeaders
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.doublereceive.DoubleReceive
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimiter
import org.slf4j.event.Level
import java.time.Duration
import java.util.UUID

fun Application.configureMonitoring(config: AppConfig) {
    install(CallId) {
        header(HttpHeaders.XRequestId)
        generate { UUID.randomUUID().toString() }
        verify { it.isNotBlank() }
    }

    install(CallLogging) {
        callIdMdc("call_id")
        level = Level.INFO
    }

    install(DoubleReceive)

    install(RateLimit) {
        register(RateLimitName("global")) {
            rateLimiter(
                limit = config.rateLimitPerMinute,
                refillPeriod = Duration.ofMinutes(1)
            )
        }

        register(AuthRateLimit) {
            rateLimiter(limit = 5, refillPeriod = Duration.ofMinutes(1))
        }

        register(SyncRateLimit) {
            rateLimiter(limit = 10, refillPeriod = Duration.ofMinutes(1))
        }

        register(LocationBatchRateLimit) {
            rateLimiter(limit = 100, refillPeriod = Duration.ofHours(1))
        }

        register(PhotoUploadRateLimit) {
            rateLimiter(limit = 50, refillPeriod = Duration.ofHours(1))
        }

        register(DefaultFeatureRateLimit) {
            rateLimiter(limit = 100, refillPeriod = Duration.ofMinutes(1))
        }
    }
}

val GlobalRateLimit = RateLimitName("global")
val AuthRateLimit = RateLimitName("auth")
val SyncRateLimit = RateLimitName("sync")
val LocationBatchRateLimit = RateLimitName("locations")
val PhotoUploadRateLimit = RateLimitName("photos")
val DefaultFeatureRateLimit = RateLimitName("default")
