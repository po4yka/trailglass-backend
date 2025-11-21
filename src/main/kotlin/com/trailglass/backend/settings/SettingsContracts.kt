package com.trailglass.backend.settings

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

interface SettingsService {
    suspend fun getSettings(userId: UUID): UserSettings
    suspend fun updateSettings(request: SettingsUpdateRequest): UserSettings
}

@Serializable
data class UserSettings(
    val userId: UUID,
    val preferences: Map<String, String>,
    val updatedAt: Instant,
    val serverVersion: Long
)

@Serializable
data class SettingsUpdateRequest(
    val userId: UUID,
    val deviceId: UUID,
    val preferences: Map<String, String>
)
