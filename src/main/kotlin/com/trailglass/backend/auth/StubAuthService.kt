package com.trailglass.backend.auth

import com.trailglass.backend.common.UserProfile
import java.time.Instant
import java.util.UUID

class StubAuthService(private val jwtProvider: JwtProvider) : AuthService {
    override suspend fun register(request: RegisterRequest): AuthSession {
        val userId = UUID.randomUUID()
        val tokens = jwtProvider.issueTokens(userId, request.email, request.deviceInfo.deviceId)
        val profile = UserProfile(
            userId = userId,
            email = request.email,
            displayName = request.displayName,
            createdAt = Instant.now(),
        )

        return AuthSession(
            user = profile,
            tokens = tokens,
            deviceId = request.deviceInfo.deviceId,
            lastSyncTimestamp = null,
        )
    }

    override suspend fun login(request: LoginRequest): AuthSession {
        val userId = UUID.randomUUID()
        val tokens = jwtProvider.issueTokens(userId, request.email, request.deviceInfo.deviceId)
        val profile = UserProfile(
            userId = userId,
            email = request.email,
            displayName = request.email.substringBefore("@"),
            createdAt = Instant.now(),
        )

        return AuthSession(
            user = profile,
            tokens = tokens,
            deviceId = request.deviceInfo.deviceId,
            lastSyncTimestamp = Instant.now(),
        )
    }

    override suspend fun refresh(request: RefreshRequest) = jwtProvider.issueTokens(
        userId = UUID.randomUUID(),
        email = "user@example.com",
        deviceId = request.deviceId,
    )

    override suspend fun logout(refreshToken: String, deviceId: UUID) {
        // Stub: no-op until persistence is implemented
    }

    override suspend fun requestPasswordReset(email: String) {
        // Stub: would send email in a real implementation
    }

    override suspend fun resetPassword(token: String, newPassword: String) {
        // Stub: validate token and update hashed password in persistence
    }
}
