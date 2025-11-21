package com.trailglass.backend.persistence

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table
import java.time.Instant
import java.util.UUID

object Photos : UUIDTable("photos") {
    val userId = uuid("user_id")
    val deviceId = uuid("device_id")
    val fileName = text("file_name")
    val mimeType = text("mime_type")
    val sizeBytes = long("size_bytes")
    val storageKey = text("storage_key")
    val storageBackend = varchar("storage_backend", 50)
    val uploadedAt = timestamp("uploaded_at").nullable()
    val updatedAt = timestamp("updated_at")
    val deletedAt = timestamp("deleted_at").nullable()
    val blobDeletedAt = timestamp("blob_deleted_at").nullable()
    val serverVersion = long("server_version").autoIncrement()

    override val primaryKey = PrimaryKey(id)
}

object PhotoBlobs : Table("photo_blobs") {
    val storageKey = text("storage_key")
    val contentType = text("content_type").nullable()
    val data = binary("data", Long.MAX_VALUE.toInt())
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(storageKey)
}

fun photoStorageKey(userId: UUID, photoId: UUID, fileName: String): String {
    val sanitized = fileName.substringAfterLast('/')
    return "photos/$userId/$photoId/$sanitized"
}
