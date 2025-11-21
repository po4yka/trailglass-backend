package com.trailglass.backend

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HealthRouteTest {
    @Test
    fun `returns ok when required headers are present`() = testApplication {
        application { module() }

        val response = client.get("/api/v1/health") {
            headers.append("X-Device-ID", "test-device")
            headers.append("X-App-Version", "1.0.0")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"status\":\"ok\""))
    }

    @Test
    fun `rejects requests missing headers`() = testApplication {
        application { module() }

        val response = client.get("/api/v1/health")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
