package com.trailglass.backend.plugins

import com.trailglass.backend.config.AppConfig
import io.ktor.http.HttpHeaders
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.doublereceive.DoubleReceive
import io.ktor.server.plugins.ratelimit.*
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import org.koin.ktor.ext.inject
import org.slf4j.event.Level
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import java.util.UUID

fun Application.configureMonitoring(config: AppConfig) {
    install(CallId) {
        header(HttpHeaders.XRequestId)
        generate { UUID.randomUUID().toString() }
        verify { it.isNotBlank() }
    }

    install(CallLogging) { level = Level.INFO }

    install(DoubleReceive)

    if (config.enableMetrics) {
        val meterRegistry by inject<MeterRegistry>()

        install(MicrometerMetrics) {
            registry = meterRegistry

            // Register JVM metrics
            meterBinders = listOf(
                JvmMemoryMetrics(),
                JvmGcMetrics(),
                JvmThreadMetrics(),
                ClassLoaderMetrics(),
                ProcessorMetrics()
            )
        }
    }

    install(RateLimit) {
        register(RateLimitName("global")) {
            rateLimiter(
                limit = config.rateLimitPerMinute.toInt(),
                refillPeriod = 1.minutes
            )
        }

        register(AuthRateLimit) {
            rateLimiter(limit = 5, refillPeriod = 1.minutes)
        }

        register(SyncRateLimit) {
            rateLimiter(limit = 10, refillPeriod = 1.minutes)
        }

        register(LocationBatchRateLimit) {
            rateLimiter(limit = 100, refillPeriod = 1.hours)
        }

        register(PhotoUploadRateLimit) {
            rateLimiter(limit = 50, refillPeriod = 1.hours)
        }

        register(DefaultFeatureRateLimit) {
            rateLimiter(limit = 100, refillPeriod = 1.minutes)
        }
    }
}


