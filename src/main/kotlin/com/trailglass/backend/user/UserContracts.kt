package com.trailglass.backend.user

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

interface UserProfileService {
    suspend fun getProfile(userId: UUID): UserProfile?
    suspend fun upsertProfile(profile: UserProfile): UserProfile
    suspend fun getUserStatistics(userId: UUID): UserProfileStatistics
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

@Serializable
data class UserProfileStatistics(
    val totalLocations: Int = 0,
    val totalTrips: Int = 0,
    val totalPlaceVisits: Int = 0,
    val totalPhotos: Int = 0,
    val countriesVisited: Int = 0,
    val totalDistance: Double = 0.0,
)

@Serializable
data class UserProfileResponse(
    val userId: UUID,
    val email: String,
    val displayName: String,
    val profilePhotoUrl: String? = null,
    val createdAt: Instant,
    val statistics: UserProfileStatistics = UserProfileStatistics(),
)
