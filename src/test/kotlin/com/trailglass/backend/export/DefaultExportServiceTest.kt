package com.trailglass.backend.export

import com.trailglass.backend.email.EmailService
import com.trailglass.backend.scheduler.RecurringTaskScheduler
import com.trailglass.backend.storage.InMemoryObjectStorageService
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import java.time.Duration
import java.time.Instant
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultExportServiceTest {
    private val dispatcher = StandardTestDispatcher()
    private val scope = CoroutineScope(dispatcher)

    @Test
    fun `export job completes and notifies user`() = runTest(dispatcher) {
        val storage = InMemoryObjectStorageService()
        val email = RecordingEmailService()
        val repository = InMemoryExportJobRepository()
        val metrics = ExportMetrics(SimpleMeterRegistry())
        val scheduler = RecurringTaskScheduler(scope)
        val service = DefaultExportService(
            storage = storage,
            emailService = email,
            repository = repository,
            scheduler = scheduler,
            metrics = metrics,
            scope = scope,
            retention = Duration.ofSeconds(1),
        )

        val userId = UUID.randomUUID()
        val deviceId = UUID.randomUUID()
        val job = service.requestExport(userId, deviceId, "user@example.com")

        dispatcher.scheduler.advanceUntilIdle()

        val status = service.getStatus(job.id, job.userId)
        assertEquals(ExportStatus.COMPLETED, status.status)
        assertNotNull(status.downloadUrl)
        assertEquals(listOf("user@example.com"), email.sent)
    }

    @Test
    fun `cleanup removes expired export artifacts`() = runTest(dispatcher) {
        val storage = InMemoryObjectStorageService()
        val repository = InMemoryExportJobRepository()
        val metrics = ExportMetrics(SimpleMeterRegistry())
        val scheduler = RecurringTaskScheduler(scope)
        val email = RecordingEmailService()
        val service = DefaultExportService(
            storage = storage,
            emailService = email,
            repository = repository,
            scheduler = scheduler,
            metrics = metrics,
            scope = scope,
            retention = Duration.ofMillis(10),
        )
        val cleanup = ExportHousekeepingJob(repository, storage, metrics)

        val userId = UUID.randomUUID()
        val deviceId = UUID.randomUUID()
        val job = service.requestExport(userId, deviceId, null)

        dispatcher.scheduler.advanceUntilIdle()

        val completed = repository.get(job.id)!!.copy(expiresAt = Instant.now().minusMillis(5))
        repository.save(completed)

        cleanup.removeExpired()

        val expired = service.getStatus(job.id, userId)
        assertEquals(ExportStatus.EXPIRED, expired.status)
        assertNull(expired.downloadUrl)
    }
}

private class RecordingEmailService : EmailService {
    val sent = mutableListOf<String>()

    override suspend fun sendPasswordResetEmail(email: String, resetToken: String, resetUrl: String): Boolean {
        sent.add(email)
        return true
    }

    override suspend fun sendExportReadyEmail(email: String, exportUrl: String, expiresAt: Instant): Boolean {
        sent.add(email)
        return true
    }
}
