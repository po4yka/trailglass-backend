@file:UseSerializers(UUIDSerializer::class, InstantSerializer::class)

package com.trailglass.backend.export

import com.trailglass.backend.common.InstantSerializer
import com.trailglass.backend.common.UUIDSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Instant
import java.util.UUID

interface ExportService {
    suspend fun requestExport(request: ExportRequest): ExportJob
    suspend fun getStatus(exportId: UUID, userId: UUID): ExportJob
}

@Serializable
data class ExportRequest(
    val userId: UUID,
    val deviceId: UUID,
    val email: String? = null,
    val format: ExportFormat = ExportFormat.JSON,
    val includePhotos: Boolean = false,
    val startDate: Instant? = null,
    val endDate: Instant? = null,
)

@Serializable
enum class ExportFormat {
    JSON,
    ZIP,
}

@Serializable
data class ExportJob(
    val id: UUID,
    val userId: UUID,
    val deviceId: UUID,
    val status: ExportStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val downloadUrl: String? = null,
    val expiresAt: Instant? = null,
    val fileSize: Long? = null,
)

@Serializable
enum class ExportStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    EXPIRED,
}
