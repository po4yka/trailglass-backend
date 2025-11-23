package com.trailglass.backend.auth

import com.auth0.jwt.JWT
import com.trailglass.backend.common.AuthTokens
import com.trailglass.backend.common.UserProfile
import com.trailglass.backend.plugins.ApiException
import com.trailglass.backend.scheduler.RecurringTaskScheduler
import io.ktor.http.HttpStatusCode
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import de.mkammerer.argon2.Argon2
import de.mkammerer.argon2.Argon2Factory

class DefaultAuthService(
    private val jwtProvider: JwtProvider,
    private val emailService: com.trailglass.backend.email.EmailService,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val userRepository: UserRepository,
    private val argon2Config: com.trailglass.backend.config.Argon2Config,
    private val clock: Clock = Clock.systemUTC(),
    private val refreshTokenTtlSeconds: Long = JwtProvider.DEFAULT_REFRESH_TOKEN_EXPIRY_SECONDS,
    private val passwordResetTokenTtl: Duration = Duration.ofHours(1),
    private val argon2: Argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id),
) : AuthService {

    private val refreshTokens = ConcurrentHashMap<String, StoredRefreshToken>()

    override suspend fun register(request: RegisterRequest): AuthSession {
        val existing = userRepository.findByEmail(request.email.lowercase())
        if (existing != null) {
            throw ApiException(HttpStatusCode.Conflict, "user_exists", "User already registered")
        }

        val newUser = User(
            id = UUID.randomUUID(),
            email = request.email.lowercase(),
            displayName = request.displayName,
            passwordHash = hashPassword(request.password),
            createdAt = clock.instant(),
            lastSyncTimestamp = null,
        )

        val user = userRepository.create(newUser)

        return buildSession(user, request.deviceInfo.deviceId)
    }

    override suspend fun login(request: LoginRequest): AuthSession {
        val user = userRepository.findByEmail(request.email.lowercase())
            ?: throw ApiException(HttpStatusCode.Unauthorized, "invalid_credentials", "Invalid email or password")

        val passwordMatches = verifyPassword(request.password, user.passwordHash)
        if (!passwordMatches) {
            throw ApiException(HttpStatusCode.Unauthorized, "invalid_credentials", "Invalid email or password")
        }

        return buildSession(user, request.deviceInfo.deviceId, user.lastSyncTimestamp)
    }

    override suspend fun refresh(request: RefreshRequest): AuthTokens {
        val decoded = try {
            jwtProvider.verifier().verify(request.refreshToken)
        } catch (ex: Exception) {
            invalidateStoredToken(request.refreshToken)
            throw ApiException(HttpStatusCode.Unauthorized, "invalid_refresh_token", "Invalid or expired refresh token")
        }

        if (decoded.getClaim("type").asString() != "refresh") {
            throw ApiException(HttpStatusCode.BadRequest, "invalid_token_type", "Expected refresh token")
        }

        val tokenDeviceId = decoded.getClaim("deviceId").asString()?.let(UUID::fromString)
        if (tokenDeviceId == null || tokenDeviceId != request.deviceId) {
            invalidateStoredToken(request.refreshToken)
            throw ApiException(HttpStatusCode.Unauthorized, "device_mismatch", "Token is bound to a different device")
        }

        val stored = refreshTokens[request.refreshToken]
        if (stored == null || stored.isExpired(clock.instant())) {
            invalidateStoredToken(request.refreshToken)
            throw ApiException(HttpStatusCode.Unauthorized, "invalid_refresh_token", "Invalid or expired refresh token")
        }

        val user = userRepository.findById(stored.userId)
            ?: throw ApiException(HttpStatusCode.Unauthorized, "invalid_refresh_token", "User no longer exists")

        invalidateStoredToken(request.refreshToken)
        return issueTokens(user, stored.deviceId)
    }

    override suspend fun logout(refreshToken: String, deviceId: UUID) {
        refreshTokens.entries
            .filter { it.value.deviceId == deviceId }
            .map { it.key }
            .forEach { refreshTokens.remove(it) }
    }

    override suspend fun requestPasswordReset(email: String) {
        val user = userRepository.findByEmail(email.lowercase()) ?: return
        val token = UUID.randomUUID().toString()
        val expiresAt = clock.instant().plus(passwordResetTokenTtl)

        // Store the token in the database
        passwordResetTokenRepository.create(
            userId = user.id,
            token = token,
            expiresAt = expiresAt
        )

        // Send password reset email asynchronously
        // The reset URL should be constructed by the client/frontend
        // For now, we pass the token and a placeholder URL
        val resetUrl = "https://app.trailglass.com/reset-password?token=$token"

        try {
            emailService.sendPasswordResetEmail(
                email = user.email,
                resetToken = token,
                resetUrl = resetUrl
            )
        } catch (e: Exception) {
            // Log the error but don't fail the request
            // The token is still valid and the user can try again
            org.slf4j.LoggerFactory.getLogger(this::class.java)
                .error("Failed to send password reset email to ${user.email}", e)
        }
    }

    override suspend fun resetPassword(token: String, newPassword: String) {
        // Find the token in the database
        val resetToken = passwordResetTokenRepository.findByToken(token)
            ?: throw ApiException(HttpStatusCode.BadRequest, "invalid_reset_token", "The reset token is invalid or expired")

        // Validate token expiration
        if (resetToken.isExpired(clock.instant())) {
            throw ApiException(HttpStatusCode.BadRequest, "invalid_reset_token", "The reset token is invalid or expired")
        }

        // Validate token hasn't been consumed
        if (resetToken.isConsumed()) {
            throw ApiException(HttpStatusCode.BadRequest, "invalid_reset_token", "The reset token is invalid or expired")
        }

        // Find the user
        val user = userRepository.findById(resetToken.userId)
            ?: throw ApiException(HttpStatusCode.NotFound, "user_not_found", "User not found")

        // Update the password
        userRepository.updatePassword(user.id, hashPassword(newPassword))

        // Mark the token as consumed
        passwordResetTokenRepository.markAsConsumed(resetToken.id)
    }

    private fun buildSession(user: User, deviceId: UUID, lastSync: Instant? = null): AuthSession {
        val tokens = issueTokens(user, deviceId)
        val profile = UserProfile(
            userId = user.id,
            email = user.email,
            displayName = user.displayName,
            createdAt = user.createdAt,
        )

        return AuthSession(
            user = profile,
            tokens = tokens,
            deviceId = deviceId,
            lastSyncTimestamp = lastSync,
        )
    }

    private fun issueTokens(user: User, deviceId: UUID): AuthTokens {
        val tokens = jwtProvider.issueTokens(
            userId = user.id,
            email = user.email,
            deviceId = deviceId,
            refreshExpiresInSeconds = refreshTokenTtlSeconds,
        )

        val expiresAt = JWT.decode(tokens.refreshToken).expiresAt?.toInstant()
            ?: throw ApiException(HttpStatusCode.InternalServerError, "token_error", "Unable to decode refresh token expiry")

        refreshTokens[tokens.refreshToken] = StoredRefreshToken(
            token = tokens.refreshToken,
            userId = user.id,
            deviceId = deviceId,
            expiresAt = expiresAt,
        )

        return tokens
    }

    private fun invalidateStoredToken(refreshToken: String) {
        refreshTokens.remove(refreshToken)
    }

    private fun hashPassword(password: String): String {
        return argon2.hash(argon2Config.iterations, argon2Config.memory, argon2Config.parallelism, password.toCharArray())
    }

    private fun verifyPassword(password: String, hash: String): Boolean {
        return try {
            argon2.verify(hash, password.toCharArray())
        } catch (ex: Exception) {
            false
        }
    }

    fun scheduleTokenCleanup(scheduler: RecurringTaskScheduler, cleanupJob: PasswordResetTokenCleanupJob) {
        scheduler.schedule(
            name = "password-reset-token-cleanup",
            interval = Duration.ofHours(6),
            initialDelay = Duration.ofMinutes(5),
        ) {
            cleanupJob.cleanupExpired()
        }
    }
}

private data class StoredRefreshToken(
    val token: String,
    val userId: UUID,
    val deviceId: UUID,
    val expiresAt: Instant,
) {
    fun isExpired(now: Instant): Boolean = now.isAfter(expiresAt)
}

