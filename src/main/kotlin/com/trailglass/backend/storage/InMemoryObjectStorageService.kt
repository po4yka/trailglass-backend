package com.trailglass.backend.storage

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URL
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

data class StoredObject(
    val key: String,
    val contentType: String,
    val bytes: ByteArray,
    val createdAt: Instant,
)

class InMemoryObjectStorageService : ObjectStorageService {
    private val storage = ConcurrentHashMap<String, StoredObject>()

    override suspend fun presignUpload(key: String, contentType: String, contentLength: Long): PresignedObject {
        return PresignedObject(url = URL("https://storage.local/upload/$key"))
    }

    override suspend fun presignDownload(key: String): PresignedObject {
        return PresignedObject(url = URL("https://storage.local/$key"))
    }

    override suspend fun deleteObject(key: String) {
        storage.remove(key)
    }

    override suspend fun putBytes(key: String, contentType: String, bytes: ByteArray) {
        storage[key] = StoredObject(key, contentType, bytes, Instant.now())
    }

    override suspend fun openStream(key: String): InputStream {
        val obj = storage[key] ?: error("object not found")
        return ByteArrayInputStream(obj.bytes)
    }
}
