@file:UseSerializers(UUIDSerializer::class, InstantSerializer::class)

package com.trailglass.backend.location

import com.trailglass.backend.common.InstantSerializer
import com.trailglass.backend.common.UUIDSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Instant
import java.util.UUID

interface LocationService {
    suspend fun upsertBatch(request: LocationBatchRequest): LocationBatchResult
    suspend fun getLocations(userId: UUID, since: Instant?, limit: Int = 200): List<LocationSample>
    suspend fun getLocation(userId: UUID, locationId: UUID): LocationSample
    suspend fun deleteLocations(userId: UUID, ids: List<UUID>): LocationBatchResult
}

@Serializable
data class LocationSample(
    val id: UUID,
    val userId: UUID,
    val deviceId: UUID,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float?,
    val recordedAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,
    val serverVersion: Long
)

@Serializable
data class LocationBatchRequest(
    val userId: UUID,
    val deviceId: UUID,
    val samples: List<LocationSample>
)

@Serializable
data class LocationBatchResult(
    val appliedCount: Int,
    val serverVersion: Long
)
