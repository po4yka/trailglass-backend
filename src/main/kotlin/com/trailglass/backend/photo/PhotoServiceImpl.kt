package com.trailglass.backend.photo

import com.trailglass.backend.config.StorageConfig
import com.trailglass.backend.persistence.PhotoRepository
import com.trailglass.backend.persistence.photoStorageKey
import com.trailglass.backend.persistence.thumbnailStorageKey
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
    private val thumbnailGenerator = ThumbnailGenerator(
        ThumbnailConfig(
            size = storageConfig.thumbnailSize,
            quality = storageConfig.thumbnailQuality,
        )
    )

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

    override suspend fun uploadPhoto(userId: UUID, deviceId: UUID, fileBytes: ByteArray, fileName: String, mimeType: String, metadata: PhotoMetadataRequest?): PhotoRecord {
        val photoId = metadata?.id ?: UUID.randomUUID()
        val storageKey = photoStorageKey(userId, photoId, fileName)
        val now = Instant.now()

        // Create photo metadata
        val photoMetadata = repository.create(
            photoId = photoId,
            userId = userId,
            deviceId = deviceId,
            fileName = fileName,
            mimeType = mimeType,
            sizeBytes = fileBytes.size.toLong(),
            storageKey = storageKey,
            storageBackend = storageConfig.backend.name.lowercase(),
        )

        // Upload photo binary
        storageService.putBytes(storageKey, mimeType, fileBytes)

        // Mark as uploaded
        val uploadedMetadata = repository.markUploaded(photoId, userId)
            ?: throw IllegalArgumentException("Photo not found after creation")

        // Generate thumbnail asynchronously
        scope.launch {
            generateAndStoreThumbnail(uploadedMetadata)
        }

        // Return photo record with presigned URLs
        val download = storageService.presignDownload(storageKey)
        return PhotoRecord(
            photo = uploadedMetadata,
            download = download,
            thumbnailUrl = null // Will be available after thumbnail generation
        )
    }

    override suspend fun confirmUpload(photoId: UUID, userId: UUID): PhotoRecord {
        val metadata = repository.markUploaded(photoId, userId)
            ?: throw IllegalArgumentException("Photo not found for user")

        // Generate thumbnail asynchronously (don't block photo upload on failure)
        scope.launch {
            generateAndStoreThumbnail(metadata)
        }

        val download = if (metadata.deletedAt == null) storageService.presignDownload(metadata.storageKey) else null
        val thumbnailUrl = if (metadata.deletedAt == null && metadata.thumbnailStorageKey != null) {
            storageService.presignDownload(metadata.thumbnailStorageKey)
        } else null

        return PhotoRecord(metadata, download, thumbnailUrl)
    }

    private suspend fun generateAndStoreThumbnail(metadata: PhotoMetadata) {
        runCatching {
            logger.debug("Starting thumbnail generation for photo {}", metadata.id)

            // Download the original photo
            storageService.openStream(metadata.storageKey).use { inputStream ->
                // Generate thumbnail
                val thumbnailBytes = thumbnailGenerator.generateThumbnail(inputStream, metadata.mimeType)
                    .getOrElse { error ->
                        logger.warn("Failed to generate thumbnail for photo {}: {}", metadata.id, error.message)
                        return
                    }

                // Determine thumbnail storage key
                val thumbKey = thumbnailStorageKey(metadata.userId, metadata.id, metadata.fileName)

                // Determine thumbnail mime type
                val thumbMimeType = when {
                    metadata.mimeType.contains("png", ignoreCase = true) -> "image/png"
                    metadata.mimeType.contains("webp", ignoreCase = true) -> "image/webp"
                    else -> "image/jpeg"
                }

                // Store thumbnail
                storageService.putBytes(thumbKey, thumbMimeType, thumbnailBytes)

                // Update metadata with thumbnail storage key
                repository.updateThumbnailStorageKey(metadata.id, metadata.userId, thumbKey)

                logger.info("Successfully generated and stored thumbnail for photo {}", metadata.id)
            }
        }.onFailure { error ->
            logger.error("Failed to generate thumbnail for photo {}", metadata.id, error)
        }
    }

    override suspend fun fetchMetadata(userId: UUID, updatedAfter: Instant?, limit: Int): List<PhotoRecord> {
        return repository.list(userId, updatedAfter, limit)
            .map { photo ->
                val download = if (photo.deletedAt == null) storageService.presignDownload(photo.storageKey) else null
                val thumbnailUrl = if (photo.deletedAt == null && photo.thumbnailStorageKey != null) {
                    storageService.presignDownload(photo.thumbnailStorageKey)
                } else null
                PhotoRecord(photo, download, thumbnailUrl)
            }
    }

    override suspend fun getPhoto(photoId: UUID, userId: UUID): PhotoRecord {
        val metadata = repository.find(photoId, userId)
            ?: throw IllegalArgumentException("Photo not found for user")
        val download = if (metadata.deletedAt == null) storageService.presignDownload(metadata.storageKey) else null
        val thumbnailUrl = if (metadata.deletedAt == null && metadata.thumbnailStorageKey != null) {
            storageService.presignDownload(metadata.thumbnailStorageKey)
        } else null
        return PhotoRecord(metadata, download, thumbnailUrl)
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
