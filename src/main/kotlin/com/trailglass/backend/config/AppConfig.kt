package com.trailglass.backend.config

import kotlin.system.exitProcess

data class AppConfig(
    val host: String,
    val port: Int,
    val environment: String,
    val databaseUrl: String?,
    val databaseUser: String?,
    val databasePassword: String?,
    val jwtSecret: String?,
    val jwtIssuer: String,
    val jwtAudience: String,
    val rateLimitPerMinute: Long,
    val enableMetrics: Boolean,
)

object ConfigLoader {
    private const val DEFAULT_PORT = 8080
    private const val DEFAULT_HOST = "0.0.0.0"
    private const val DEFAULT_ENVIRONMENT = "development"
    private const val DEFAULT_JWT_ISSUER = "trailglass"
    private const val DEFAULT_JWT_AUDIENCE = "trailglass-clients"
    private const val DEFAULT_JWT_SECRET = "dev-secret-change-me"
    private const val DEFAULT_RATE_LIMIT_PER_MINUTE = 120L

    fun fromEnv(): AppConfig {
        val port = env("APP_PORT")?.toIntOrNull() ?: DEFAULT_PORT
        val host = env("APP_HOST") ?: DEFAULT_HOST
        val environment = env("APP_ENVIRONMENT") ?: DEFAULT_ENVIRONMENT
        val jwtIssuer = env("JWT_ISSUER") ?: DEFAULT_JWT_ISSUER
        val jwtAudience = env("JWT_AUDIENCE") ?: DEFAULT_JWT_AUDIENCE
        val rateLimit = env("GLOBAL_RATE_LIMIT_PER_MINUTE")?.toLongOrNull()
            ?: DEFAULT_RATE_LIMIT_PER_MINUTE

        val enableMetrics = env("ENABLE_METRICS")?.toBooleanStrictOrNull() ?: false

        val config = AppConfig(
            host = host,
            port = port,
            environment = environment,
            databaseUrl = env("DATABASE_URL"),
            databaseUser = env("DATABASE_USER"),
            databasePassword = env("DATABASE_PASSWORD"),
            jwtSecret = env("JWT_SECRET") ?: defaultSecret(environment),
            jwtIssuer = jwtIssuer,
            jwtAudience = jwtAudience,
            rateLimitPerMinute = rateLimit,
            enableMetrics = enableMetrics,
        )

        validate(config)
        return config
    }

    private fun env(key: String): String? = System.getenv(key)?.takeIf { it.isNotBlank() }

    private fun validate(config: AppConfig) {
        if (config.environment.lowercase() == "production") {
            if (config.jwtSecret.isNullOrBlank() || config.jwtSecret == DEFAULT_JWT_SECRET) {
                println("[fatal] JWT_SECRET is required in production environments")
                exitProcess(1)
            }
        }

        if (config.databaseUrl.isNullOrBlank()) {
            println("[fatal] DATABASE_URL is required for persistence configuration")
            exitProcess(1)
        }
    }

    private fun defaultSecret(environment: String): String? {
        return if (environment.lowercase() == "production") null else DEFAULT_JWT_SECRET
    }
}
