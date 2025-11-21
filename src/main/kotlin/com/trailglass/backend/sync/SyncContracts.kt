@file:UseSerializers(UUIDSerializer::class, InstantSerializer::class)

package com.trailglass.backend.sync

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Instant
import java.util.UUID
import com.trailglass.backend.common.UUIDSerializer
import com.trailglass.backend.common.InstantSerializer

interface SyncService {
    suspend fun getStatus(deviceId: UUID, userId: UUID): SyncStatus
    suspend fun applyDelta(request: SyncDeltaRequest): SyncDeltaResponse
    suspend fun resolveConflict(request: ConflictResolutionRequest): ConflictResolutionResult
}

@Serializable
data class SyncStatus(
    val deviceId: UUID,
    val userId: UUID,
    val latestServerVersion: Long,
    val lastSyncAt: Instant?
)

@Serializable
data class SyncEnvelope(
    val id: UUID,
    val serverVersion: Long,
    val updatedAt: Instant,
    val deletedAt: Instant?,
    val payload: String,
    val isEncrypted: Boolean,
    val deviceId: UUID
)

@Serializable
data class SyncDeltaRequest(
    val userId: UUID,
    val deviceId: UUID,
    val sinceVersion: Long,
    val incoming: List<SyncEnvelope>
)

@Serializable
data class SyncDeltaResponse(
    val serverVersion: Long,
    val applied: List<SyncEnvelope>,
    val conflicts: List<SyncConflict>,
    val outbound: List<SyncEnvelope>
)

@Serializable
data class SyncConflict(
    val entityId: UUID,
    val serverVersion: Long,
    val deviceVersion: Long,
    val serverPayload: String,
    val devicePayload: String,
    val isEncrypted: Boolean
)

@Serializable
data class ConflictResolutionRequest(
    val conflictId: UUID,
    val entityId: UUID,
    val chosenPayload: String,
    val isEncrypted: Boolean,
    val userId: UUID,
    val deviceId: UUID
)

@Serializable
data class ConflictResolutionResult(
    val entityId: UUID,
    val serverVersion: Long,
    val resolvedAt: Instant
)
