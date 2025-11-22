package com.trailglass.backend.photo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.coobird.thumbnailator.Thumbnails
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

data class ThumbnailConfig(
    val size: Int = 300,
    val quality: Float = 0.85f,
)

class ThumbnailGenerator(private val config: ThumbnailConfig = ThumbnailConfig()) {
    private val logger = LoggerFactory.getLogger(ThumbnailGenerator::class.java)

    suspend fun generateThumbnail(inputStream: InputStream, mimeType: String): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            runCatching {
                val outputStream = ByteArrayOutputStream()

                val outputFormat = when {
                    mimeType.contains("png", ignoreCase = true) -> "png"
                    mimeType.contains("webp", ignoreCase = true) -> "webp"
                    else -> "jpg"
                }

                Thumbnails.of(inputStream)
                    .size(config.size, config.size)
                    .outputQuality(config.quality.toDouble())
                    .outputFormat(outputFormat)
                    .toOutputStream(outputStream)

                val thumbnailBytes = outputStream.toByteArray()
                logger.debug("Generated thumbnail: size=${thumbnailBytes.size} bytes, format=$outputFormat")
                thumbnailBytes
            }.onFailure { error ->
                logger.error("Failed to generate thumbnail", error)
            }
        }

    suspend fun generateThumbnail(imageBytes: ByteArray, mimeType: String): Result<ByteArray> {
        return generateThumbnail(ByteArrayInputStream(imageBytes), mimeType)
    }
}
