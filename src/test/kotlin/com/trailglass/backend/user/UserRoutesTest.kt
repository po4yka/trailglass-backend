package com.trailglass.backend.user

import com.trailglass.backend.config.AppConfig
import com.trailglass.backend.plugins.configureSerialization
import com.trailglass.backend.plugins.configureStatusHandling
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UserRoutesTest {

    @Test
    fun `GET profile returns user profile when exists`() = testApplication {
        val userId = UUID.randomUUID()
        val profile = UserProfile(
            id = userId,
            email = "test@example.com",
            displayName = "Test User",
            updatedAt = Instant.now(),
            deletedAt = null,
            serverVersion = 123
        )

        val mockService = mockk<UserProfileService> {
            coEvery { getProfile(userId) } returns profile
        }

        application { configureTestApp(mockService) }

        val client = jsonClient()
        val response = client.get("/api/v1/user/profile?userId=$userId")

        assertEquals(HttpStatusCode.OK, response.status)
        val profileResponse = response.body<UserProfileResponse>()
        assertEquals(userId, profileResponse.userId)
        assertEquals("test@example.com", profileResponse.email)
        assertEquals("Test User", profileResponse.displayName)
        assertNotNull(profileResponse.statistics)
    }

    @Test
    fun `GET profile returns 404 when profile not found`() = testApplication {
        val userId = UUID.randomUUID()

        val mockService = mockk<UserProfileService> {
            coEvery { getProfile(userId) } returns null
        }

        application { configureTestApp(mockService) }

        val client = jsonClient()
        val response = client.get("/api/v1/user/profile?userId=$userId")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET profile returns 400 when userId is missing`() = testApplication {
        val mockService = mockk<UserProfileService>()

        application { configureTestApp(mockService) }

        val client = jsonClient()
        val response = client.get("/api/v1/user/profile")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    private fun Application.configureTestApp(userProfileService: UserProfileService) {
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
                    single { userProfileService }
                }
            )
        }

        configureSerialization()
        configureStatusHandling()
        routing {
            route("/api/v1/user") {
                userProfileRoutes()
            }
        }
    }

    private fun ApplicationTestBuilder.jsonClient() = createClient {
        install(ContentNegotiation) { json() }
    }
}
