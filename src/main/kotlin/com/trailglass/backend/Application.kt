package com.trailglass.backend

import com.trailglass.backend.routes.apiRoutes
import com.trailglass.backend.common.InstantSerializer
import com.trailglass.backend.common.UUIDSerializer
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.slf4j.event.Level
import kotlin.time.Duration.Companion.minutes
import java.util.UUID

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    val serializers = SerializersModule {
        contextual(UUIDSerializer)
        contextual(InstantSerializer)
    }

    install(ContentNegotiation) {
        json(
            Json {
                serializersModule = serializers
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
        )
    }

    install(CallId) {
        header(io.ktor.http.HttpHeaders.XRequestId)
        generate { UUID.randomUUID().toString() }
        verify { it.isNotBlank() }
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled exception", cause)
            call.respondText(
                "Internal server error",
                status = io.ktor.http.HttpStatusCode.InternalServerError
            )
        }
    }

    install(CallLogging) {
        level = Level.INFO
    }

    install(RateLimit) {
        register(RateLimitName("api")) {
            rateLimiter(limit = 100, refillPeriod = 1.minutes)
            requestKey { call -> call.request.local.remoteHost }
        }
    }

    install(DoubleReceive)

    install(Authentication) { }

    routing {
        route("/api") {
            route("/v1") {
                apiRoutes()
            }
        }
    }
}
