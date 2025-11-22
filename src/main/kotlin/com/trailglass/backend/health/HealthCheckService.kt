package com.trailglass.backend.health

import com.trailglass.backend.storage.ObjectStorageService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.Table
import org.slf4j.LoggerFactory
import javax.sql.DataSource
import kotlin.system.measureTimeMillis

/**
 * Service for checking the health of application components.
 * Implements caching to avoid excessive health check overhead.
 */
class HealthCheckService(
    private val dataSource: DataSource,
    private val storageService: ObjectStorageService,
    private val emailEnabled: Boolean = false,
    private val cacheDurationMs: Long = 10_000 // Cache health checks for 10 seconds
) {
    private val logger = LoggerFactory.getLogger(HealthCheckService::class.java)
    private val cacheMutex = Mutex()

    private var lastCheckTime: Long = 0
    private var cachedDatabaseHealth: ComponentHealth? = null
    private var cachedStorageHealth: ComponentHealth? = null
    private var cachedEmailHealth: ComponentHealth? = null

    /**
     * Checks database connectivity and returns health status.
     * Executes a simple query to verify the connection is alive.
     */
    suspend fun checkDatabase(): ComponentHealth = cacheMutex.withLock {
        val now = System.currentTimeMillis()
        if (cachedDatabaseHealth != null && (now - lastCheckTime) < cacheDurationMs) {
            return cachedDatabaseHealth!!
        }

        return try {
            val responseTime = measureTimeMillis {
                // Simple query to verify database is accessible
                transaction {
                    // Query the information_schema to verify connection
                    exec("SELECT 1") { rs ->
                        rs.next()
                    }
                }
            }

            ComponentHealth(
                status = HealthStatus.UP,
                responseTime = "${responseTime}ms"
            ).also {
                cachedDatabaseHealth = it
                lastCheckTime = now
            }
        } catch (e: Exception) {
            logger.error("Database health check failed", e)
            ComponentHealth(
                status = HealthStatus.DOWN,
                error = e.message
            ).also {
                cachedDatabaseHealth = it
                lastCheckTime = now
            }
        }
    }

    /**
     * Checks storage service health.
     * For now, we consider storage healthy if the service is instantiated.
     * Future enhancement: could test actual connectivity to S3/MinIO.
     */
    suspend fun checkStorage(): ComponentHealth = cacheMutex.withLock {
        val now = System.currentTimeMillis()
        if (cachedStorageHealth != null && (now - lastCheckTime) < cacheDurationMs) {
            return cachedStorageHealth!!
        }

        return try {
            // Storage service is initialized, consider it healthy
            // In a real-world scenario, you might want to test actual connectivity
            // by listing buckets or checking a health endpoint
            ComponentHealth(
                status = HealthStatus.UP
            ).also {
                cachedStorageHealth = it
                lastCheckTime = now
            }
        } catch (e: Exception) {
            logger.error("Storage health check failed", e)
            ComponentHealth(
                status = HealthStatus.DOWN,
                error = e.message
            ).also {
                cachedStorageHealth = it
                lastCheckTime = now
            }
        }
    }

    /**
     * Checks email service health.
     * Returns disabled status if email is not configured.
     */
    suspend fun checkEmail(): ComponentHealth = cacheMutex.withLock {
        val now = System.currentTimeMillis()
        if (cachedEmailHealth != null && (now - lastCheckTime) < cacheDurationMs) {
            return cachedEmailHealth!!
        }

        return if (!emailEnabled) {
            ComponentHealth(
                status = HealthStatus.DISABLED
            ).also {
                cachedEmailHealth = it
                lastCheckTime = now
            }
        } else {
            // Email is enabled, consider it healthy
            // Future enhancement: could test SMTP connectivity
            ComponentHealth(
                status = HealthStatus.UP
            ).also {
                cachedEmailHealth = it
                lastCheckTime = now
            }
        }
    }

    /**
     * Checks all critical components and returns overall health.
     * Critical components: database, storage
     * Non-critical: email
     */
    suspend fun checkAllComponents(): Map<String, ComponentHealth> {
        return mapOf(
            "database" to checkDatabase(),
            "storage" to checkStorage(),
            "email" to checkEmail()
        )
    }

    /**
     * Determines if the application is ready to serve traffic.
     * Ready means all critical components (database, storage) are UP.
     */
    suspend fun isReady(): Boolean {
        val dbHealth = checkDatabase()
        val storageHealth = checkStorage()
        return dbHealth.status == HealthStatus.UP && storageHealth.status == HealthStatus.UP
    }

    /**
     * Determines overall health status based on component health.
     */
    fun determineOverallStatus(components: Map<String, ComponentHealth>): HealthStatus {
        val statuses = components.values.map { it.status }

        return when {
            statuses.any { it == HealthStatus.DOWN } -> HealthStatus.UNHEALTHY
            statuses.any { it == HealthStatus.DEGRADED } -> HealthStatus.DEGRADED
            else -> HealthStatus.HEALTHY
        }
    }

    /**
     * Clears the health check cache.
     * Useful for testing or forcing immediate health checks.
     */
    suspend fun clearCache() = cacheMutex.withLock {
        lastCheckTime = 0
        cachedDatabaseHealth = null
        cachedStorageHealth = null
        cachedEmailHealth = null
    }
}

enum class HealthStatus {
    UP,
    DOWN,
    HEALTHY,
    DEGRADED,
    UNHEALTHY,
    DISABLED
}

data class ComponentHealth(
    val status: HealthStatus,
    val responseTime: String? = null,
    val error: String? = null
)
