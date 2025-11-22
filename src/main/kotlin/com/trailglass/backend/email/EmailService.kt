package com.trailglass.backend.email

import java.time.Instant

/**
 * Service for sending transactional emails.
 * Implementations can use SMTP, console logging, or cloud email services.
 */
interface EmailService {
    /**
     * Sends a password reset email to the user.
     *
     * @param email The recipient's email address
     * @param resetToken The password reset token
     * @param resetUrl The full URL for resetting the password (including the token)
     * @return true if email was sent successfully, false otherwise
     */
    suspend fun sendPasswordResetEmail(
        email: String,
        resetToken: String,
        resetUrl: String
    ): Boolean

    /**
     * Sends an email notification when an export is ready for download.
     * This method is optional and reserved for future use.
     *
     * @param email The recipient's email address
     * @param exportUrl The URL where the export can be downloaded
     * @param expiresAt When the export link expires
     * @return true if email was sent successfully, false otherwise
     */
    suspend fun sendExportReadyEmail(
        email: String,
        exportUrl: String,
        expiresAt: Instant
    ): Boolean {
        // Default implementation does nothing
        return false
    }
}
