@file:UseSerializers(UUIDSerializer::class, InstantSerializer::class)

package com.trailglass.backend.photo

import com.trailglass.backend.common.InstantSerializer
import com.trailglass.backend.common.UUIDSerializer
import com.trailglass.backend.storage.PresignedObject
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Instant
import java.util.UUID

interface PhotoService {
    suspend fun createUpload(userId: UUID, deviceId: UUID, request: PhotoUploadRequest): PhotoUploadPlan
    suspend fun confirmUpload(photoId: UUID, userId: UUID): PhotoRecord
    suspend fun fetchMetadata(userId: UUID, updatedAfter: Instant?, limit: Int = 100): List<PhotoRecord>
    suspend fun getPhoto(photoId: UUID, userId: UUID): PhotoRecord
    suspend fun deletePhoto(photoId: UUID, userId: UUID)
    suspend fun cleanupOrphanedBlobs()
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
    val storageKey: String,
)

@Serializable
data class PhotoUploadRequest(
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
)

@Serializable
data class PhotoUploadPlan(
    val photo: PhotoMetadata,
    val upload: PresignedObject,
)

@Serializable
data class PhotoRecord(
    val photo: PhotoMetadata,
    val download: PresignedObject?,
)
