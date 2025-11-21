package com.trailglass.backend.export

import com.trailglass.backend.email.EmailService
import com.trailglass.backend.scheduler.RecurringTaskScheduler
import com.trailglass.backend.storage.ObjectStorageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DefaultExportService(
    private val storage: ObjectStorageService,
    private val emailService: EmailService,
    private val repository: ExportJobRepository,
    private val scheduler: RecurringTaskScheduler,
    private val metrics: ExportMetrics,
    private val scope: CoroutineScope,
    private val retention: Duration = Duration.ofHours(24),
) : ExportService {
    private val logger = LoggerFactory.getLogger(DefaultExportService::class.java)

    override suspend fun requestExport(userId: UUID, deviceId: UUID, email: String?): ExportJob {
        val now = Instant.now()
        val job = ExportJobRecord(
            id = UUID.randomUUID(),
            userId = userId,
            deviceId = deviceId,
            status = ExportStatus.PENDING,
            createdAt = now,
            updatedAt = now,
            email = email,
        )

        repository.save(job)
        metrics.markRequested()
        scope.launch { process(job.id) }
        return job.toJob()
    }

    override suspend fun getStatus(exportId: UUID, userId: UUID): ExportJob {
        val record = repository.get(exportId)
            ?: error("export not found")

        require(record.userId == userId) { "export does not belong to user" }
        return record.toJob()
    }

    private suspend fun process(exportId: UUID) {
        val record = repository.get(exportId) ?: return
        val running = record.copy(status = ExportStatus.RUNNING, updatedAt = Instant.now())
        repository.save(running)
        metrics.markStarted()

        try {
            val archiveBytes = buildArchive(running)
            val key = "exports/${running.userId}/${running.id}.zip"
            storage.putBytes(key, "application/zip", archiveBytes)
            val presigned = storage.presignDownload(key)
            val completion = running.copy(
                status = ExportStatus.COMPLETED,
                downloadUrl = presigned.url.toString(),
                downloadKey = key,
                updatedAt = Instant.now(),
                expiresAt = Instant.now().plus(retention),
            )

            repository.save(completion)
            metrics.markCompleted()
            notifyUser(completion)
            logger.info("Export job {} completed for user {}", completion.id, completion.userId)
        } catch (ex: Exception) {
            logger.error("Export job {} failed", running.id, ex)
            repository.save(
                running.copy(
                    status = ExportStatus.FAILED,
                    updatedAt = Instant.now(),
                ),
            )
            metrics.markFailed()
        }
    }

    private suspend fun notifyUser(completion: ExportJobRecord) {
        val email = completion.email ?: return
        emailService.sendExportReady(email, completion.downloadUrl.orEmpty())
    }

    private fun buildArchive(record: ExportJobRecord): ByteArray {
        val payload = """
            {
              "userId": "${record.userId}",
              "deviceId": "${record.deviceId}",
              "exportedAt": "${Instant.now()}"
            }
        """.trimIndent().toByteArray(StandardCharsets.UTF_8)

        val buffer = ByteArrayOutputStream()
        ZipOutputStream(buffer).use { zip ->
            zip.putNextEntry(ZipEntry("metadata.json"))
            zip.write(payload)
            zip.closeEntry()
        }

        return buffer.toByteArray()
    }

    fun scheduleHousekeeping(cleanupJob: ExportHousekeepingJob) {
        scheduler.schedule(
            name = "export-cleanup",
            interval = retention,
            initialDelay = retention,
        ) {
            cleanupJob.removeExpired()
        }
    }

    private fun ExportJobRecord.toJob(): ExportJob = ExportJob(
        id = id,
        userId = userId,
        deviceId = deviceId,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
        downloadUrl = downloadUrl,
        expiresAt = expiresAt,
    )
}
