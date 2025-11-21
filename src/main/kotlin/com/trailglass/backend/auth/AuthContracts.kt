@file:UseSerializers(UUIDSerializer::class, InstantSerializer::class)

package com.trailglass.backend.auth

import com.trailglass.backend.common.AuthTokens
import com.trailglass.backend.common.DeviceInfo
import com.trailglass.backend.common.UserProfile
import com.trailglass.backend.common.InstantSerializer
import com.trailglass.backend.common.UUIDSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Instant
import java.util.UUID

interface AuthService {
    suspend fun register(request: RegisterRequest): AuthSession
    suspend fun login(request: LoginRequest): AuthSession
    suspend fun refresh(request: RefreshRequest): AuthTokens
    suspend fun logout(refreshToken: String, deviceId: UUID)
    suspend fun requestPasswordReset(email: String)
    suspend fun resetPassword(token: String, newPassword: String)
}

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String,
    val deviceInfo: DeviceInfo
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val deviceInfo: DeviceInfo
)

@Serializable
data class RefreshRequest(
    val refreshToken: String,
    val deviceId: UUID
)

@Serializable
data class AuthSession(
    val user: UserProfile,
    val tokens: AuthTokens,
    val deviceId: UUID,
    val lastSyncTimestamp: Instant?
)

@Serializable
data class PasswordResetRequest(
    val email: String,
)

@Serializable
data class PasswordResetConfirm(
    val token: String,
    val newPassword: String,
)
