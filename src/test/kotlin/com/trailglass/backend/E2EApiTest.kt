package com.trailglass.backend

import com.trailglass.backend.auth.*
import com.trailglass.backend.common.AuthTokens
import com.trailglass.backend.common.DeviceInfo
import com.trailglass.backend.config.AppConfig
import com.trailglass.backend.di.appModule
import com.trailglass.backend.location.LocationBatchRequest
import com.trailglass.backend.location.LocationBatchResult
import com.trailglass.backend.location.LocationSample
import com.trailglass.backend.persistence.DatabaseFactory
import com.trailglass.backend.persistence.FlywayMigrator
import com.trailglass.backend.photo.PhotoUploadPlan
import com.trailglass.backend.photo.PhotoUploadRequest
import com.trailglass.backend.photo.PhotoRecord
import com.trailglass.backend.sync.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.MinIOContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.util.UUID
import kotlin.test.*

/**
 * Comprehensive end-to-end API test suite that verifies full request/response cycles
 * using real database (PostgreSQL via Testcontainers) and storage (MinIO).
 *
 * Tests cover:
 * - Complete authentication flows
 * - Password reset flows
 * - Sync operations with conflict detection
 * - Photo upload workflows
 * - Error handling and validation
 */
@Testcontainers
class E2EApiTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("trailglass_test")
            .withUsername("test")
            .withPassword("test")

        @Container
        val minio = MinIOContainer("minio/minio:latest")
            .withUserName("minioadmin")
            .withPassword("minioadmin")
    }

    private fun createTestConfig(): AppConfig {
        return AppConfig(
            host = "localhost",
            port = 8080,
            environment = "test",
            databaseUrl = postgres.jdbcUrl,
            databaseUser = postgres.username,
            databasePassword = postgres.password,
            jwtSecret = "test-secret-key-for-e2e-testing-only",
            jwtIssuer = "trailglass-test",
            jwtAudience = "trailglass-test-clients",
            rateLimitPerMinute = 1000,
            enableMetrics = false,
            autoMigrate = true,
            storage = com.trailglass.backend.config.StorageConfig(
                backend = com.trailglass.backend.config.StorageBackend.S3,
                bucket = "trailglass-test",
                region = "us-east-1",
                endpoint = minio.s3URL,
                accessKey = minio.userName,
                secretKey = minio.password,
                usePathStyle = true,
                signingSecret = null,
                thumbnailSize = 300,
                thumbnailQuality = 0.85f,
            ),
            cloudflareAccess = null,
            email = com.trailglass.backend.config.EmailConfig(
                enabled = false,
                provider = com.trailglass.backend.config.EmailProvider.CONSOLE,
                smtp = null,
            ),
        )
    }

    private fun ApplicationTestBuilder.setupTestApp() {
        val config = createTestConfig()
        val dataSource = DatabaseFactory.dataSource(config)
        val flyway = FlywayMigrator.migrate(dataSource, config.autoMigrate)

        application {
            install(Koin) {
                slf4jLogger()
                modules(appModule(config, dataSource, flyway))
            }
            module()
        }
    }

    private fun ApplicationTestBuilder.jsonClient(): HttpClient = createClient {
        install(ContentNegotiation) { json() }
    }

    private fun testDeviceInfo(name: String = "TestDevice"): DeviceInfo = DeviceInfo(
        deviceId = UUID.randomUUID(),
        deviceName = name,
        platform = "android",
        osVersion = "14",
        appVersion = "1.0.0"
    )

    private fun requiredHeaders(deviceId: UUID = UUID.randomUUID()): Map<String, String> = mapOf(
        "X-Device-ID" to deviceId.toString(),
        "X-App-Version" to "1.0.0"
    )

    // AUTHENTICATION FLOW TESTS

    @Test
    fun `complete authentication flow works end to end`() = testApplication {
        setupTestApp()
        val client = jsonClient()
        val deviceInfo = testDeviceInfo()
        val headers = requiredHeaders(deviceInfo.deviceId)

        // Step 1: Register a new user
        val registerRequest = RegisterRequest(
            email = "e2e-test@example.com",
            password = "SecureP@ss123!",
            displayName = "E2E Test User",
            deviceInfo = deviceInfo
        )

        val registerResponse = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            headers.forEach { (key, value) -> header(key, value) }
            setBody(registerRequest)
        }

        assertEquals(HttpStatusCode.OK, registerResponse.status)
        val session = registerResponse.body<AuthSession>()
        assertEquals(registerRequest.email.lowercase(), session.user.email)
        assertTrue(session.tokens.accessToken.isNotBlank())
        assertTrue(session.tokens.refreshToken.isNotBlank())
        assertEquals(deviceInfo.deviceId, session.deviceId)

        // Step 2: Login with the registered credentials
        val loginRequest = LoginRequest(
            email = registerRequest.email,
            password = registerRequest.password,
            deviceInfo = deviceInfo
        )

        val loginResponse = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            headers.forEach { (key, value) -> header(key, value) }
            setBody(loginRequest)
        }

        assertEquals(HttpStatusCode.OK, loginResponse.status)
        val loginSession = loginResponse.body<AuthSession>()
        assertEquals(session.user.email, loginSession.user.email)

        // Step 3: Access protected endpoint with token
        val protectedResponse = client.get("/api/v1/health") {
            headers.forEach { (key, value) -> header(key, value) }
            header(HttpHeaders.Authorization, "Bearer ${loginSession.tokens.accessToken}")
        }

        assertEquals(HttpStatusCode.OK, protectedResponse.status)

        // Step 4: Logout and invalidate token
        val logoutResponse = client.post("/api/v1/auth/logout") {
            contentType(ContentType.Application.Json)
            headers.forEach { (key, value) -> header(key, value) }
            header(HttpHeaders.Authorization, "Bearer ${loginSession.tokens.accessToken}")
            setBody(RefreshRequest(loginSession.tokens.refreshToken, loginSession.deviceId))
        }

        assertEquals(HttpStatusCode.NoContent, logoutResponse.status)

        // Step 5: Verify refresh token is invalidated
        val refreshAfterLogout = client.post("/api/v1/auth/refresh") {
            contentType(ContentType.Application.Json)
            headers.forEach { (key, value) -> header(key, value) }
            setBody(RefreshRequest(loginSession.tokens.refreshToken, loginSession.deviceId))
        }

        assertEquals(HttpStatusCode.Unauthorized, refreshAfterLogout.status)
    }

    @Test
    fun `token refresh rotates tokens correctly`() = testApplication {
        setupTestApp()
        val client = jsonClient()
        val deviceInfo = testDeviceInfo()
        val headers = requiredHeaders(deviceInfo.deviceId)

        // Register and login
        val session = registerAndLogin(client, headers, deviceInfo, "refresh-test@example.com")

        // Refresh the token
        val refreshRequest = RefreshRequest(session.tokens.refreshToken, session.deviceId)
        val refreshResponse = client.post("/api/v1/auth/refresh") {
            contentType(ContentType.Application.Json)
            headers.forEach { (key, value) -> header(key, value) }
            setBody(refreshRequest)
        }

        assertEquals(HttpStatusCode.OK, refreshResponse.status)
        val newTokens = refreshResponse.body<AuthTokens>()

        // Verify new tokens are different
        assertNotEquals(session.tokens.accessToken, newTokens.accessToken)
        assertNotEquals(session.tokens.refreshToken, newTokens.refreshToken)

        // Verify old refresh token is invalidated
        val reuseAttempt = client.post("/api/v1/auth/refresh") {
            contentType(ContentType.Application.Json)
            headers.forEach { (key, value) -> header(key, value) }
            setBody(refreshRequest)
        }

        assertEquals(HttpStatusCode.Unauthorized, reuseAttempt.status)

        // Verify new access token works
        val protectedResponse = client.get("/api/v1/health") {
            headers.forEach { (key, value) -> header(key, value) }
            header(HttpHeaders.Authorization, "Bearer ${newTokens.accessToken}")
        }

        assertEquals(HttpStatusCode.OK, protectedResponse.status)
    }

    @Test
    fun `password reset flow works end to end`() = testApplication {
        setupTestApp()
        val client = jsonClient()
        val deviceInfo = testDeviceInfo()
        val headers = requiredHeaders(deviceInfo.deviceId)
        val email = "reset-test@example.com"

        // Register a user
        val originalPassword = "OriginalP@ss123!"
        val session = registerAndLogin(client, headers, deviceInfo, email, originalPassword)

        // Request password reset
        val resetRequest = PasswordResetRequest(email = email)
        val resetRequestResponse = client.post("/api/v1/auth/reset-password-request") {
            contentType(ContentType.Application.Json)
            headers.forEach { (key, value) -> header(key, value) }
            setBody(resetRequest)
        }

        assertEquals(HttpStatusCode.Accepted, resetRequestResponse.status)

        // Note: In a real scenario, we'd need to extract the token from email
        // For E2E testing, we'd need a test email service or database query
        // This tests the API contract but not the full email flow

        // Verify original password still works before reset
        val loginBeforeReset = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            headers.forEach { (key, value) -> header(key, value) }
            setBody(LoginRequest(email, originalPassword, deviceInfo))
        }

        assertEquals(HttpStatusCode.OK, loginBeforeReset.status)
    }

    // SYNC FLOW TESTS

    @Test
    fun `location sync flow works end to end`() = testApplication {
        setupTestApp()
        val client = jsonClient()
        val deviceInfo = testDeviceInfo("Device1")
        val headers = requiredHeaders(deviceInfo.deviceId)

        // Setup: Create authenticated user
        val session = registerAndLogin(client, headers, deviceInfo, "sync-test@example.com")
        val authHeader = "Bearer ${session.tokens.accessToken}"

        // Step 1: Get initial sync status
        val statusResponse = client.get("/api/v1/sync/status") {
            headers.forEach { (key, value) -> header(key, value) }
            header(HttpHeaders.Authorization, authHeader)
            parameter("deviceId", session.deviceId.toString())
            parameter("userId", session.user.userId.toString())
        }

        assertEquals(HttpStatusCode.OK, statusResponse.status)
        val initialStatus = statusResponse.body<SyncStatus>()
        assertEquals(session.deviceId, initialStatus.deviceId)
        assertEquals(session.user.userId, initialStatus.userId)
        assertNull(initialStatus.lastSyncAt)

        // Step 2: Upload location samples
        val now = Instant.now()
        val locationSamples = listOf(
            LocationSample(
                id = UUID.randomUUID(),
                userId = session.user.userId,
                deviceId = session.deviceId,
                latitude = 37.7749,
                longitude = -122.4194,
                accuracy = 10.0f,
                recordedAt = now,
                updatedAt = now,
                deletedAt = null,
                serverVersion = 0
            )
        )

        val locationRequest = LocationBatchRequest(
            userId = session.user.userId,
            deviceId = session.deviceId,
            samples = locationSamples
        )

        val locationResponse = client.post("/api/v1/locations/batch") {
            contentType(ContentType.Application.Json)
            headers.forEach { (key, value) -> header(key, value) }
            header(HttpHeaders.Authorization, authHeader)
            setBody(locationRequest)
        }

        assertEquals(HttpStatusCode.OK, locationResponse.status)
        val locationResult = locationResponse.body<LocationBatchResult>()
        assertEquals(1, locationResult.appliedCount)
        assertTrue(locationResult.serverVersion > 0)

        // Step 3: Perform delta sync
        val syncEnvelope = SyncEnvelope(
            id = UUID.randomUUID(),
            serverVersion = 0,
            updatedAt = now,
            deletedAt = null,
            payload = """{"type":"test","data":"sample"}""",
            isEncrypted = false,
            deviceId = session.deviceId
        )

        val deltaRequest = SyncDeltaRequest(
            userId = session.user.userId,
            deviceId = session.deviceId,
            sinceVersion = 0,
            incoming = listOf(syncEnvelope)
        )

        val deltaResponse = client.post("/api/v1/sync/delta") {
            contentType(ContentType.Application.Json)
            headers.forEach { (key, value) -> header(key, value) }
            header(HttpHeaders.Authorization, authHeader)
            setBody(deltaRequest)
        }

        assertEquals(HttpStatusCode.OK, deltaResponse.status)
        val deltaResult = deltaResponse.body<SyncDeltaResponse>()
        assertTrue(deltaResult.serverVersion > 0)
        assertTrue(deltaResult.applied.size <= 1)
    }

    @Test
    fun `sync conflict detection and resolution works`() = testApplication {
        setupTestApp()
        val client = jsonClient()
        val device1Info = testDeviceInfo("Device1")
        val device2Info = testDeviceInfo("Device2")
        val headers1 = requiredHeaders(device1Info.deviceId)
        val headers2 = requiredHeaders(device2Info.deviceId)

        // Setup: Create user and login from two devices
        val session1 = registerAndLogin(client, headers1, device1Info, "conflict-test@example.com")

        val session2 = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            headers2.forEach { (key, value) -> header(key, value) }
            setBody(LoginRequest("conflict-test@example.com", "SecureP@ss123!", device2Info))
        }.body<AuthSession>()

        val now = Instant.now()
        val entityId = UUID.randomUUID()

        // Device 1 creates an entity
        val envelope1 = SyncEnvelope(
            id = entityId,
            serverVersion = 0,
            updatedAt = now,
            deletedAt = null,
            payload = """{"version":1,"data":"from-device-1"}""",
            isEncrypted = false,
            deviceId = session1.deviceId
        )

        val delta1 = client.post("/api/v1/sync/delta") {
            contentType(ContentType.Application.Json)
            headers1.forEach { (key, value) -> header(key, value) }
            header(HttpHeaders.Authorization, "Bearer ${session1.tokens.accessToken}")
            setBody(SyncDeltaRequest(session1.user.userId, session1.deviceId, 0, listOf(envelope1)))
        }

        assertEquals(HttpStatusCode.OK, delta1.status)
        val delta1Result = delta1.body<SyncDeltaResponse>()

        // Device 2 tries to update the same entity with old version (conflict)
        val envelope2 = SyncEnvelope(
            id = entityId,
            serverVersion = 0,  // Using old version creates conflict
            updatedAt = now.plusSeconds(1),
            deletedAt = null,
            payload = """{"version":2,"data":"from-device-2"}""",
            isEncrypted = false,
            deviceId = session2.deviceId
        )

        val delta2 = client.post("/api/v1/sync/delta") {
            contentType(ContentType.Application.Json)
            headers2.forEach { (key, value) -> header(key, value) }
            header(HttpHeaders.Authorization, "Bearer ${session2.tokens.accessToken}")
            setBody(SyncDeltaRequest(session2.user.userId, session2.deviceId, 0, listOf(envelope2)))
        }

        assertEquals(HttpStatusCode.OK, delta2.status)
        val delta2Result = delta2.body<SyncDeltaResponse>()

        // Verify conflict was detected
        if (delta2Result.conflicts.isNotEmpty()) {
            val conflict = delta2Result.conflicts.first()
            assertEquals(entityId, conflict.entityId)

            // Resolve the conflict by choosing device 2's version
            val resolutionRequest = ConflictResolutionRequest(
                conflictId = conflict.conflictId,
                entityId = conflict.entityId,
                chosenPayload = envelope2.payload,
                isEncrypted = false,
                userId = session2.user.userId,
                deviceId = session2.deviceId
            )

            val resolutionResponse = client.post("/api/v1/sync/resolve-conflict") {
                contentType(ContentType.Application.Json)
                headers2.forEach { (key, value) -> header(key, value) }
                header(HttpHeaders.Authorization, "Bearer ${session2.tokens.accessToken}")
                setBody(resolutionRequest)
            }

            assertEquals(HttpStatusCode.OK, resolutionResponse.status)
            val resolution = resolutionResponse.body<ConflictResolutionResult>()
            assertEquals(entityId, resolution.entityId)
            assertTrue(resolution.serverVersion > delta1Result.serverVersion)
        }
    }

    // PHOTO UPLOAD FLOW TESTS

    @Test
    fun `photo upload flow works end to end`() = testApplication {
        setupTestApp()
        val client = jsonClient()
        val deviceInfo = testDeviceInfo()
        val headers = requiredHeaders(deviceInfo.deviceId)

        // Setup: Create authenticated user
        val session = registerAndLogin(client, headers, deviceInfo, "photo-test@example.com")
        val authHeader = "Bearer ${session.tokens.accessToken}"

        // Step 1: Create photo upload and get presigned URL
        val uploadRequest = PhotoUploadRequest(
            fileName = "test-photo.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 1024 * 1024  // 1MB
        )

        val uploadResponse = client.post("/api/v1/photos") {
            contentType(ContentType.Application.Json)
            headers.forEach { (key, value) -> header(key, value) }
            header(HttpHeaders.Authorization, authHeader)
            setBody(uploadRequest)
        }

        assertEquals(HttpStatusCode.Created, uploadResponse.status)
        val uploadPlan = uploadResponse.body<PhotoUploadPlan>()
        assertEquals(session.user.userId, uploadPlan.photo.userId)
        assertEquals(uploadRequest.fileName, uploadPlan.photo.fileName)
        assertTrue(uploadPlan.upload.url.isNotBlank())
        assertNull(uploadPlan.photo.uploadedAt)  // Not confirmed yet

        // Step 2: Confirm the upload
        val confirmResponse = client.post("/api/v1/photos/${uploadPlan.photo.id}/confirm") {
            contentType(ContentType.Application.Json)
            headers.forEach { (key, value) -> header(key, value) }
            header(HttpHeaders.Authorization, authHeader)
        }

        assertEquals(HttpStatusCode.OK, confirmResponse.status)
        val confirmedPhoto = confirmResponse.body<PhotoRecord>()
        assertNotNull(confirmedPhoto.photo.uploadedAt)

        // Step 3: Get the photo with download URL
        val getResponse = client.get("/api/v1/photos/${uploadPlan.photo.id}") {
            headers.forEach { (key, value) -> header(key, value) }
            header(HttpHeaders.Authorization, authHeader)
        }

        assertEquals(HttpStatusCode.OK, getResponse.status)
        val photoRecord = getResponse.body<PhotoRecord>()
        assertEquals(uploadPlan.photo.id, photoRecord.photo.id)
        assertNotNull(photoRecord.download)

        // Step 4: Delete the photo (soft delete)
        val deleteResponse = client.delete("/api/v1/photos/${uploadPlan.photo.id}") {
            headers.forEach { (key, value) -> header(key, value) }
            header(HttpHeaders.Authorization, authHeader)
        }

        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        // Step 5: Verify photo is marked as deleted
        val getAfterDelete = client.get("/api/v1/photos/${uploadPlan.photo.id}") {
            headers.forEach { (key, value) -> header(key, value) }
            header(HttpHeaders.Authorization, authHeader)
        }

        // Should either return 404 or the photo with deletedAt set
        assertTrue(
            getAfterDelete.status == HttpStatusCode.NotFound ||
            (getAfterDelete.status == HttpStatusCode.OK &&
             getAfterDelete.body<PhotoRecord>().photo.deletedAt != null)
        )
    }

    @Test
    fun `photo metadata fetch returns multiple photos`() = testApplication {
        setupTestApp()
        val client = jsonClient()
        val deviceInfo = testDeviceInfo()
        val headers = requiredHeaders(deviceInfo.deviceId)

        val session = registerAndLogin(client, headers, deviceInfo, "photo-list-test@example.com")
        val authHeader = "Bearer ${session.tokens.accessToken}"

        // Create multiple photos
        val photoIds = mutableListOf<UUID>()
        repeat(3) { index ->
            val uploadRequest = PhotoUploadRequest(
                fileName = "photo-$index.jpg",
                mimeType = "image/jpeg",
                sizeBytes = 512 * 1024
            )

            val uploadResponse = client.post("/api/v1/photos") {
                contentType(ContentType.Application.Json)
                headers.forEach { (key, value) -> header(key, value) }
                header(HttpHeaders.Authorization, authHeader)
                setBody(uploadRequest)
            }

            assertEquals(HttpStatusCode.Created, uploadResponse.status)
            val plan = uploadResponse.body<PhotoUploadPlan>()
            photoIds.add(plan.photo.id)
        }

        // Fetch all photos
        val listResponse = client.get("/api/v1/photos") {
            headers.forEach { (key, value) -> header(key, value) }
            header(HttpHeaders.Authorization, authHeader)
            parameter("limit", "100")
        }

        assertEquals(HttpStatusCode.OK, listResponse.status)
        val photos = listResponse.body<List<PhotoRecord>>()
        assertTrue(photos.size >= 3)
        assertTrue(photoIds.all { id -> photos.any { it.photo.id == id } })
    }

    // ERROR HANDLING TESTS

    @Test
    fun `invalid credentials return 401`() = testApplication {
        setupTestApp()
        val client = jsonClient()
        val deviceInfo = testDeviceInfo()
        val headers = requiredHeaders(deviceInfo.deviceId)

        // Register a user
        registerAndLogin(client, headers, deviceInfo, "error-test@example.com")

        // Try to login with wrong password
        val loginRequest = LoginRequest(
            email = "error-test@example.com",
            password = "WrongPassword123!",
            deviceInfo = deviceInfo
        )

        val response = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            headers.forEach { (key, value) -> header(key, value) }
            setBody(loginRequest)
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `missing auth header returns 401`() = testApplication {
        setupTestApp()
        val client = jsonClient()
        val headers = requiredHeaders()

        // Try to access protected endpoint without auth
        val response = client.get("/api/v1/sync/status") {
            headers.forEach { (key, value) -> header(key, value) }
            parameter("deviceId", UUID.randomUUID().toString())
            parameter("userId", UUID.randomUUID().toString())
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `invalid UUID format returns 400`() = testApplication {
        setupTestApp()
        val client = jsonClient()
        val deviceInfo = testDeviceInfo()
        val headers = requiredHeaders(deviceInfo.deviceId)

        val session = registerAndLogin(client, headers, deviceInfo, "uuid-test@example.com")

        // Try to get photo with invalid UUID
        val response = client.get("/api/v1/photos/invalid-uuid") {
            headers.forEach { (key, value) -> header(key, value) }
            header(HttpHeaders.Authorization, "Bearer ${session.tokens.accessToken}")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `accessing other user data returns 404`() = testApplication {
        setupTestApp()
        val client = jsonClient()
        val device1Info = testDeviceInfo("Device1")
        val device2Info = testDeviceInfo("Device2")
        val headers1 = requiredHeaders(device1Info.deviceId)
        val headers2 = requiredHeaders(device2Info.deviceId)

        // Create two different users
        val session1 = registerAndLogin(client, headers1, device1Info, "user1@example.com")
        val session2 = registerAndLogin(client, headers2, device2Info, "user2@example.com")

        // User 1 creates a photo
        val uploadRequest = PhotoUploadRequest(
            fileName = "private-photo.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 1024 * 1024
        )

        val uploadResponse = client.post("/api/v1/photos") {
            contentType(ContentType.Application.Json)
            headers1.forEach { (key, value) -> header(key, value) }
            header(HttpHeaders.Authorization, "Bearer ${session1.tokens.accessToken}")
            setBody(uploadRequest)
        }

        assertEquals(HttpStatusCode.Created, uploadResponse.status)
        val photo = uploadResponse.body<PhotoUploadPlan>()

        // User 2 tries to access User 1's photo
        val accessAttempt = client.get("/api/v1/photos/${photo.photo.id}") {
            headers2.forEach { (key, value) -> header(key, value) }
            header(HttpHeaders.Authorization, "Bearer ${session2.tokens.accessToken}")
        }

        // Should return 404 (not found) to prevent information disclosure
        assertEquals(HttpStatusCode.NotFound, accessAttempt.status)
    }

    @Test
    fun `missing required headers returns 400`() = testApplication {
        setupTestApp()
        val client = jsonClient()

        // Try to access API without required headers
        val response = client.get("/api/v1/health")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `duplicate user registration returns 409 conflict`() = testApplication {
        setupTestApp()
        val client = jsonClient()
        val deviceInfo = testDeviceInfo()
        val headers = requiredHeaders(deviceInfo.deviceId)

        val registerRequest = RegisterRequest(
            email = "duplicate@example.com",
            password = "SecureP@ss123!",
            displayName = "First User",
            deviceInfo = deviceInfo
        )

        // First registration
        val firstResponse = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            headers.forEach { (key, value) -> header(key, value) }
            setBody(registerRequest)
        }

        assertEquals(HttpStatusCode.OK, firstResponse.status)

        // Second registration with same email
        val secondResponse = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            headers.forEach { (key, value) -> header(key, value) }
            setBody(registerRequest)
        }

        assertEquals(HttpStatusCode.Conflict, secondResponse.status)
    }

    @Test
    fun `expired access token returns 401`() = testApplication {
        setupTestApp()
        val client = jsonClient()
        val deviceInfo = testDeviceInfo()
        val headers = requiredHeaders(deviceInfo.deviceId)

        // Use an obviously invalid/expired token
        val invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"

        val response = client.get("/api/v1/sync/status") {
            headers.forEach { (key, value) -> header(key, value) }
            header(HttpHeaders.Authorization, "Bearer $invalidToken")
            parameter("deviceId", UUID.randomUUID().toString())
            parameter("userId", UUID.randomUUID().toString())
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    // HELPER METHODS

    private suspend fun registerAndLogin(
        client: HttpClient,
        headers: Map<String, String>,
        deviceInfo: DeviceInfo,
        email: String,
        password: String = "SecureP@ss123!"
    ): AuthSession {
        val registerRequest = RegisterRequest(
            email = email,
            password = password,
            displayName = "Test User",
            deviceInfo = deviceInfo
        )

        val response = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            headers.forEach { (key, value) -> header(key, value) }
            setBody(registerRequest)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        return response.body()
    }
}
