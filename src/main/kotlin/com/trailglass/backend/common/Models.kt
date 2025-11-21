@file:UseSerializers(UUIDSerializer::class, InstantSerializer::class)

package com.trailglass.backend.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Instant
import java.util.UUID

@Serializable
data class DeviceInfo(
    val deviceId: UUID,
    val deviceName: String,
    val platform: String,
    val osVersion: String,
    val appVersion: String
)

@Serializable
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long
)

@Serializable
data class UserProfile(
    val userId: UUID,
    val email: String,
    val displayName: String,
    val createdAt: Instant
)
