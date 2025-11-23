package com.trailglass.backend.stubs

import com.trailglass.backend.export.ExportJob
import com.trailglass.backend.export.ExportRequest
import com.trailglass.backend.export.ExportService
import com.trailglass.backend.export.ExportStatus
import com.trailglass.backend.location.LocationBatchRequest
import com.trailglass.backend.location.LocationBatchResult
import com.trailglass.backend.location.LocationSample
import com.trailglass.backend.location.LocationService
import com.trailglass.backend.photo.PhotoMetadata
import com.trailglass.backend.photo.PhotoMetadataRequest
import com.trailglass.backend.photo.PhotoRecord
import com.trailglass.backend.photo.PhotoService
import com.trailglass.backend.photo.PhotoUploadPlan
import com.trailglass.backend.photo.PhotoUploadRequest
import com.trailglass.backend.settings.SettingsService
import com.trailglass.backend.settings.SettingsUpdateRequest
import com.trailglass.backend.settings.UserSettings
import com.trailglass.backend.storage.PresignedObject
import com.trailglass.backend.sync.ConflictResolutionRequest
import com.trailglass.backend.sync.ConflictResolutionResult
import com.trailglass.backend.sync.SyncDeltaRequest
import com.trailglass.backend.sync.SyncDeltaResponse
import com.trailglass.backend.sync.SyncEnvelope
import com.trailglass.backend.sync.SyncService
import com.trailglass.backend.sync.SyncStatus
import com.trailglass.backend.trip.TripRecord
import com.trailglass.backend.trip.TripService
import com.trailglass.backend.trip.TripUpdateRequest
import com.trailglass.backend.trip.TripUpsertRequest
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class StubSyncService : SyncService {
    private val envelopes = ConcurrentHashMap<UUID, MutableList<SyncEnvelope>>()
    private val status = ConcurrentHashMap<Pair<UUID, UUID>, SyncStatus>()

    override suspend fun getStatus(deviceId: UUID, userId: UUID): SyncStatus =
        status.getOrPut(deviceId to userId) {
            SyncStatus(
                deviceId = deviceId,
                userId = userId,
                latestServerVersion = 0,
                lastSyncAt = null,
            )
        }

    override suspend fun applyDelta(request: SyncDeltaRequest): SyncDeltaResponse {
        val currentStatus = getStatus(request.deviceId, request.userId)
        val updatedVersion = maxOf(currentStatus.latestServerVersion, Instant.now().toEpochMilli())
        val deviceKey = request.deviceId
        val deviceEnvelopes = envelopes.getOrPut(deviceKey) { mutableListOf() }
        deviceEnvelopes.addAll(request.incoming)

        val outbound = envelopes.values
            .flatten()
            .filter { it.serverVersion > request.sinceVersion }
            .filterNot { it.deviceId == request.deviceId }

        val newStatus = currentStatus.copy(latestServerVersion = updatedVersion, lastSyncAt = Instant.now())
        status[request.deviceId to request.userId] = newStatus

        return SyncDeltaResponse(
            serverVersion = updatedVersion,
            applied = request.incoming,
            conflicts = emptyList(),
            outbound = outbound,
        )
    }

    override suspend fun resolveConflict(request: ConflictResolutionRequest): ConflictResolutionResult =
        ConflictResolutionResult(
            entityId = request.entityId,
            serverVersion = Instant.now().toEpochMilli(),
            resolvedAt = Instant.now(),
        )
}

class StubLocationService : LocationService {
    private val samples = ConcurrentHashMap<UUID, LocationSample>()

    override suspend fun upsertBatch(request: LocationBatchRequest): LocationBatchResult {
        require(request.samples.all { it.userId == request.userId }) { "All samples must match request userId" }
        require(request.samples.all { it.deviceId == request.deviceId }) { "All samples must match request deviceId" }

        val version = Instant.now().toEpochMilli()
        var applied = 0
        request.samples.forEach { sample ->
            val existing = samples[sample.id]
            if (existing == null || existing.updatedAt < sample.updatedAt) {
                samples[sample.id] = sample.copy(serverVersion = version)
                applied++
            }
        }

        return LocationBatchResult(appliedCount = applied, serverVersion = version)
    }

