package com.trailglass.backend.auth

import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryPasswordResetTokenRepository : PasswordResetTokenRepository {
    private val tokens = ConcurrentHashMap<String, PasswordResetToken>()

    override suspend fun create(userId: UUID, token: String, expiresAt: Instant): UUID {
        val tokenId = UUID.randomUUID()
        val resetToken = PasswordResetToken(
            id = tokenId,
            userId = userId,
            token = token,
            expiresAt = expiresAt,
            createdAt = Instant.now(),
            consumedAt = null
        )
        tokens[token] = resetToken
        return tokenId
    }

    override suspend fun findByToken(token: String): PasswordResetToken? {
        return tokens[token]
    }

    override suspend fun markAsConsumed(tokenId: UUID): Boolean {
        tokens.values.find { it.id == tokenId }?.let { token ->
            tokens[token.token] = token.copy(consumedAt = Instant.now())
            return true
        }
        return false
    }

    override suspend fun deleteExpired(): Int {
        val now = Instant.now()
        val expiredTokens = tokens.values.filter { it.isExpired(now) && it.isConsumed() }
        expiredTokens.forEach { tokens.remove(it.token) }
        return expiredTokens.size
    }
}
