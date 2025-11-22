package com.trailglass.backend.auth

import org.slf4j.LoggerFactory

class PasswordResetTokenCleanupJob(
    private val repository: PasswordResetTokenRepository
) {
    private val logger = LoggerFactory.getLogger(PasswordResetTokenCleanupJob::class.java)

    suspend fun cleanupExpired() {
        val deleted = repository.deleteExpired()
        if (deleted > 0) {
            logger.info("Cleaned up {} expired password reset tokens", deleted)
        }
    }
}
