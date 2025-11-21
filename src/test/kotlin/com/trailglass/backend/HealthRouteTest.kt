package com.trailglass.backend

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class HealthRouteTest {
    @Test
    fun `health endpoint returns ok when headers present`() = testApplication {
        application { module() }

        val response = client.get("/api/v1/health") {
            header("X-Device-ID", "device-123")
            header("X-App-Version", "1.0.0")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("{\"status\":\"ok\"}", response.bodyAsText())
    }
}