    override suspend fun getLocations(
        userId: UUID,
        since: Instant?,
        startTime: Instant?,
        endTime: Instant?,
        minAccuracy: Float?,
        limit: Int,
        offset: Int
    ): List<LocationSample> {
        return samples.values
            .filter { it.userId == userId }
            .filter { since == null || it.updatedAt >= since }
            .filter { startTime == null || it.recordedAt >= startTime }
            .filter { endTime == null || it.recordedAt <= endTime }
            .filter { minAccuracy == null || (it.accuracy ?: Float.MAX_VALUE) <= minAccuracy }
            .sortedByDescending { it.updatedAt }
            .drop(offset)
            .take(limit)
    }

    override suspend fun getLocation(userId: UUID, locationId: UUID): LocationSample =
        samples[locationId]?.takeIf { it.userId == userId }
            ?: throw IllegalArgumentException("Location not found for user")

    override suspend fun deleteLocations(userId: UUID, ids: List<UUID>): LocationBatchResult {
        val version = Instant.now().toEpochMilli()
        var applied = 0
        ids.forEach { id ->
            val existing = samples[id]
            if (existing != null && existing.userId == userId) {
                samples[id] = existing.copy(deletedAt = Instant.now(), updatedAt = Instant.now(), serverVersion = version)
                applied++
            }
        }
        return LocationBatchResult(appliedCount = applied, serverVersion = version)
    }
}

class StubTripService : TripService {
    private val trips = ConcurrentHashMap<UUID, TripRecord>()

    override suspend fun upsertTrip(request: TripUpsertRequest): TripRecord {
        val version = Instant.now().toEpochMilli()
        val existing = trips[request.trip.id]
        val record = if (existing == null || existing.updatedAt < request.trip.updatedAt) {
            request.trip.copy(serverVersion = version)
        } else {
            existing
        }
        trips[record.id] = record
        return record
    }

    override suspend fun updateTrip(userId: UUID, tripId: UUID, request: TripUpdateRequest, expectedVersion: Long?): TripRecord {
        val current = trips[tripId]?.takeIf { it.userId == userId }
            ?: throw IllegalArgumentException("Trip not found for user")
        if (expectedVersion != null && current.serverVersion != expectedVersion) {
            throw IllegalArgumentException("Entity was modified by another device. Current version: ${current.serverVersion}, expected: $expectedVersion")
        }

        val now = Instant.now()
        val updated = current.copy(
            name = request.name ?: current.name,
            startDate = request.startDate ?: current.startDate,
            endDate = request.endDate ?: current.endDate,
            updatedAt = now,
            serverVersion = now.toEpochMilli(),
        )
        trips[tripId] = updated
        return updated
    }

    override suspend fun listTrips(
        userId: UUID,
        updatedAfter: Instant?,
        startDate: Instant?,
        endDate: Instant?,
        limit: Int,
        offset: Int
    ): List<TripRecord> = trips.values
        .filter { it.userId == userId }
        .filter { updatedAfter == null || it.updatedAt >= updatedAfter }
        .filter { startDate == null || (it.startDate ?: Instant.EPOCH) >= startDate }
        .filter { endDate == null || (it.endDate ?: Instant.MAX) <= endDate }
        .sortedByDescending { it.updatedAt }
        .drop(offset)
        .take(limit)

    override suspend fun getTrip(userId: UUID, tripId: UUID): TripRecord =
        trips[tripId]?.takeIf { it.userId == userId }
            ?: throw IllegalArgumentException("Trip not found for user")

    override suspend fun deleteTrip(userId: UUID, tripId: UUID): TripRecord {
        val now = Instant.now()
        val current = trips[tripId]?.takeIf { it.userId == userId }
            ?: throw IllegalArgumentException("Trip not found for user")
        val deleted = current.copy(deletedAt = now, updatedAt = now, serverVersion = now.toEpochMilli())
        trips[tripId] = deleted
        return deleted
    }
}

class StubPhotoService : PhotoService {
    private data class PhotoEntry(var metadata: PhotoMetadata, var bytes: ByteArray? = null)

    private val photos = ConcurrentHashMap<UUID, PhotoEntry>()

