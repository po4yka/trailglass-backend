package com.trailglass.backend.export

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

interface ExportService {
    suspend fun requestExport(userId: UUID, deviceId: UUID): ExportJob
    suspend fun getStatus(exportId: UUID, userId: UUID): ExportJob
}

@Serializable
data class ExportJob(
    val id: UUID,
    val userId: UUID,
    val deviceId: UUID,
    val status: ExportStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val downloadUrl: String? = null
)

@Serializable
enum class ExportStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}
