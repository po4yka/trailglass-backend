package com.trailglass.backend.email

import org.slf4j.LoggerFactory
import java.time.Instant

/**
 * Email service implementation that logs emails to the console.
 * Useful for development and testing environments where actual email sending is not required.
 */
class ConsoleEmailService : EmailService {
    private val logger = LoggerFactory.getLogger(ConsoleEmailService::class.java)

    override suspend fun sendPasswordResetEmail(
        email: String,
        resetToken: String,
        resetUrl: String
    ): Boolean {
        logger.info("=".repeat(80))
        logger.info("EMAIL: Password Reset Request")
        logger.info("To: $email")
        logger.info("Subject: Reset Your Password - Trailglass")
        logger.info("-".repeat(80))
        logger.info("Reset Token: $resetToken")
        logger.info("Reset URL: $resetUrl")
        logger.info("-".repeat(80))
        logger.info("Plain Text Content:")
        logger.info(EmailTemplates.passwordResetEmailPlainText(resetUrl))
        logger.info("=".repeat(80))

        return true
    }

    override suspend fun sendExportReadyEmail(
        email: String,
        exportUrl: String,
        expiresAt: Instant
    ): Boolean {
        logger.info("=".repeat(80))
        logger.info("EMAIL: Export Ready Notification")
        logger.info("To: $email")
        logger.info("Subject: Your Export is Ready - Trailglass")
        logger.info("-".repeat(80))
        logger.info("Export URL: $exportUrl")
        logger.info("Expires At: $expiresAt")
        logger.info("-".repeat(80))
        logger.info("Plain Text Content:")
        logger.info(EmailTemplates.exportReadyEmailPlainText(exportUrl, expiresAt))
        logger.info("=".repeat(80))

        return true
    }
}
