package com.trailglass.backend.photo

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class ThumbnailGeneratorTest {

    private val generator = ThumbnailGenerator(ThumbnailConfig(size = 100, quality = 0.85f))

    @Test
    fun `generate thumbnail from JPEG image`() = runBlocking {
        val testImage = createTestImage(500, 500)
        val imageBytes = imageToBytes(testImage, "jpg")

        val result = generator.generateThumbnail(imageBytes, "image/jpeg")

        assertTrue(result.isSuccess)
        val thumbnailBytes = result.getOrNull()
        assertNotNull(thumbnailBytes)
        assertTrue(thumbnailBytes!!.isNotEmpty())

        // Verify thumbnail is smaller than original
        assertTrue(thumbnailBytes.size < imageBytes.size)

        // Verify thumbnail can be read as an image
        val thumbnail = ImageIO.read(ByteArrayInputStream(thumbnailBytes))
        assertNotNull(thumbnail)
        assertTrue(thumbnail.width <= 100)
        assertTrue(thumbnail.height <= 100)
    }

    @Test
    fun `generate thumbnail from PNG image`() = runBlocking {
        val testImage = createTestImage(800, 600)
        val imageBytes = imageToBytes(testImage, "png")

        val result = generator.generateThumbnail(imageBytes, "image/png")

        assertTrue(result.isSuccess)
        val thumbnailBytes = result.getOrNull()
        assertNotNull(thumbnailBytes)
        assertTrue(thumbnailBytes!!.isNotEmpty())

        val thumbnail = ImageIO.read(ByteArrayInputStream(thumbnailBytes))
        assertNotNull(thumbnail)
        assertTrue(thumbnail.width <= 100)
        assertTrue(thumbnail.height <= 100)
    }

    @Test
    fun `generate thumbnail maintains aspect ratio`() = runBlocking {
        val testImage = createTestImage(1000, 500)
        val imageBytes = imageToBytes(testImage, "jpg")

        val result = generator.generateThumbnail(imageBytes, "image/jpeg")

        assertTrue(result.isSuccess)
        val thumbnailBytes = result.getOrNull()
        assertNotNull(thumbnailBytes)

        val thumbnail = ImageIO.read(ByteArrayInputStream(thumbnailBytes))
        assertNotNull(thumbnail)

        // Should maintain 2:1 aspect ratio
        val aspectRatio = thumbnail.width.toFloat() / thumbnail.height.toFloat()
        assertTrue(aspectRatio in 1.9f..2.1f)
    }

    @Test
    fun `generate thumbnail handles invalid image data`() = runBlocking {
        val invalidData = byteArrayOf(1, 2, 3, 4, 5)

        val result = generator.generateThumbnail(invalidData, "image/jpeg")

        assertTrue(result.isFailure)
    }

    @Test
    fun `generate thumbnail from stream`() = runBlocking {
        val testImage = createTestImage(400, 400)
        val imageBytes = imageToBytes(testImage, "jpg")
        val inputStream = ByteArrayInputStream(imageBytes)

        val result = generator.generateThumbnail(inputStream, "image/jpeg")

        assertTrue(result.isSuccess)
        val thumbnailBytes = result.getOrNull()
        assertNotNull(thumbnailBytes)
        assertTrue(thumbnailBytes!!.isNotEmpty())
    }

    private fun createTestImage(width: Int, height: Int): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()

        graphics.color = Color.BLUE
        graphics.fillRect(0, 0, width, height)

        graphics.color = Color.WHITE
        graphics.fillOval(width / 4, height / 4, width / 2, height / 2)

        graphics.dispose()
        return image
    }

    private fun imageToBytes(image: BufferedImage, format: String): ByteArray {
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(image, format, outputStream)
        return outputStream.toByteArray()
    }
}
