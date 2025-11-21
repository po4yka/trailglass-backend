package com.trailglass.backend.photo

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

interface PhotoService {
    suspend fun requestUpload(request: PhotoUploadRequest): PresignedUpload
    suspend fun confirmUpload(photoId: UUID, userId: UUID, deviceId: UUID): PhotoMetadata
    suspend fun fetchMetadata(userId: UUID, updatedAfter: Instant?, limit: Int = 100): List<PhotoMetadata>
}

@Serializable
data class PhotoMetadata(
    val id: UUID,
    val userId: UUID,
    val deviceId: UUID,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val uploadedAt: Instant?,
    val updatedAt: Instant,
    val deletedAt: Instant?,
    val serverVersion: Long,
    val storageKey: String
)

@Serializable
data class PhotoUploadRequest(
    val userId: UUID,
    val deviceId: UUID,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long
)

@Serializable
data class PresignedUpload(
    val uploadUrl: String,
    val headers: Map<String, String> = emptyMap(),
    val photoId: UUID
)
