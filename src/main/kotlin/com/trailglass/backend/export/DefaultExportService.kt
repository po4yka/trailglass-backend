package com.trailglass.backend.export

import com.trailglass.backend.email.EmailService
import com.trailglass.backend.scheduler.RecurringTaskScheduler
import com.trailglass.backend.storage.ObjectStorageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DefaultExportService(
    private val storage: ObjectStorageService,
    private val emailService: EmailService,
    private val repository: ExportJobRepository,
    private val scheduler: RecurringTaskScheduler,
    private val metrics: ExportMetrics,
    private val scope: CoroutineScope,
    private val locationService: com.trailglass.backend.location.LocationService,
    private val tripService: com.trailglass.backend.trip.TripService,
    private val placeVisitService: com.trailglass.backend.visit.PlaceVisitService,
    private val photoService: com.trailglass.backend.photo.PhotoService,
    private val retention: Duration = Duration.ofHours(24),
) : ExportService {
    private val logger = LoggerFactory.getLogger(DefaultExportService::class.java)
    private val json = kotlinx.serialization.json.Json { prettyPrint = true }

    override suspend fun requestExport(request: ExportRequest): ExportJob {
        val now = Instant.now()
        val job = ExportJobRecord(
            id = UUID.randomUUID(),
            userId = request.userId,
            deviceId = request.deviceId,
            status = ExportStatus.PENDING,
            createdAt = now,
            updatedAt = now,
            email = request.email,
            format = request.format,
            includePhotos = request.includePhotos,
            startDate = request.startDate,
            endDate = request.endDate,
        )

        repository.save(job)
        metrics.markRequested()
        scope.launch { process(job.id) }
        return job.toJob()
    }

    override suspend fun getStatus(exportId: UUID, userId: UUID): ExportJob {
        val record = repository.get(exportId)
            ?: error("export not found")

        require(record.userId == userId) { "export does not belong to user" }
        return record.toJob()
    }

    private suspend fun process(exportId: UUID) {
        val record = repository.get(exportId) ?: return
        val running = record.copy(status = ExportStatus.RUNNING, updatedAt = Instant.now())
        repository.save(running)
        metrics.markStarted()

        try {
            val archiveBytes = buildArchive(running)
            val extension = when (running.format) {
                ExportFormat.JSON -> "json"
                ExportFormat.ZIP -> "zip"
            }
            val key = "exports/${running.userId}/${running.id}.$extension"
            val contentType = when (running.format) {
                ExportFormat.JSON -> "application/json"
                ExportFormat.ZIP -> "application/zip"
            }
            storage.putBytes(key, contentType, archiveBytes)
            val presigned = storage.presignDownload(key)
            val completion = running.copy(
                status = ExportStatus.COMPLETED,
                downloadUrl = presigned.url.toString(),
                downloadKey = key,
                updatedAt = Instant.now(),
                expiresAt = Instant.now().plus(retention),
                fileSize = archiveBytes.size.toLong(),
            )

            repository.save(completion)
            metrics.markCompleted()
            notifyUser(completion)
            logger.info("Export job {} completed for user {}", completion.id, completion.userId)
        } catch (ex: Exception) {
            logger.error("Export job {} failed", running.id, ex)
            repository.save(
                running.copy(
                    status = ExportStatus.FAILED,
                    updatedAt = Instant.now(),
                ),
            )
            metrics.markFailed()
        }
    }

    private suspend fun notifyUser(completion: ExportJobRecord) {
        val email = completion.email ?: return
        val expiresAt = completion.expiresAt ?: Instant.now().plus(retention)
        emailService.sendExportReadyEmail(email, completion.downloadUrl.orEmpty(), expiresAt)
    }

    private suspend fun buildArchive(record: ExportJobRecord): ByteArray {
        val exportedAt = Instant.now()
        val metadata = ExportMetadata(
            userId = record.userId,
            deviceId = record.deviceId,
            exportedAt = exportedAt,
            includePhotos = record.includePhotos,
            startDate = record.startDate,
            endDate = record.endDate,
            format = record.format,
        )

        val locations = locationService.getLocations(
            userId = record.userId,
            startTime = record.startDate,
            endTime = record.endDate,
            limit = 5_000,
        )
        val trips = tripService.listTrips(
            userId = record.userId,
            startDate = record.startDate,
            endDate = record.endDate,
            limit = 5_000,
        )
        val placeVisits = placeVisitService.listVisits(
            userId = record.userId,
            startTime = record.startDate,
            endTime = record.endDate,
            limit = 5_000,
        )
        val photos = if (record.includePhotos) {
            photoService.fetchMetadata(record.userId, updatedAfter = null, limit = 5_000)
        } else {
            emptyList()
        }

        val payload = ExportPayload(
            metadata = metadata,
            locations = locations,
            trips = trips,
            visits = placeVisits,
            photos = photos,
        )

        val serialized = json.encodeToString(ExportPayload.serializer(), payload).toByteArray(StandardCharsets.UTF_8)

        return when (record.format) {
            ExportFormat.JSON -> serialized
            ExportFormat.ZIP -> {
                val buffer = ByteArrayOutputStream()
                ZipOutputStream(buffer).use { zip ->
                    zip.putNextEntry(ZipEntry("metadata.json"))
                    zip.write(json.encodeToString(ExportMetadata.serializer(), metadata).toByteArray(StandardCharsets.UTF_8))
                    zip.closeEntry()

                    zip.putNextEntry(ZipEntry("locations.json"))
                    zip.write(json.encodeToString(ListSerializer(com.trailglass.backend.location.LocationSample.serializer()), locations).toByteArray(StandardCharsets.UTF_8))
                    zip.closeEntry()

                    zip.putNextEntry(ZipEntry("trips.json"))
                    zip.write(json.encodeToString(ListSerializer(com.trailglass.backend.trip.TripRecord.serializer()), trips).toByteArray(StandardCharsets.UTF_8))
                    zip.closeEntry()

                    zip.putNextEntry(ZipEntry("place_visits.json"))
                    zip.write(json.encodeToString(ListSerializer(com.trailglass.backend.visit.PlaceVisit.serializer()), placeVisits).toByteArray(StandardCharsets.UTF_8))
                    zip.closeEntry()

                    zip.putNextEntry(ZipEntry("photos.json"))
                    zip.write(json.encodeToString(ListSerializer(com.trailglass.backend.photo.PhotoMetadata.serializer()), photos).toByteArray(StandardCharsets.UTF_8))
                    zip.closeEntry()
                }
                buffer.toByteArray()
            }
        }
    }

    fun scheduleHousekeeping(cleanupJob: ExportHousekeepingJob) {
        scheduler.schedule(
            name = "export-cleanup",
            interval = retention,
            initialDelay = retention,
        ) {
            cleanupJob.removeExpired()
        }
    }

    private fun ExportJobRecord.toJob(): ExportJob = ExportJob(
        id = id,
        userId = userId,
        deviceId = deviceId,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
        downloadUrl = downloadUrl,
        expiresAt = expiresAt,
        fileSize = fileSize,
    )
}

@kotlinx.serialization.Serializable
data class ExportMetadata(
    val userId: java.util.UUID,
    val deviceId: java.util.UUID?,
    val exportedAt: Instant,
    val includePhotos: Boolean,
    val startDate: Instant?,
    val endDate: Instant?,
    val format: ExportFormat,
)

@kotlinx.serialization.Serializable
data class ExportPayload(
    val metadata: ExportMetadata,
    val locations: List<com.trailglass.backend.location.LocationSample>,
    val trips: List<com.trailglass.backend.trip.TripRecord>,
    val visits: List<com.trailglass.backend.visit.PlaceVisit>,
    val photos: List<com.trailglass.backend.photo.PhotoMetadata>,
)
