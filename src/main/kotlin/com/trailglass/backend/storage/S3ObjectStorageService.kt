package com.trailglass.backend.storage

import com.trailglass.backend.config.StorageConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.io.InputStream
import java.net.URI
import java.time.Duration
import java.time.Instant
import kotlin.math.abs

class S3ObjectStorageService(private val config: StorageConfig) : ObjectStorageService {
    private val credentials = StaticCredentialsProvider.create(
        AwsBasicCredentials.create(config.accessKey, config.secretKey)
    )

    private val region = Region.of(config.region)

    private val client: S3Client = S3Client.builder()
        .region(region)
        .credentialsProvider(credentials)
        .apply {
            if (!config.endpoint.isNullOrBlank()) {
                endpointOverride(URI.create(config.endpoint))
            }
        }
        .forcePathStyle(config.usePathStyle)
        .build()

    private val presigner: S3Presigner = S3Presigner.builder()
        .region(region)
        .credentialsProvider(credentials)
        .apply {
            if (!config.endpoint.isNullOrBlank()) {
                endpointOverride(URI.create(config.endpoint))
            }
        }
        .build()

    init {
        ensureBucket()
    }

    override suspend fun presignUpload(key: String, contentType: String, contentLength: Long): PresignedObject =
        withContext(Dispatchers.IO) {
            val request = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest { req ->
                    req.bucket(config.bucket)
                        .key(key)
                        .contentType(contentType)
                        .contentLength(contentLength)
                }
                .build()

            val result = presigner.presignPutObject(request)
            val expiresInSeconds = abs(Duration.between(Instant.now(), result.expiration()).seconds)
            PresignedObject(
                url = result.url().toString(),
                headers = result.signedHeaders().mapValues { it.value.joinToString(",") },
                expiresInSeconds = expiresInSeconds,
            )
        }

    override suspend fun presignDownload(key: String): PresignedObject = withContext(Dispatchers.IO) {
        val request = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(15))
            .getObjectRequest { req ->
                req.bucket(config.bucket)
                    .key(key)
            }
            .build()

        val result = presigner.presignGetObject(request)
        val expiresInSeconds = abs(Duration.between(Instant.now(), result.expiration()).seconds)
        PresignedObject(
            url = result.url().toString(),
            headers = result.signedHeaders().mapValues { it.value.joinToString(",") },
            expiresInSeconds = expiresInSeconds,
        )
    }

    override suspend fun deleteObject(key: String) {
        withContext(Dispatchers.IO) {
            client.deleteObject { it.bucket(config.bucket).key(key) }
        }
    }

    override suspend fun putBytes(key: String, contentType: String, bytes: ByteArray) {
        withContext(Dispatchers.IO) {
            client.putObject({ req ->
                req.bucket(config.bucket)
                    .key(key)
                    .contentType(contentType)
            }, RequestBody.fromBytes(bytes))
        }
    }

    override suspend fun openStream(key: String): InputStream = withContext(Dispatchers.IO) {
        try {
            val response = client.getObject { req -> req.bucket(config.bucket).key(key) }
            response
        } catch (ex: NoSuchKeyException) {
            throw IllegalStateException("Object not found: $key", ex)
        }
    }

    private fun ensureBucket() {
        val bucketName = config.bucket ?: return
        val exists = runCatching {
            client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build())
            true
        }.getOrDefault(false)

        if (!exists) {
            client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build())
        }
    }
}
