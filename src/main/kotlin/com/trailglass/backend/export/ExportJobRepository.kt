package com.trailglass.backend.export

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.UUID

data class ExportJobRecord(
    val id: UUID,
    val userId: UUID,
    val deviceId: UUID,
    val status: ExportStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val downloadUrl: String? = null,
    val downloadKey: String? = null,
    val expiresAt: Instant? = null,
    val email: String? = null,
)

interface ExportJobRepository {
    suspend fun save(record: ExportJobRecord)
    suspend fun get(id: UUID): ExportJobRecord?
    suspend fun list(): List<ExportJobRecord>
}

class InMemoryExportJobRepository : ExportJobRepository {
    private val mutex = Mutex()
    private val records = LinkedHashMap<UUID, ExportJobRecord>()

    override suspend fun save(record: ExportJobRecord) {
        mutex.withLock { records[record.id] = record }
    }

    override suspend fun get(id: UUID): ExportJobRecord? = mutex.withLock { records[id] }

    override suspend fun list(): List<ExportJobRecord> = mutex.withLock { records.values.toList() }
}
