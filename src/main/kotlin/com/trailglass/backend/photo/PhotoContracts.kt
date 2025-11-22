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
    suspend fun uploadPhoto(userId: UUID, deviceId: UUID, fileBytes: ByteArray, fileName: String, mimeType: String, metadata: PhotoMetadataRequest?): PhotoRecord
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
    val thumbnailStorageKey: String?,
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
    val thumbnailUrl: PresignedObject?,
)

@Serializable
data class PhotoMetadataRequest(
    val id: UUID? = null,
    val timestamp: Instant? = null,
    val location: PhotoLocation? = null,
    val placeVisitId: UUID? = null,
    val tripId: UUID? = null,
    val caption: String? = null,
    val exifData: PhotoExifData? = null,
)

@Serializable
data class PhotoLocation(
    val latitude: Double,
    val longitude: Double,
)

@Serializable
data class PhotoExifData(
    val cameraModel: String? = null,
    val focalLength: Double? = null,
    val aperture: Double? = null,
    val iso: Int? = null,
    val shutterSpeed: String? = null,
)
