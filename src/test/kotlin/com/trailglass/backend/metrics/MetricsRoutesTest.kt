package com.trailglass.backend.metrics

import com.trailglass.backend.config.AppConfig
import com.trailglass.backend.config.CloudflareAccessConfig
import com.trailglass.backend.config.EmailConfig
import com.trailglass.backend.config.EmailProvider
import com.trailglass.backend.config.StorageBackend
import com.trailglass.backend.config.StorageConfig
import com.trailglass.backend.di.metricsModule
import com.trailglass.backend.metrics.metricsRoutes
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.micrometer.core.instrument.MeterRegistry
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MetricsRoutesTest {

    private fun createTestConfig(
        enableMetrics: Boolean = true,
        cloudflareAccess: CloudflareAccessConfig? = null
    ) = AppConfig(
        host = "0.0.0.0",
        port = 8080,
        environment = "test",
        databaseUrl = "jdbc:postgresql://localhost:5432/test",
        databaseUser = "test",
        databasePassword = "test",
        jwtSecret = "test-secret",
        jwtIssuer = "test-issuer",
        jwtAudience = "test-audience",
        rateLimitPerMinute = 100,
        enableMetrics = enableMetrics,
        autoMigrate = false,
        storage = StorageConfig(
            backend = StorageBackend.DATABASE,
            bucket = null,
            region = null,
            endpoint = null,
            accessKey = null,
            secretKey = null,
            usePathStyle = false,
            signingSecret = null,
            thumbnailSize = 300,
            thumbnailQuality = 0.85f,
        ),
        cloudflareAccess = cloudflareAccess,
        email = EmailConfig(
            enabled = false,
            provider = EmailProvider.CONSOLE,
            smtp = null,
        ),
    )

    @Test
    fun `metrics endpoint returns Prometheus metrics when enabled`() = testApplication {
        val config = createTestConfig(enableMetrics = true)

        application {
            install(Koin) {
                modules(
                    module {
                        single { config }
                    } + metricsModule
                )
            }
            routing {
                metricsRoutes()
            }
        }

        val response = client.get("/metrics")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("# HELP"), "Response should contain Prometheus metrics")
    }

    @Test
    fun `metrics endpoint does not register when metrics disabled`() = testApplication {
        val config = createTestConfig(enableMetrics = false)

        application {
            install(Koin) {
                modules(
                    module {
                        single { config }
                    } + metricsModule
                )
            }
            routing {
                metricsRoutes()
            }
        }

        val response = client.get("/metrics")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `metrics endpoint requires Cloudflare Access auth when configured`() = testApplication {
        val cloudflareAccess = CloudflareAccessConfig(
            clientId = "test-client-id",
            clientSecret = "test-client-secret"
        )
        val config = createTestConfig(enableMetrics = true, cloudflareAccess = cloudflareAccess)

        application {
            install(Koin) {
                modules(
                    module {
                        single { config }
                    } + metricsModule
                )
            }
            routing {
                metricsRoutes()
            }
        }

        // Without auth headers
        val unauthorizedResponse = client.get("/metrics")
        assertEquals(HttpStatusCode.Unauthorized, unauthorizedResponse.status)

        // With correct auth headers
        val authorizedResponse = client.get("/metrics") {
            headers {
                append("CF-Access-Client-Id", "test-client-id")
                append("CF-Access-Client-Secret", "test-client-secret")
            }
        }
        assertEquals(HttpStatusCode.OK, authorizedResponse.status)

        // With incorrect auth headers
        val wrongAuthResponse = client.get("/metrics") {
            headers {
                append("CF-Access-Client-Id", "wrong-id")
                append("CF-Access-Client-Secret", "wrong-secret")
            }
        }
        assertEquals(HttpStatusCode.Unauthorized, wrongAuthResponse.status)
    }

    @Test
    fun `metrics endpoint works without Cloudflare Access when not configured`() = testApplication {
        val config = createTestConfig(enableMetrics = true, cloudflareAccess = null)

        application {
            install(Koin) {
                modules(
                    module {
                        single { config }
                    } + metricsModule
                )
            }
            routing {
                metricsRoutes()
            }
        }

        val response = client.get("/metrics")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("# HELP"), "Response should contain Prometheus metrics")
    }
}
