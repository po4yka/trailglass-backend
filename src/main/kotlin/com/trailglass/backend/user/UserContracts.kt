package com.trailglass.backend.user

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

interface UserProfileService {
    suspend fun upsertProfile(profile: UserProfile): UserProfile
    suspend fun listDevices(userId: UUID, updatedAfter: Instant?, limit: Int = 100): List<DeviceProfile>
    suspend fun registerDevice(device: DeviceProfile): DeviceProfile
    suspend fun deleteDevice(userId: UUID, deviceId: UUID): DeviceProfile?
}

@Serializable
data class UserProfile(
    val id: UUID,
    val email: String,
    val displayName: String,
    val updatedAt: Instant,
    val deletedAt: Instant?,
    val serverVersion: Long,
)

@Serializable
data class DeviceProfile(
    val id: UUID,
    val userId: UUID,
    val deviceName: String,
    val platform: String,
    val osVersion: String,
    val appVersion: String,
    val updatedAt: Instant,
    val deletedAt: Instant?,
    val serverVersion: Long,
)
