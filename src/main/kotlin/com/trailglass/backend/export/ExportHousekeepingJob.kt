package com.trailglass.backend.export

import com.trailglass.backend.storage.ObjectStorageService
import org.slf4j.LoggerFactory
import java.time.Instant

class ExportHousekeepingJob(
    private val repository: ExportJobRepository,
    private val storage: ObjectStorageService,
    private val metrics: ExportMetrics,
) {
    private val logger = LoggerFactory.getLogger(ExportHousekeepingJob::class.java)

    suspend fun removeExpired() {
        val now = Instant.now()
        val expired = repository.list()
            .filter { job -> job.expiresAt?.isBefore(now) == true && job.downloadKey != null }

        expired.forEach { job ->
            job.downloadKey?.let { key ->
                runCatching { storage.deleteObject(key) }
                    .onFailure { logger.warn("Failed to delete expired export {}", key, it) }
            }

            val updated = job.copy(
                status = ExportStatus.EXPIRED,
                downloadUrl = null,
                updatedAt = now,
            )
            repository.save(updated)
        }

        if (expired.isNotEmpty()) {
            metrics.markExpired(expired.size)
            logger.info("Expired {} export artifacts", expired.size)
        }
    }
}
