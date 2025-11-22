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
    val autoMigrate: Boolean,
    val allowPlainHttp: Boolean,
    val storage: StorageConfig,
    val cloudflareAccess: CloudflareAccessConfig?,
    val email: EmailConfig,
)

data class StorageConfig(
    val backend: StorageBackend,
    val bucket: String?,
    val region: String?,
    val endpoint: String?,
    val accessKey: String?,
    val secretKey: String?,
    val usePathStyle: Boolean,
    val signingSecret: String?,
    val thumbnailSize: Int,
    val thumbnailQuality: Float,
)

data class CloudflareAccessConfig(
    val clientId: String,
    val clientSecret: String,
)

data class EmailConfig(
    val enabled: Boolean,
    val provider: EmailProvider,
    val smtp: SmtpConfig?,
)

data class SmtpConfig(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val fromEmail: String,
    val fromName: String,
    val useTls: Boolean,
)

enum class StorageBackend { S3, DATABASE }
enum class EmailProvider { SMTP, CONSOLE, SENDGRID, SES }

object ConfigLoader {
    private const val DEFAULT_PORT = 8080
    private const val DEFAULT_HOST = "0.0.0.0"
    private const val DEFAULT_ENVIRONMENT = "development"
    private const val DEFAULT_JWT_ISSUER = "trailglass"
    private const val DEFAULT_JWT_AUDIENCE = "trailglass-clients"
    private const val DEFAULT_JWT_SECRET = "dev-secret-change-me"
    private const val DEFAULT_RATE_LIMIT_PER_MINUTE = 120L
    private const val DEFAULT_STORAGE_BACKEND = "database"
    private const val DEFAULT_STORAGE_BUCKET = "trailglass"

    fun fromEnv(): AppConfig {
        val port = env("APP_PORT")?.toIntOrNull() ?: DEFAULT_PORT
        val host = env("APP_HOST") ?: DEFAULT_HOST
        val environment = env("APP_ENVIRONMENT") ?: DEFAULT_ENVIRONMENT
        val jwtIssuer = env("JWT_ISSUER") ?: DEFAULT_JWT_ISSUER
        val jwtAudience = env("JWT_AUDIENCE") ?: DEFAULT_JWT_AUDIENCE
        val rateLimit = env("GLOBAL_RATE_LIMIT_PER_MINUTE")?.toLongOrNull()
            ?: DEFAULT_RATE_LIMIT_PER_MINUTE

        val enableMetrics = env("ENABLE_METRICS")?.toBooleanStrictOrNull() ?: false
        val autoMigrate = env("FLYWAY_AUTO_MIGRATE")?.toBooleanStrictOrNull()
            ?: environment.lowercase() != "production"
        val allowPlainHttp = env("ALLOW_PLAIN_HTTP")?.toBooleanStrictOrNull()
            ?: environment.lowercase() != "production"

        val storageBackend = env("STORAGE_BACKEND")?.lowercase()
            ?: DEFAULT_STORAGE_BACKEND
        val backend = when (storageBackend) {
            "s3", "minio" -> StorageBackend.S3
            "database", "postgres" -> StorageBackend.DATABASE
            else -> error("Unsupported STORAGE_BACKEND: $storageBackend")
        }

        val storage = StorageConfig(
            backend = backend,
            bucket = env("STORAGE_BUCKET") ?: DEFAULT_STORAGE_BUCKET,
            region = env("STORAGE_REGION") ?: "us-east-1",
            endpoint = env("STORAGE_ENDPOINT"),
            accessKey = env("STORAGE_ACCESS_KEY"),
            secretKey = env("STORAGE_SECRET_KEY"),
            usePathStyle = env("STORAGE_PATH_STYLE")?.toBooleanStrictOrNull() ?: true,
            signingSecret = env("STORAGE_SIGNING_SECRET"),
            thumbnailSize = env("THUMBNAIL_SIZE")?.toIntOrNull() ?: 300,
            thumbnailQuality = env("THUMBNAIL_QUALITY")?.toFloatOrNull() ?: 0.85f,
        )

        val cloudflareAccess = if (env("CLOUDFLARE_ACCESS_CLIENT_ID") != null && env("CLOUDFLARE_ACCESS_CLIENT_SECRET") != null) {
            CloudflareAccessConfig(
                clientId = env("CLOUDFLARE_ACCESS_CLIENT_ID")!!,
                clientSecret = env("CLOUDFLARE_ACCESS_CLIENT_SECRET")!!,
            )
        } else {
            null
        }

        val emailEnabled = env("EMAIL_ENABLED")?.toBooleanStrictOrNull() ?: false
        val emailProviderStr = env("EMAIL_PROVIDER")?.lowercase() ?: "console"
        val emailProvider = when (emailProviderStr) {
            "smtp" -> EmailProvider.SMTP
            "console" -> EmailProvider.CONSOLE
            "sendgrid" -> EmailProvider.SENDGRID
            "ses" -> EmailProvider.SES
            else -> EmailProvider.CONSOLE
        }

        val smtpConfig = if (emailProvider == EmailProvider.SMTP) {
            SmtpConfig(
                host = env("SMTP_HOST") ?: error("SMTP_HOST is required when EMAIL_PROVIDER=smtp"),
                port = env("SMTP_PORT")?.toIntOrNull() ?: 587,
                username = env("SMTP_USERNAME") ?: error("SMTP_USERNAME is required when EMAIL_PROVIDER=smtp"),
                password = env("SMTP_PASSWORD") ?: error("SMTP_PASSWORD is required when EMAIL_PROVIDER=smtp"),
                fromEmail = env("SMTP_FROM_EMAIL") ?: error("SMTP_FROM_EMAIL is required when EMAIL_PROVIDER=smtp"),
                fromName = env("SMTP_FROM_NAME") ?: "Trailglass",
                useTls = env("SMTP_TLS_ENABLED")?.toBooleanStrictOrNull() ?: true,
            )
        } else {
            null
        }

        val email = EmailConfig(
            enabled = emailEnabled,
            provider = emailProvider,
            smtp = smtpConfig,
        )

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
            autoMigrate = autoMigrate,
            allowPlainHttp = allowPlainHttp,
            storage = storage,
            cloudflareAccess = cloudflareAccess,
            email = email,
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

        if (config.storage.backend == StorageBackend.S3) {
            if (config.storage.bucket.isNullOrBlank()) {
                println("[fatal] STORAGE_BUCKET is required for S3 storage backend")
                exitProcess(1)
            }

            if (config.storage.accessKey.isNullOrBlank() || config.storage.secretKey.isNullOrBlank()) {
                println("[fatal] STORAGE_ACCESS_KEY and STORAGE_SECRET_KEY are required for S3 storage backend")
                exitProcess(1)
            }
        }
    }

    private fun defaultSecret(environment: String): String? {
        return if (environment.lowercase() == "production") null else DEFAULT_JWT_SECRET
    }
}