    override suspend fun createUpload(userId: UUID, deviceId: UUID, request: PhotoUploadRequest): PhotoUploadPlan {
        val now = Instant.now()
        val id = UUID.randomUUID()
        val storageKey = "photos/$userId/$id/${request.fileName}"
        val metadata = PhotoMetadata(
            id = id,
            userId = userId,
            deviceId = deviceId,
            fileName = request.fileName,
            mimeType = request.mimeType,
            sizeBytes = request.sizeBytes,
            uploadedAt = null,
            updatedAt = now,
            deletedAt = null,
            serverVersion = now.toEpochMilli(),
            storageKey = storageKey,
            thumbnailStorageKey = null,
        )
        photos[id] = PhotoEntry(metadata)

        return PhotoUploadPlan(
            photo = metadata,
            upload = PresignedObject(url = "https://example.com/upload/$storageKey"),
        )
    }

    override suspend fun uploadPhoto(
        userId: UUID,
        deviceId: UUID,
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        metadata: PhotoMetadataRequest?
    ): PhotoRecord {
        val now = Instant.now()
        val id = metadata?.id ?: UUID.randomUUID()
        val storageKey = "photos/$userId/$id/$fileName"
        val photoMetadata = PhotoMetadata(
            id = id,
            userId = userId,
            deviceId = deviceId,
            fileName = fileName,
            mimeType = mimeType,
            sizeBytes = fileBytes.size.toLong(),
            uploadedAt = now,
            updatedAt = now,
            deletedAt = null,
            serverVersion = now.toEpochMilli(),
            storageKey = storageKey,
            thumbnailStorageKey = null,
        )

        photos[id] = PhotoEntry(photoMetadata, fileBytes)
        return PhotoRecord(photo = photoMetadata, download = null, thumbnailUrl = null)
    }

    override suspend fun confirmUpload(photoId: UUID, userId: UUID): PhotoRecord {
        val entry = photos[photoId]?.takeIf { it.metadata.userId == userId }
            ?: throw IllegalArgumentException("Photo not found for user")
        val now = Instant.now()
        entry.metadata = entry.metadata.copy(uploadedAt = now, updatedAt = now, serverVersion = now.toEpochMilli())
        return PhotoRecord(photo = entry.metadata, download = null, thumbnailUrl = null)
    }

    override suspend fun fetchMetadata(userId: UUID, updatedAfter: Instant?, limit: Int): List<PhotoRecord> {
        return photos.values
            .map { it.metadata }
            .filter { it.userId == userId }
            .filter { updatedAfter == null || it.updatedAt >= updatedAfter }
            .sortedByDescending { it.updatedAt }
            .take(limit)
            .map { PhotoRecord(photo = it, download = null, thumbnailUrl = null) }
    }

    override suspend fun getPhoto(photoId: UUID, userId: UUID): PhotoRecord {
        val entry = photos[photoId]?.takeIf { it.metadata.userId == userId }
            ?: throw IllegalArgumentException("Photo not found for user")
        return PhotoRecord(photo = entry.metadata, download = null, thumbnailUrl = null)
    }

    override suspend fun deletePhoto(photoId: UUID, userId: UUID) {
        val entry = photos[photoId]?.takeIf { it.metadata.userId == userId }
            ?: return
        val now = Instant.now()
        entry.metadata = entry.metadata.copy(deletedAt = now, updatedAt = now, serverVersion = now.toEpochMilli())
    }

    override suspend fun cleanupOrphanedBlobs() {
        // no-op for stub implementation
    }
}

class StubSettingsService : SettingsService {
    override suspend fun getSettings(userId: UUID): UserSettings = UserSettings(
        userId = userId,
        preferences = emptyMap(),
        updatedAt = Instant.now(),
        serverVersion = Instant.now().toEpochMilli(),
    )

    override suspend fun updateSettings(request: SettingsUpdateRequest): UserSettings = UserSettings(
        userId = request.userId,
        preferences = request.preferences,
        updatedAt = Instant.now(),
        serverVersion = Instant.now().toEpochMilli(),
    )
}

class StubExportService : ExportService {
    private val jobs = ConcurrentHashMap<UUID, ExportJob>()

    override suspend fun requestExport(request: ExportRequest): ExportJob {
        val job = ExportJob(
            id = UUID.randomUUID(),
            userId = request.userId,
            deviceId = request.deviceId,
            status = ExportStatus.PENDING,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            downloadUrl = null,
            expiresAt = null,
            fileSize = null,
        )
        jobs[job.id] = job
        return job
    }

    override suspend fun getStatus(exportId: UUID, userId: UUID): ExportJob =
        jobs[exportId]?.takeIf { it.userId == userId }
            ?: throw IllegalArgumentException("export not found")
}
