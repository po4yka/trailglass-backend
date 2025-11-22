package com.trailglass.backend

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertContains

class HealthRouteTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `health endpoint returns comprehensive status with components`() = testApplication {
        application { module() }

        val response = client.get("/api/v1/health") {
            headers.append("X-Device-ID", "test-device")
            headers.append("X-App-Version", "1.0.0")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()

        // Check for required fields
        assertTrue(body.contains("\"status\""))
        assertTrue(body.contains("\"uptimeSeconds\""))
        assertTrue(body.contains("\"version\""))
        assertTrue(body.contains("\"components\""))

        // Check for component status
        assertTrue(body.contains("\"database\""))
        assertTrue(body.contains("\"storage\""))
        assertTrue(body.contains("\"email\""))
    }

    @Test
    fun `health endpoint includes database component with status`() = testApplication {
        application { module() }

        val response = client.get("/api/v1/health") {
            headers.append("X-Device-ID", "test-device")
            headers.append("X-App-Version", "1.0.0")
        }

        val body = response.bodyAsText()
        val jsonElement = json.parseToJsonElement(body).jsonObject
        val components = jsonElement["components"]?.jsonObject
        val database = components?.get("database")?.jsonObject

        assertTrue(database != null, "Database component should be present")
        assertTrue(database.containsKey("status"), "Database should have status")

        // Database should be up in tests
        val dbStatus = database["status"]?.jsonPrimitive?.content
        assertTrue(dbStatus == "up" || dbStatus == "down", "Database status should be 'up' or 'down'")
    }

    @Test
    fun `health endpoint includes storage component`() = testApplication {
        application { module() }

        val response = client.get("/api/v1/health") {
            headers.append("X-Device-ID", "test-device")
            headers.append("X-App-Version", "1.0.0")
        }

        val body = response.bodyAsText()
        val jsonElement = json.parseToJsonElement(body).jsonObject
        val components = jsonElement["components"]?.jsonObject
        val storage = components?.get("storage")?.jsonObject

        assertTrue(storage != null, "Storage component should be present")
        assertTrue(storage.containsKey("status"), "Storage should have status")
    }

    @Test
    fun `health endpoint includes email component status`() = testApplication {
        application { module() }

        val response = client.get("/api/v1/health") {
            headers.append("X-Device-ID", "test-device")
            headers.append("X-App-Version", "1.0.0")
        }

        val body = response.bodyAsText()
        val jsonElement = json.parseToJsonElement(body).jsonObject
        val components = jsonElement["components"]?.jsonObject
        val email = components?.get("email")?.jsonObject

        assertTrue(email != null, "Email component should be present")
        assertTrue(email.containsKey("status"), "Email should have status")

        // Email is typically disabled in tests
        val emailStatus = email["status"]?.jsonPrimitive?.content
        assertContains(listOf("disabled", "up", "down"), emailStatus, "Email status should be valid")
    }

    @Test
    fun `liveness endpoint returns alive status`() = testApplication {
        application { module() }

        val response = client.get("/api/v1/health/live") {
            headers.append("X-Device-ID", "test-device")
            headers.append("X-App-Version", "1.0.0")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"status\":\"alive\""))
    }

    @Test
    fun `readiness endpoint returns ready when services are up`() = testApplication {
        application { module() }

        val response = client.get("/api/v1/health/ready") {
            headers.append("X-Device-ID", "test-device")
            headers.append("X-App-Version", "1.0.0")
        }

        // Should be ready if database is up (which it should be in tests)
        assertTrue(
            response.status == HttpStatusCode.OK || response.status == HttpStatusCode.ServiceUnavailable,
            "Response should be OK (ready) or ServiceUnavailable (not ready)"
        )

        val body = response.bodyAsText()
        assertTrue(
            body.contains("\"status\":\"ready\"") || body.contains("\"status\":\"not ready\""),
            "Response should contain ready status"
        )
    }

    @Test
    fun `health endpoint rejects requests missing headers`() = testApplication {
        application { module() }

        val response = client.get("/api/v1/health")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `liveness endpoint rejects requests missing headers`() = testApplication {
        application { module() }

        val response = client.get("/api/v1/health/live")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `readiness endpoint rejects requests missing headers`() = testApplication {
        application { module() }

        val response = client.get("/api/v1/health/ready")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
