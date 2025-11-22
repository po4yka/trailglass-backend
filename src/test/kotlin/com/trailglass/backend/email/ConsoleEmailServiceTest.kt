package com.trailglass.backend.email

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertTrue

class ConsoleEmailServiceTest {

    private val emailService = ConsoleEmailService()

    @Test
    fun `sendPasswordResetEmail should return true`() = runTest {
        val result = emailService.sendPasswordResetEmail(
            email = "test@example.com",
            resetToken = "test-token-123",
            resetUrl = "https://app.trailglass.com/reset-password?token=test-token-123"
        )

        assertTrue(result, "ConsoleEmailService should always return true")
    }

    @Test
    fun `sendExportReadyEmail should return true`() = runTest {
        val expiresAt = Instant.now().plusSeconds(3600)

        val result = emailService.sendExportReadyEmail(
            email = "test@example.com",
            exportUrl = "https://app.trailglass.com/exports/test-export.zip",
            expiresAt = expiresAt
        )

        assertTrue(result, "ConsoleEmailService should always return true")
    }

    @Test
    fun `sendPasswordResetEmail should handle special characters in email`() = runTest {
        val result = emailService.sendPasswordResetEmail(
            email = "test+special@example.co.uk",
            resetToken = "test-token-456",
            resetUrl = "https://app.trailglass.com/reset-password?token=test-token-456&redirect=/dashboard"
        )

        assertTrue(result, "ConsoleEmailService should handle special characters")
    }
}
