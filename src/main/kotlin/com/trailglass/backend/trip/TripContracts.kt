package com.trailglass.backend.trip

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

interface TripService {
    suspend fun upsertTrip(request: TripUpsertRequest): TripRecord
    suspend fun listTrips(userId: UUID, updatedAfter: Instant?, limit: Int = 100): List<TripRecord>
    suspend fun deleteTrip(userId: UUID, tripId: UUID): TripRecord
}

@Serializable
data class TripRecord(
    val id: UUID,
    val userId: UUID,
    val deviceId: UUID,
    val name: String,
    val startDate: Instant?,
    val endDate: Instant?,
    val updatedAt: Instant,
    val deletedAt: Instant?,
    val serverVersion: Long
)

@Serializable
data class TripUpsertRequest(
    val trip: TripRecord
)
