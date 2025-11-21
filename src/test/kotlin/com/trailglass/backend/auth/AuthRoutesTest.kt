package com.trailglass.backend.auth

import com.trailglass.backend.common.AuthTokens
import com.trailglass.backend.common.DeviceInfo
import com.trailglass.backend.config.AppConfig
import com.trailglass.backend.plugins.AuthJwt
import com.trailglass.backend.plugins.configureAuthentication
import com.trailglass.backend.plugins.configureSerialization
import com.trailglass.backend.plugins.configureStatusHandling
import io.ktor.client.call.body
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.server.testing.ApplicationTestBuilder
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AuthRoutesTest {

    @Test
    fun `register creates user and returns tokens`() = testApplication {
        application { configureTestApp() }

        val request = RegisterRequest(
            email = "user@example.com",
            password = "P@ssw0rd!",
            displayName = "User",
            deviceInfo = deviceInfo(),
        )

        val client = jsonClient()

        val response = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val session = response.body<AuthSession>()
        assertEquals(request.email.lowercase(), session.user.email)
        assertTrue(session.tokens.accessToken.isNotBlank())
        assertEquals(request.deviceInfo.deviceId, session.deviceId)
    }

    @Test
    fun `duplicate register returns conflict`() = testApplication {
        application { configureTestApp() }

        val request = RegisterRequest(
            email = "dupe@example.com",
            password = "P@ssw0rd!",
            displayName = "User",
            deviceInfo = deviceInfo(),
        )

        val client = jsonClient()

        client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        val duplicate = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        assertEquals(HttpStatusCode.Conflict, duplicate.status)
    }

    @Test
    fun `login rejects invalid credentials`() = testApplication {
        application { configureTestApp() }

        val request = RegisterRequest(
            email = "login@example.com",
            password = "P@ssw0rd!",
            displayName = "User",
            deviceInfo = deviceInfo(),
        )

        val client = jsonClient()

        client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        val login = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request.copy(password = "wrong"))
        }

        assertEquals(HttpStatusCode.Unauthorized, login.status)
    }

    @Test
    fun `refresh rotates tokens and invalidates old refresh token`() = testApplication {
        application { configureTestApp() }

        val client = jsonClient()

        val register = registerUser(client)
        val refreshRequest = RefreshRequest(register.tokens.refreshToken, register.deviceId)

        val refreshResponse = client.post("/api/v1/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(refreshRequest)
        }

        assertEquals(HttpStatusCode.OK, refreshResponse.status)
        val refreshedTokens = refreshResponse.body<AuthTokens>()
        assertNotEquals(register.tokens.refreshToken, refreshedTokens.refreshToken)

        val reuse = client.post("/api/v1/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(refreshRequest)
        }

        assertEquals(HttpStatusCode.Unauthorized, reuse.status)
    }

    @Test
    fun `refresh rejects expired tokens`() = testApplication {
        application { configureTestApp(refreshTtlSeconds = -60) }

        val client = jsonClient()

        val register = registerUser(client)
        val refreshRequest = RefreshRequest(register.tokens.refreshToken, register.deviceId)

        val refreshResponse = client.post("/api/v1/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(refreshRequest)
        }

        assertEquals(HttpStatusCode.Unauthorized, refreshResponse.status)
    }

    @Test
    fun `logout invalidates stored refresh token`() = testApplication {
        application { configureTestApp() }

        val client = jsonClient()

        val register = registerUser(client)
        val logoutResponse = client.post("/api/v1/auth/logout") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${register.tokens.accessToken}")
            setBody(RefreshRequest(register.tokens.refreshToken, register.deviceId))
        }

        assertEquals(HttpStatusCode.NoContent, logoutResponse.status)

        val refreshResponse = client.post("/api/v1/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(register.tokens.refreshToken, register.deviceId))
        }

        assertEquals(HttpStatusCode.Unauthorized, refreshResponse.status)
    }

    @Test
    fun `refresh enforces device binding`() = testApplication {
        application { configureTestApp() }

        val client = jsonClient()

        val register = registerUser(client)
        val refreshResponse = client.post("/api/v1/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(register.tokens.refreshToken, UUID.randomUUID()))
        }

        assertEquals(HttpStatusCode.Unauthorized, refreshResponse.status)
    }

    @Test
    fun `authentication middleware rejects refresh tokens`() = testApplication {
        application {
            configureTestApp()
            routing {
                authenticate(AuthJwt) {
                    get("/api/v1/protected") { call.respondText("ok") }
                }
            }
        }

        val client = jsonClient()

        val register = registerUser(client)

        val refreshAttempt = client.get("/api/v1/protected") {
            header(HttpHeaders.Authorization, "Bearer ${register.tokens.refreshToken}")
        }
        assertEquals(HttpStatusCode.Unauthorized, refreshAttempt.status)

        val accessAttempt = client.get("/api/v1/protected") {
            header(HttpHeaders.Authorization, "Bearer ${register.tokens.accessToken}")
        }
        assertEquals(HttpStatusCode.OK, accessAttempt.status)
    }

    private fun Application.configureTestApp(refreshTtlSeconds: Long = JwtProvider.DEFAULT_REFRESH_TOKEN_EXPIRY_SECONDS) {
        val config = AppConfig(
            host = "localhost",
            port = 8080,
            environment = "test",
            databaseUrl = null,
            databaseUser = null,
            databasePassword = null,
            jwtSecret = "test-secret",
            jwtIssuer = "trailglass",
            jwtAudience = "trailglass-clients",
            rateLimitPerMinute = 120,
            enableMetrics = false,
        )

        install(Koin) {
            modules(
                module {
                    single { config }
                    single { JwtProvider(get()) }
                    single<AuthService> { DefaultAuthService(get(), refreshTokenTtlSeconds = refreshTtlSeconds) }
                }
            )
        }

        configureSerialization()
        configureStatusHandling()
        configureAuthentication()
        routing {
            route("/api/v1") { authRoutes() }
        }
    }

    private fun ApplicationTestBuilder.jsonClient() = createClient {
        install(ContentNegotiation) { json() }
    }

    private fun deviceInfo(): DeviceInfo = DeviceInfo(
        deviceId = UUID.randomUUID(),
        deviceName = "Pixel",
        platform = "android",
        osVersion = "14",
        appVersion = "1.0.0"
    )

    private suspend fun registerUser(client: HttpClient): AuthSession {
        val request = RegisterRequest(
            email = "registered@example.com",
            password = "P@ssw0rd!",
            displayName = "Registered",
            deviceInfo = deviceInfo(),
        )

        val response = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        return response.body()
    }
}
