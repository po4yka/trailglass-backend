package com.trailglass.backend.stubs

import com.trailglass.backend.export.ExportJob
import com.trailglass.backend.export.ExportService
import com.trailglass.backend.export.ExportStatus
import com.trailglass.backend.location.LocationBatchRequest
import com.trailglass.backend.location.LocationBatchResult
import com.trailglass.backend.location.LocationSample
import com.trailglass.backend.location.LocationService
import com.trailglass.backend.photo.PhotoMetadata
import com.trailglass.backend.photo.PhotoService
import com.trailglass.backend.photo.PhotoUploadRequest
import com.trailglass.backend.photo.PresignedUpload
import com.trailglass.backend.settings.SettingsService
import com.trailglass.backend.settings.SettingsUpdateRequest
import com.trailglass.backend.settings.UserSettings
import com.trailglass.backend.sync.ConflictResolutionRequest
import com.trailglass.backend.sync.ConflictResolutionResult
import com.trailglass.backend.sync.SyncDeltaRequest
import com.trailglass.backend.sync.SyncDeltaResponse
import com.trailglass.backend.sync.SyncEnvelope
import com.trailglass.backend.sync.SyncService
import com.trailglass.backend.sync.SyncStatus
import com.trailglass.backend.trip.TripRecord
import com.trailglass.backend.trip.TripService
import com.trailglass.backend.trip.TripUpsertRequest
import java.time.Instant
import java.util.UUID

class StubSyncService : SyncService {
    override suspend fun getStatus(deviceId: UUID, userId: UUID): SyncStatus = SyncStatus(
        deviceId = deviceId,
        userId = userId,
        latestServerVersion = 0,
        lastSyncAt = null,
    )

    override suspend fun applyDelta(request: SyncDeltaRequest): SyncDeltaResponse = SyncDeltaResponse(
        serverVersion = request.sinceVersion + 1,
        applied = emptyList(),
        conflicts = emptyList(),
        outbound = emptyList(),
    )

    override suspend fun resolveConflict(request: ConflictResolutionRequest): ConflictResolutionResult =
        ConflictResolutionResult(
            entityId = request.entityId,
            serverVersion = request.conflictId.mostSignificantBits.toLong().absoluteValue,
            resolvedAt = Instant.now(),
        )
}

class StubLocationService : LocationService {
    override suspend fun upsertBatch(request: LocationBatchRequest): LocationBatchResult = LocationBatchResult(
        appliedCount = request.samples.size,
        serverVersion = Instant.now().epochSecond,
    )

    override suspend fun getLocations(userId: UUID, since: Instant?, limit: Int): List<LocationSample> = emptyList()

    override suspend fun deleteLocations(userId: UUID, ids: List<UUID>): LocationBatchResult = LocationBatchResult(
        appliedCount = ids.size,
        serverVersion = Instant.now().epochSecond,
    )
}

class StubTripService : TripService {
    override suspend fun upsertTrip(request: TripUpsertRequest): TripRecord = request.trip

    override suspend fun listTrips(userId: UUID, updatedAfter: Instant?, limit: Int): List<TripRecord> = emptyList()

    override suspend fun deleteTrip(userId: UUID, tripId: UUID): TripRecord = TripRecord(
        id = tripId,
        userId = userId,
        deviceId = UUID.randomUUID(),
        name = "Deleted Trip",
        startDate = null,
        endDate = null,
        updatedAt = Instant.now(),
        deletedAt = Instant.now(),
        serverVersion = Instant.now().epochSecond,
    )
}

class StubPhotoService : PhotoService {
    override suspend fun requestUpload(request: PhotoUploadRequest): PresignedUpload = PresignedUpload(
        uploadUrl = "https://example.com/upload/${UUID.randomUUID()}",
        photoId = UUID.randomUUID(),
    )

    override suspend fun confirmUpload(photoId: UUID, userId: UUID, deviceId: UUID): PhotoMetadata = PhotoMetadata(
        id = photoId,
        userId = userId,
        deviceId = deviceId,
        fileName = "photo.jpg",
        mimeType = "image/jpeg",
        sizeBytes = 0,
        uploadedAt = Instant.now(),
        updatedAt = Instant.now(),
        deletedAt = null,
        serverVersion = Instant.now().epochSecond,
        storageKey = "photos/$userId/$photoId.jpg",
    )

    override suspend fun fetchMetadata(userId: UUID, updatedAfter: Instant?, limit: Int): List<PhotoMetadata> = emptyList()
}

class StubSettingsService : SettingsService {
    override suspend fun getSettings(userId: UUID): UserSettings = UserSettings(
        userId = userId,
        preferences = emptyMap(),
        updatedAt = Instant.now(),
        serverVersion = Instant.now().epochSecond,
    )

    override suspend fun updateSettings(request: SettingsUpdateRequest): UserSettings = UserSettings(
        userId = request.userId,
        preferences = request.preferences,
        updatedAt = Instant.now(),
        serverVersion = Instant.now().epochSecond,
    )
}

class StubExportService : ExportService {
    override suspend fun requestExport(userId: UUID, deviceId: UUID, email: String?): ExportJob = ExportJob(
        id = UUID.randomUUID(),
        userId = userId,
        deviceId = deviceId,
        status = ExportStatus.PENDING,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        downloadUrl = null,
        expiresAt = null,
    )

    override suspend fun getStatus(exportId: UUID, userId: UUID): ExportJob = ExportJob(
        id = exportId,
        userId = userId,
        deviceId = UUID.randomUUID(),
        status = ExportStatus.PENDING,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        downloadUrl = null,
        expiresAt = null,
    )
}

private val Long.absoluteValue: Long
    get() = if (this < 0) -this else this
