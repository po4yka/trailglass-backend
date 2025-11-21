package com.trailglass.backend.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.trailglass.backend.common.AuthTokens
import com.trailglass.backend.config.AppConfig
import java.time.Instant
import java.util.Date
import java.util.UUID

class JwtProvider(private val config: AppConfig) {
    private val algorithm: Algorithm = Algorithm.HMAC256(requireNotNull(config.jwtSecret))

    private val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withAudience(config.jwtAudience)
        .withIssuer(config.jwtIssuer)
        .build()

    fun issueTokens(
        userId: UUID,
        email: String,
        deviceId: UUID,
        expiresInSeconds: Long = DEFAULT_ACCESS_TOKEN_EXPIRY_SECONDS,
        refreshExpiresInSeconds: Long = DEFAULT_REFRESH_TOKEN_EXPIRY_SECONDS,
    ): AuthTokens {
        val now = Instant.now()
        val accessToken = JWT.create()
            .withSubject(userId.toString())
            .withIssuer(config.jwtIssuer)
            .withAudience(config.jwtAudience)
            .withClaim("email", email)
            .withClaim("deviceId", deviceId.toString())
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(now.plusSeconds(expiresInSeconds)))
            .sign(algorithm)

        val refreshToken = JWT.create()
            .withSubject(userId.toString())
            .withIssuer(config.jwtIssuer)
            .withAudience(config.jwtAudience)
            .withClaim("type", "refresh")
            .withClaim("deviceId", deviceId.toString())
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(now.plusSeconds(refreshExpiresInSeconds)))
            .sign(algorithm)

        return AuthTokens(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresInSeconds = expiresInSeconds,
        )
    }

    fun verifier(): JWTVerifier = verifier

    val audience: String
        get() = config.jwtAudience

    companion object {
        const val DEFAULT_ACCESS_TOKEN_EXPIRY_SECONDS = 3600L
        const val DEFAULT_REFRESH_TOKEN_EXPIRY_SECONDS = 60L * 60L * 24L * 30L
    }
}
