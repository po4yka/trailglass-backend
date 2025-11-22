package com.trailglass.backend.visit

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

interface PlaceVisitService {
    suspend fun upsertVisits(visits: List<PlaceVisit>): VisitBatchResult
    suspend fun updateVisit(userId: UUID, visitId: UUID, request: PlaceVisitUpdateRequest, expectedVersion: Long?): PlaceVisit
    suspend fun listVisits(
        userId: UUID,
        updatedAfter: Instant? = null,
        startTime: Instant? = null,
        endTime: Instant? = null,
        category: String? = null,
        isFavorite: Boolean? = null,
        limit: Int = 200,
        offset: Int = 0
    ): List<PlaceVisit>
    suspend fun getVisit(userId: UUID, visitId: UUID): PlaceVisit
    suspend fun deleteVisits(userId: UUID, ids: List<UUID>): VisitBatchResult
}

@Serializable
data class PlaceVisit(
    val id: UUID,
    val userId: UUID,
    val deviceId: UUID,
    val latitude: Double,
    val longitude: Double,
    val arrivedAt: Instant,
    val departedAt: Instant?,
    val category: String? = null,
    val isFavorite: Boolean = false,
    val updatedAt: Instant,
    val deletedAt: Instant?,
    val serverVersion: Long,
)

@Serializable
data class VisitBatchResult(
    val appliedCount: Int,
    val serverVersion: Long,
)

@Serializable
data class PlaceVisitUpdateRequest(
    val placeName: String? = null,
    val notes: String? = null,
    val isFavorite: Boolean? = null,
    val category: String? = null,
    val arrivedAt: Instant? = null,
    val departedAt: Instant? = null,
    val expectedVersion: Long? = null,
)
