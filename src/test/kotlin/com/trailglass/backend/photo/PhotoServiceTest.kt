package com.trailglass.backend.photo

import com.trailglass.backend.config.AppConfig
import com.trailglass.backend.config.EmailConfig
import com.trailglass.backend.config.EmailProvider
import com.trailglass.backend.config.StorageBackend
import com.trailglass.backend.config.StorageConfig
import com.trailglass.backend.persistence.DatabaseFactory
import com.trailglass.backend.persistence.PhotoRepository
import com.trailglass.backend.storage.InlineUrlSigner
import com.trailglass.backend.storage.ObjectStorageService
import com.trailglass.backend.storage.PostgresObjectStorageService
import com.trailglass.backend.storage.S3ObjectStorageService
import com.trailglass.backend.storage.inlineStorageRoutes
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PhotoServiceTest {
    private val network = Network.newNetwork()
    private val postgres = PostgreSQLContainer("postgres:15").apply {
        withNetwork(network)
        withDatabaseName("trailglass")
        withUsername("postgres")
        withPassword("postgres")
    }

    private val minio = GenericContainer<Nothing>("quay.io/minio/minio:latest").apply {
        withNetwork(network)
        withEnv("MINIO_ROOT_USER", "minioadmin")
        withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
        withCommand("server /data --console-address :9001")
        addExposedPort(9000)
        waitingFor(Wait.forHttp("/minio/health/ready").forPort(9000).withStartupTimeout(Duration.ofMinutes(2)))
    }

    private lateinit var dataSource: DataSource
    private lateinit var database: Database

    @BeforeAll
    fun setup() {
        postgres.start()
        minio.start()

        val config = AppConfig(
            host = "localhost",
            port = 0,
            environment = "test",
            databaseUrl = postgres.jdbcUrl,
            databaseUser = postgres.username,
            databasePassword = postgres.password,
            jwtSecret = "secret",
            jwtIssuer = "issuer",
            jwtAudience = "audience",
            rateLimitPerMinute = 100,
            enableMetrics = false,
            storage = StorageConfig(
                backend = StorageBackend.S3,
                bucket = "photos",
                region = "us-east-1",
                endpoint = "http://${minio.host}:${minio.getMappedPort(9000)}",
                accessKey = "minioadmin",
                secretKey = "minioadmin",
                usePathStyle = true,
                signingSecret = "secret",
                thumbnailSize = 300,
                thumbnailQuality = 0.85f,
            ),
            cloudflareAccess = null,
            email = EmailConfig(
                enabled = false,
                provider = EmailProvider.CONSOLE,
                smtp = null,
            ),
        )

        dataSource = DatabaseFactory.dataSource(config)
        database = DatabaseFactory.connect(dataSource)
        flyway(config).migrate()
        seedUsers()
    }

    @AfterAll
    fun tearDown() {
        postgres.stop()
        minio.stop()
    }

    @Test
    fun `presigned upload and download via minio`() = runBlocking {
        val storageConfig = StorageConfig(
            backend = StorageBackend.S3,
            bucket = "photos",
            region = "us-east-1",
            endpoint = "http://${minio.host}:${minio.getMappedPort(9000)}",
            accessKey = "minioadmin",
            secretKey = "minioadmin",
            usePathStyle = true,
            signingSecret = "secret",
            thumbnailSize = 300,
            thumbnailQuality = 0.85f,
        )
        val storage: ObjectStorageService = S3ObjectStorageService(storageConfig)
        val repo = PhotoRepository(database)
        val service = PhotoServiceImpl(repo, storage, storageConfig, cleanupIntervalMinutes = 60)

        val userId = existingUserId()
        val deviceId = existingDeviceId()
        val plan = service.createUpload(userId, deviceId, PhotoUploadRequest("photo.jpg", "image/jpeg", 4))

        val bytes = byteArrayOf(1, 2, 3, 4)
        val client = HttpClient.newHttpClient()
        val uploadRequestBuilder = HttpRequest.newBuilder(URI(plan.upload.url))
            .method("PUT", HttpRequest.BodyPublishers.ofByteArray(bytes))
        plan.upload.headers.forEach { (key, value) -> uploadRequestBuilder.header(key, value) }
        val uploadResponse = client.send(uploadRequestBuilder.build(), HttpResponse.BodyHandlers.ofString())
        assertEquals(200, uploadResponse.statusCode())

        val confirmed = service.confirmUpload(plan.photo.id, userId)
        val downloadUrl = requireNotNull(confirmed.download).url
        val downloadResponse = client.send(HttpRequest.newBuilder(URI(downloadUrl)).GET().build(), HttpResponse.BodyHandlers.ofByteArray())
        assertEquals(200, downloadResponse.statusCode())
        assertArrayEquals(bytes, downloadResponse.body())

        service.deletePhoto(plan.photo.id, userId)
        service.cleanupOrphanedBlobs()
        val notFoundResponse = client.send(HttpRequest.newBuilder(URI(downloadUrl)).GET().build(), HttpResponse.BodyHandlers.ofString())
        assertEquals(404, notFoundResponse.statusCode())
    }

    @Test
    fun `postgres inline fallback allows upload and download`() = runBlocking {
        val storageConfig = StorageConfig(
            backend = StorageBackend.DATABASE,
            bucket = null,
            region = null,
            endpoint = null,
            accessKey = null,
            secretKey = null,
            usePathStyle = true,
            signingSecret = "secret",
            thumbnailSize = 300,
            thumbnailQuality = 0.85f,
        )
        val storage = PostgresObjectStorageService(database, InlineUrlSigner("secret"))
        val repo = PhotoRepository(database)
        val service = PhotoServiceImpl(repo, storage, storageConfig, cleanupIntervalMinutes = 60)

        val userId = existingUserId()
        val deviceId = existingDeviceId()
        val plan = service.createUpload(userId, deviceId, PhotoUploadRequest("inline.jpg", "image/jpeg", 3))

        val bytes = byteArrayOf(9, 8, 7)
        testApplication {
            application {
                routing {
                    inlineStorageRoutes(storage)
                }
            }

            val uploadResponse = client.put(plan.upload.url) {
                contentType(ContentType.Image.JPEG)
                setBody(bytes)
                headers { plan.upload.headers.forEach { (k, v) -> append(k, v) } }
            }
            assertEquals(HttpStatusCode.NoContent, uploadResponse.status)

            val confirmed = service.confirmUpload(plan.photo.id, userId)
            val downloadUrl = requireNotNull(confirmed.download).url
            val downloadResponse = client.get(downloadUrl)
            assertEquals(HttpStatusCode.OK, downloadResponse.status)
            assertArrayEquals(bytes, downloadResponse.body())
        }
    }

    private fun flyway(config: AppConfig) = Flyway.configure()
        .dataSource(config.databaseUrl, config.databaseUser, config.databasePassword)
        .load()

    private fun seedUsers() {
        val userId = existingUserId()
        val deviceId = existingDeviceId()
        transaction(database) {
            exec("INSERT INTO users (id, email, password_hash) VALUES ('$userId', 'user@example.com', 'hash') ON CONFLICT (id) DO NOTHING")
            exec("INSERT INTO devices (id, user_id, label) VALUES ('$deviceId', '$userId', 'device') ON CONFLICT (id) DO NOTHING")
        }
    }

    private fun existingUserId(): UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private fun existingDeviceId(): UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")
}
