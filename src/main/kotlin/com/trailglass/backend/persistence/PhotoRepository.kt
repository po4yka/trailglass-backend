package com.trailglass.backend.persistence

import com.trailglass.backend.photo.PhotoMetadata
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.SortOrder
import java.time.Instant
import java.util.UUID

class PhotoRepository(database: Database) : ExposedRepository(database) {
    suspend fun create(
        photoId: UUID,
        userId: UUID,
        deviceId: UUID,
        fileName: String,
        mimeType: String,
        sizeBytes: Long,
        storageKey: String,
        storageBackend: String,
    ): PhotoMetadata = suspendTx {
        Photos.insert {
            it[id] = photoId
            it[Photos.userId] = userId
            it[Photos.deviceId] = deviceId
            it[Photos.fileName] = fileName
            it[Photos.mimeType] = mimeType
            it[Photos.sizeBytes] = sizeBytes
            it[Photos.storageKey] = storageKey
            it[Photos.storageBackend] = storageBackend
            it[Photos.uploadedAt] = null
            it[Photos.updatedAt] = Instant.now()
            it[Photos.deletedAt] = null
            it[Photos.blobDeletedAt] = null
        }

        findInternal(photoId, userId) ?: throw IllegalStateException("Failed to create photo: $photoId")
    }

    suspend fun markUploaded(photoId: UUID, userId: UUID): PhotoMetadata? = suspendTx {
        val updated = Photos.update({ Photos.id eq photoId and (Photos.userId eq userId) }) {
            it[uploadedAt] = Instant.now()
            it[updatedAt] = Instant.now()
        }
        if (updated == 0) return@suspendTx null
        findInternal(photoId, userId)
    }

    suspend fun find(photoId: UUID, userId: UUID): PhotoMetadata? = suspendTx {
        findInternal(photoId, userId)
    }

    private fun Transaction.findInternal(photoId: UUID, userId: UUID): PhotoMetadata? {
        return Photos.select { Photos.id eq photoId and (Photos.userId eq userId) }
            .limit(1)
            .map(::toMetadata)
            .firstOrNull()
    }

    suspend fun list(userId: UUID, updatedAfter: Instant?, limit: Int): List<PhotoMetadata> = suspendTx {
        var query = Photos.select { Photos.userId eq userId }
        if (updatedAfter != null) {
            query = query.andWhere { Photos.updatedAt greater updatedAfter }
        }
        query.orderBy(Photos.updatedAt to SortOrder.DESC).limit(limit).map(::toMetadata)
    }

    suspend fun markDeleted(photoId: UUID, userId: UUID): Boolean = suspendTx {
        val updated = Photos.update({ Photos.id eq photoId and (Photos.userId eq userId) }) {
            it[deletedAt] = Instant.now()
            it[updatedAt] = Instant.now()
        }
        updated > 0
    }

    suspend fun pendingBlobDeletion(limit: Int = 50): List<PhotoMetadata> = suspendTx {
        Photos.select { Photos.deletedAt.isNotNull() and Photos.blobDeletedAt.isNull() }
            .limit(limit)
            .map(::toMetadata)
    }

    suspend fun markBlobDeleted(photoId: UUID) = suspendTx {
        Photos.update({ Photos.id eq photoId }) {
            it[blobDeletedAt] = Instant.now()
            it[updatedAt] = Instant.now()
        }
    }

    suspend fun updateThumbnailStorageKey(photoId: UUID, userId: UUID, thumbnailStorageKey: String): Boolean = suspendTx {
        val updated = Photos.update({ Photos.id eq photoId and (Photos.userId eq userId) }) {
            it[Photos.thumbnailStorageKey] = thumbnailStorageKey
            it[updatedAt] = Instant.now()
        }
        updated > 0
    }

    private fun toMetadata(row: ResultRow): PhotoMetadata = PhotoMetadata(
        id = row[Photos.id].value,
        userId = row[Photos.userId],
        deviceId = row[Photos.deviceId],
        fileName = row[Photos.fileName],
        mimeType = row[Photos.mimeType],
        sizeBytes = row[Photos.sizeBytes],
        uploadedAt = row[Photos.uploadedAt],
        updatedAt = row[Photos.updatedAt],
        deletedAt = row[Photos.deletedAt],
        serverVersion = row[Photos.serverVersion],
        storageKey = row[Photos.storageKey],
        thumbnailStorageKey = row[Photos.thumbnailStorageKey],
    )
}
