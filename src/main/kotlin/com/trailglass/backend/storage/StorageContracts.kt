package com.trailglass.backend.storage

import java.io.InputStream
import java.net.URL

interface ObjectStorageService {
    suspend fun presignUpload(key: String, contentType: String, contentLength: Long): PresignedObject
    suspend fun presignDownload(key: String): PresignedObject
    suspend fun deleteObject(key: String)
    suspend fun putBytes(key: String, contentType: String, bytes: ByteArray)
    suspend fun openStream(key: String): InputStream
}

data class PresignedObject(
    val url: URL,
    val headers: Map<String, String> = emptyMap(),
    val expiresInSeconds: Long = 900
)
