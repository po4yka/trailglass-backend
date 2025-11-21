package com.trailglass.backend

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.forwardedheaders.ForwardedHeaders
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.request.origin
import io.ktor.server.request.userAgent
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.request.path
import kotlinx.serialization.Serializable
import org.flywaydb.core.Flyway
import org.slf4j.event.Level
import java.time.Duration
import java.util.UUID

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port) {
        trailglassModule()
    }.start(wait = true)
}

data class DatabaseConfig(
    val host: String,
    val port: Int,
    val name: String,
    val user: String,
    val password: String,
    val sslMode: String,
    val maximumPoolSize: Int,
)

data class MetricsConfig(
    val enabled: Boolean,
    val path: String,
    val cloudflareAccessClientId: String?,
    val cloudflareAccessClientSecret: String?,
)

data class AppConfig(
    val database: DatabaseConfig,
    val enforceHttps: Boolean,
    val metrics: MetricsConfig,
) {
    companion object {
        fun fromEnv(): AppConfig {
            val env = System.getenv()
            val dbPort = env["DATABASE_PORT"]?.toIntOrNull() ?: 5432
            val dbPool = env["DATABASE_MAX_POOL_SIZE"]?.toIntOrNull() ?: 5
            val metricsEnabled = env["METRICS_ENABLED"]?.lowercase() == "true"
            val metricsPath = env["METRICS_PATH"] ?: "/metrics"

            return AppConfig(
                database = DatabaseConfig(
                    host = env["DATABASE_HOST"] ?: "postgres",
                    port = dbPort,
                    name = env["DATABASE_NAME"] ?: "trailglass",
                    user = env["DATABASE_USER"] ?: env["POSTGRES_USER"] ?: "postgres",
                    password = env["DATABASE_PASSWORD"] ?: env["POSTGRES_PASSWORD"] ?: "postgres",
                    sslMode = env["DATABASE_SSL_MODE"] ?: "prefer",
                    maximumPoolSize = dbPool,
                ),
                enforceHttps = env["ALLOW_PLAIN_HTTP"]?.lowercase() != "true",
                metrics = MetricsConfig(
                    enabled = metricsEnabled,
                    path = metricsPath,
                    cloudflareAccessClientId = env["CLOUDFLARE_ACCESS_CLIENT_ID"],
                    cloudflareAccessClientSecret = env["CLOUDFLARE_ACCESS_CLIENT_SECRET"],
                ),
            )
        }
    }
}

private fun createDataSource(config: DatabaseConfig): HikariDataSource {
    val jdbcUrl = "jdbc:postgresql://${config.host}:${config.port}/${config.name}?sslmode=${config.sslMode}"
    val hikariConfig = HikariConfig().apply {
        this.jdbcUrl = jdbcUrl
        username = config.user
        password = config.password
        maximumPoolSize = config.maximumPoolSize
        minimumIdle = 1
        idleTimeout = Duration.ofMinutes(10).toMillis()
        connectionTimeout = Duration.ofSeconds(10).toMillis()
        driverClassName = "org.postgresql.Driver"
    }
    return HikariDataSource(hikariConfig)
}

private fun runFlywayMigrations(dataSource: HikariDataSource) {
    val flyway = Flyway.configure()
        .dataSource(dataSource)
        .locations("filesystem:db/migration")
        .load()
    flyway.migrate()
}

private fun createMetricsRegistry(enabled: Boolean): Pair<CompositeMeterRegistry, PrometheusMeterRegistry?> {
    if (!enabled) {
        return CompositeMeterRegistry(Clock.SYSTEM) to null
    }
    val prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val composite = CompositeMeterRegistry(Clock.SYSTEM).apply { add(prometheusRegistry) }
    return composite to prometheusRegistry
}

private fun Application.installHttpsEnforcement(enforceHttps: Boolean) {
    if (!enforceHttps) return

    intercept(ApplicationCallPipeline.Plugins) {
        val scheme = call.request.origin.scheme
        if (scheme != "https") {
            call.respond(
                HttpStatusCode.Forbidden,
                ErrorResponse(
                    code = "insecure_request",
                    message = "HTTPS is required behind the proxy",
                ),
            )
            finish()
        }
    }
}

private fun Application.configureRouting(
    dataSource: HikariDataSource,
    metricsConfig: MetricsConfig,
    prometheusRegistry: PrometheusMeterRegistry?,
) {
    routing {
        get("/health") {
            try {
                dataSource.connection.use { connection ->
                    connection.createStatement().use { statement ->
                        statement.execute("SELECT 1")
                    }
                }
                call.respondText("OK")
            } catch (ex: Exception) {
                call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    ErrorResponse(code = "db_unavailable", message = ex.message ?: "Database unavailable"),
                )
            }
        }

        if (metricsConfig.enabled && prometheusRegistry != null) {
            get(metricsConfig.path) {
                val clientId = metricsConfig.cloudflareAccessClientId
                val clientSecret = metricsConfig.cloudflareAccessClientSecret
                if (!clientId.isNullOrBlank() && !clientSecret.isNullOrBlank()) {
                    val headerId = call.request.headers["CF-Access-Client-Id"]
                    val headerSecret = call.request.headers["CF-Access-Client-Secret"]
                    if (headerId != clientId || headerSecret != clientSecret) {
                        call.respond(HttpStatusCode.Unauthorized, "Cloudflare Access service token required")
                        return@get
                    }
                }
                call.respondText(prometheusRegistry.scrape())
            }
        }
    }
}

fun Application.trailglassModule() {
    install(ForwardedHeaders)
    install(XForwardedHeaders)
    install(ContentNegotiation) { json() }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            environment.log.error("Unhandled error", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("internal_error", "Unexpected server error"))
        }
    }
    install(CallLogging) {
        level = Level.INFO
        format { call -> "${call.request.httpMethod.value} ${call.request.path()} ua=${call.request.userAgent()}" }
    }

    val config = AppConfig.fromEnv()
    val dataSource = createDataSource(config.database)
    environment.monitor.subscribe(ApplicationStopped) {
        dataSource.close()
    }

    runFlywayMigrations(dataSource)

    val (meterRegistry, prometheusRegistry) = createMetricsRegistry(config.metrics.enabled)
    install(MicrometerMetrics) {
        registry = meterRegistry
        timersAsPercentiles = true
    }

    installHttpsEnforcement(config.enforceHttps)
    configureRouting(dataSource, config.metrics, prometheusRegistry)
}

@Serializable
data class ErrorResponse(
    val code: String,
    val message: String,
    val requestId: String = UUID.randomUUID().toString(),
)
