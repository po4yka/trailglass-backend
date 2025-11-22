package com.trailglass.backend.storage

import com.trailglass.backend.persistence.ExposedRepository
import com.trailglass.backend.persistence.PhotoBlobs
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant

class PostgresObjectStorageService(
    database: Database,
    private val signer: InlineUrlSigner,
) : ExposedRepository(database), ObjectStorageService {
    override suspend fun presignUpload(key: String, contentType: String, contentLength: Long): PresignedObject {
        val expiresAt = Instant.now().plusSeconds(DEFAULT_EXPIRY_SECONDS)
        val token = signer.sign(key, InlineUrlSigner.Operation.UPLOAD, expiresAt)
        return PresignedObject(
            url = inlineUrl(key, token),
            headers = mapOf("Content-Type" to contentType),
            expiresInSeconds = DEFAULT_EXPIRY_SECONDS,
        )
    }

    override suspend fun presignDownload(key: String): PresignedObject {
        val expiresAt = Instant.now().plusSeconds(DEFAULT_EXPIRY_SECONDS)
        val token = signer.sign(key, InlineUrlSigner.Operation.DOWNLOAD, expiresAt)
        return PresignedObject(
            url = inlineUrl(key, token),
            expiresInSeconds = DEFAULT_EXPIRY_SECONDS,
        )
    }

    override suspend fun deleteObject(key: String) = suspendTx {
        PhotoBlobs.update({ PhotoBlobs.storageKey eq key }) {
            it[data] = ByteArray(0)
            it[contentType] = null
            it[updatedAt] = Instant.now()
        }
    }

    override suspend fun putBytes(key: String, contentType: String, bytes: ByteArray) = suspendTx {
        val exists = PhotoBlobs.select { PhotoBlobs.storageKey eq key }.count() > 0
        if (exists) {
            PhotoBlobs.update({ PhotoBlobs.storageKey eq key }) {
                it[data] = bytes
                it[PhotoBlobs.contentType] = contentType
                it[updatedAt] = Instant.now()
            }
        } else {
            PhotoBlobs.insert {
                it[storageKey] = key
                it[PhotoBlobs.contentType] = contentType
                it[PhotoBlobs.data] = bytes
                it[createdAt] = Instant.now()
                it[updatedAt] = Instant.now()
            }
        }
    }

    override suspend fun openStream(key: String): InputStream {
        val bytes = suspendTx {
            PhotoBlobs.select { PhotoBlobs.storageKey eq key }
                .limit(1)
                .map { it[PhotoBlobs.data] }
                .firstOrNull()
        } ?: throw IllegalStateException("Object not found: $key")

        return ByteArrayInputStream(bytes)
    }

    fun verifyToken(key: String, token: String, operation: InlineUrlSigner.Operation, now: Instant = Instant.now()): Boolean {
        return signer.verify(token, key, operation, now)
    }

    private fun inlineUrl(key: String, token: String): String {
        val encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8)
        val encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8)
        return "/api/v1/storage/inline/object?key=$encodedKey&token=$encodedToken"
    }

    companion object {
        private const val DEFAULT_EXPIRY_SECONDS = 900L
    }
}
