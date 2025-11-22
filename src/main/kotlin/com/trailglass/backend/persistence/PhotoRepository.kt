package com.trailglass.backend.persistence

import com.trailglass.backend.photo.PhotoMetadata
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.SortOrder
import java.time.Instant
import java.util.UUID

class PhotoRepository(private val database: Database) {
    suspend fun create(
        photoId: UUID,
        userId: UUID,
        deviceId: UUID,
        fileName: String,
        mimeType: String,
        sizeBytes: Long,
        storageKey: String,
        storageBackend: String,
    ): PhotoMetadata = newSuspendedTransaction(Dispatchers.IO, db = database) {
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

        find(photoId, userId)!!
    }

    suspend fun markUploaded(photoId: UUID, userId: UUID): PhotoMetadata? =
        newSuspendedTransaction(Dispatchers.IO, db = database) {
            val updated = Photos.update({ Photos.id eq photoId and (Photos.userId eq userId) }) {
                it[uploadedAt] = Instant.now()
                it[updatedAt] = Instant.now()
            }
            if (updated == 0) return@newSuspendedTransaction null
            find(photoId, userId)
        }

    suspend fun find(photoId: UUID, userId: UUID): PhotoMetadata? =
        newSuspendedTransaction(Dispatchers.IO, db = database) {
            Photos.select { Photos.id eq photoId and (Photos.userId eq userId) }
                .limit(1)
                .map(::toMetadata)
                .firstOrNull()
        }

    suspend fun list(userId: UUID, updatedAfter: Instant?, limit: Int): List<PhotoMetadata> =
        newSuspendedTransaction(Dispatchers.IO, db = database) {
            var query = Photos.select { Photos.userId eq userId }
            if (updatedAfter != null) {
                query = query.andWhere { Photos.updatedAt greater updatedAfter }
            }
            query.orderBy(Photos.updatedAt to SortOrder.DESC).limit(limit).map(::toMetadata)
        }

    suspend fun markDeleted(photoId: UUID, userId: UUID): Boolean =
        newSuspendedTransaction(Dispatchers.IO, db = database) {
            val updated = Photos.update({ Photos.id eq photoId and (Photos.userId eq userId) }) {
                it[deletedAt] = Instant.now()
                it[updatedAt] = Instant.now()
            }
            updated > 0
        }

    suspend fun pendingBlobDeletion(limit: Int = 50): List<PhotoMetadata> =
        newSuspendedTransaction(Dispatchers.IO, db = database) {
            Photos.select { Photos.deletedAt.isNotNull() and Photos.blobDeletedAt.isNull() }
                .limit(limit)
                .map(::toMetadata)
        }

    suspend fun markBlobDeleted(photoId: UUID) {
        newSuspendedTransaction(Dispatchers.IO, db = database) {
            Photos.update({ Photos.id eq photoId }) {
                it[blobDeletedAt] = Instant.now()
                it[updatedAt] = Instant.now()
            }
        }
    }

    suspend fun updateThumbnailStorageKey(photoId: UUID, userId: UUID, thumbnailStorageKey: String): Boolean =
        newSuspendedTransaction(Dispatchers.IO, db = database) {
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
