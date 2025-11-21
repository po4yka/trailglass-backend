package com.trailglass.backend.photo

import com.trailglass.backend.config.StorageConfig
import com.trailglass.backend.persistence.PhotoRepository
import com.trailglass.backend.persistence.photoStorageKey
import com.trailglass.backend.storage.ObjectStorageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

class PhotoServiceImpl(
    private val repository: PhotoRepository,
    private val storageService: ObjectStorageService,
    private val storageConfig: StorageConfig,
    private val cleanupIntervalMinutes: Long = 5,
) : PhotoService {
    private val logger = LoggerFactory.getLogger(PhotoServiceImpl::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        scope.launch {
            while (true) {
                delay(cleanupIntervalMinutes.minutes)
                cleanupOrphanedBlobs()
            }
        }
    }

    override suspend fun createUpload(userId: UUID, deviceId: UUID, request: PhotoUploadRequest): PhotoUploadPlan {
        val photoId = UUID.randomUUID()
        val key = photoStorageKey(userId, photoId, request.fileName)
        val metadata = repository.create(
            photoId = photoId,
            userId = userId,
            deviceId = deviceId,
            fileName = request.fileName,
            mimeType = request.mimeType,
            sizeBytes = request.sizeBytes,
            storageKey = key,
            storageBackend = storageConfig.backend.name.lowercase(),
        )

        val upload = storageService.presignUpload(key, request.mimeType, request.sizeBytes)
        return PhotoUploadPlan(metadata, upload)
    }

    override suspend fun confirmUpload(photoId: UUID, userId: UUID): PhotoRecord {
        val metadata = repository.markUploaded(photoId, userId)
            ?: throw IllegalArgumentException("Photo not found for user")
        val download = if (metadata.deletedAt == null) storageService.presignDownload(metadata.storageKey) else null
        return PhotoRecord(metadata, download)
    }

    override suspend fun fetchMetadata(userId: UUID, updatedAfter: Instant?, limit: Int): List<PhotoRecord> {
        return repository.list(userId, updatedAfter, limit)
            .map { photo ->
                val download = if (photo.deletedAt == null) storageService.presignDownload(photo.storageKey) else null
                PhotoRecord(photo, download)
            }
    }

    override suspend fun getPhoto(photoId: UUID, userId: UUID): PhotoRecord {
        val metadata = repository.find(photoId, userId)
            ?: throw IllegalArgumentException("Photo not found for user")
        val download = if (metadata.deletedAt == null) storageService.presignDownload(metadata.storageKey) else null
        return PhotoRecord(metadata, download)
    }

    override suspend fun deletePhoto(photoId: UUID, userId: UUID) {
        val marked = repository.markDeleted(photoId, userId)
        if (!marked) throw IllegalArgumentException("Photo not found for user")
    }

    override suspend fun cleanupOrphanedBlobs() {
        val candidates = repository.pendingBlobDeletion()
        for (photo in candidates) {
            runCatching {
                storageService.deleteObject(photo.storageKey)
                repository.markBlobDeleted(photo.id)
            }.onFailure { ex ->
                logger.warn("Failed to cleanup blob for photo {}", photo.id, ex)
            }
        }
    }
}
