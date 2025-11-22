package com.trailglass.backend.metrics

import com.trailglass.backend.config.AppConfig
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.koin.ktor.ext.inject

fun Route.metricsRoutes() {
    val config by inject<AppConfig>()
    val meterRegistry by inject<MeterRegistry>()

    if (!config.enableMetrics) {
        return
    }

    get("/metrics") {
        // Check for optional Cloudflare Access authentication
        if (config.cloudflareAccess != null) {
            val clientId = call.request.headers["CF-Access-Client-Id"]
            val clientSecret = call.request.headers["CF-Access-Client-Secret"]

            if (clientId != config.cloudflareAccess.clientId || clientSecret != config.cloudflareAccess.clientSecret) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid Cloudflare Access credentials"))
                return@get
            }
        }

        if (meterRegistry is PrometheusMeterRegistry) {
            val prometheusRegistry = meterRegistry as PrometheusMeterRegistry
            call.respondText(prometheusRegistry.scrape(), io.ktor.http.ContentType.parse("text/plain; version=0.0.4"))
        } else {
            call.respond(HttpStatusCode.ServiceUnavailable, mapOf("error" to "Metrics not enabled"))
        }
    }
}
