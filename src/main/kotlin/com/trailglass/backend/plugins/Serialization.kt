package com.trailglass.backend.plugins

import com.trailglass.backend.common.InstantSerializer
import com.trailglass.backend.common.UUIDSerializer
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.doublereceive.DoubleReceive
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

fun Application.configureSerialization() {
    install(DoubleReceive)

    val serializers = SerializersModule {
        contextual(UUIDSerializer)
        contextual(InstantSerializer)
    }

    install(ContentNegotiation) {
        json(
            Json {
                serializersModule = serializers
                ignoreUnknownKeys = true
                prettyPrint = false
                encodeDefaults = true
                explicitNulls = false
            }
        )
    }
}
