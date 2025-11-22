package com.trailglass.backend.email

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.time.Instant

/**
 * Tests for SmtpEmailService.
 * These tests require a real SMTP server and are disabled by default.
 * To enable them, set the environment variable SMTP_TEST_ENABLED=true
 * and provide valid SMTP credentials.
 */
class SmtpEmailServiceTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "SMTP_TEST_ENABLED", matches = "true")
    fun `sendPasswordResetEmail should send email via SMTP`() = runTest {
        val emailService = createTestEmailService()

        val result = emailService.sendPasswordResetEmail(
            email = System.getenv("SMTP_TEST_TO_EMAIL") ?: "test@example.com",
            resetToken = "test-token-123",
            resetUrl = "https://app.trailglass.com/reset-password?token=test-token-123"
        )

        // Note: This test requires valid SMTP credentials and may fail in CI
        // The result depends on the SMTP server configuration
        println("Email send result: $result")
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "SMTP_TEST_ENABLED", matches = "true")
    fun `sendExportReadyEmail should send email via SMTP`() = runTest {
        val emailService = createTestEmailService()
        val expiresAt = Instant.now().plusSeconds(3600)

        val result = emailService.sendExportReadyEmail(
            email = System.getenv("SMTP_TEST_TO_EMAIL") ?: "test@example.com",
            exportUrl = "https://app.trailglass.com/exports/test-export.zip",
            expiresAt = expiresAt
        )

        // Note: This test requires valid SMTP credentials and may fail in CI
        println("Email send result: $result")
    }

    private fun createTestEmailService(): SmtpEmailService {
        return SmtpEmailService(
            host = System.getenv("SMTP_HOST") ?: "smtp.example.com",
            port = System.getenv("SMTP_PORT")?.toIntOrNull() ?: 587,
            username = System.getenv("SMTP_USERNAME") ?: "test@example.com",
            password = System.getenv("SMTP_PASSWORD") ?: "password",
            fromEmail = System.getenv("SMTP_FROM_EMAIL") ?: "noreply@trailglass.com",
            fromName = System.getenv("SMTP_FROM_NAME") ?: "Trailglass",
            useTls = System.getenv("SMTP_TLS_ENABLED")?.toBoolean() ?: true
        )
    }
}
