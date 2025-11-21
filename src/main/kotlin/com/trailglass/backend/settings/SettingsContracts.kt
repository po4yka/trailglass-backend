@file:UseSerializers(UUIDSerializer::class, InstantSerializer::class)

package com.trailglass.backend.settings

import com.trailglass.backend.common.InstantSerializer
import com.trailglass.backend.common.UUIDSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
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
