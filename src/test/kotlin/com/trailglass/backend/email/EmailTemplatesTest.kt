package com.trailglass.backend.email

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertContains
import kotlin.test.assertTrue

class EmailTemplatesTest {

    @Test
    fun `passwordResetEmail should contain reset URL`() {
        val resetUrl = "https://app.trailglass.com/reset-password?token=abc123"
        val html = EmailTemplates.passwordResetEmail(resetUrl)

        assertContains(html, resetUrl, "HTML should contain reset URL")
        assertContains(html, "Reset Your Password", "HTML should contain title")
        assertContains(html, "Reset Password", "HTML should contain button text")
        assertContains(html, "<!DOCTYPE html>", "HTML should be a complete document")
    }

    @Test
    fun `passwordResetEmail should escape special characters in URL`() {
        val resetUrl = "https://app.trailglass.com/reset?token=abc&redirect=/dashboard"
        val html = EmailTemplates.passwordResetEmail(resetUrl)

        assertContains(html, resetUrl, "HTML should contain the URL with special characters")
    }

    @Test
    fun `passwordResetEmailPlainText should contain reset URL`() {
        val resetUrl = "https://app.trailglass.com/reset-password?token=abc123"
        val text = EmailTemplates.passwordResetEmailPlainText(resetUrl)

        assertContains(text, resetUrl, "Plain text should contain reset URL")
        assertContains(text, "Reset Your Password", "Plain text should contain title")
        assertTrue(text.lines().isNotEmpty(), "Plain text should have multiple lines")
    }

    @Test
    fun `exportReadyEmail should contain export URL and expiry date`() {
        val exportUrl = "https://app.trailglass.com/exports/test.zip"
        val expiresAt = Instant.parse("2025-12-31T23:59:59Z")
        val html = EmailTemplates.exportReadyEmail(exportUrl, expiresAt)

        assertContains(html, exportUrl, "HTML should contain export URL")
        assertContains(html, "Your Export is Ready", "HTML should contain title")
        assertContains(html, "Download Export", "HTML should contain button text")
        assertContains(html, "<!DOCTYPE html>", "HTML should be a complete document")
    }

    @Test
    fun `exportReadyEmailPlainText should contain export URL and expiry date`() {
        val exportUrl = "https://app.trailglass.com/exports/test.zip"
        val expiresAt = Instant.parse("2025-12-31T23:59:59Z")
        val text = EmailTemplates.exportReadyEmailPlainText(exportUrl, expiresAt)

        assertContains(text, exportUrl, "Plain text should contain export URL")
        assertContains(text, "Your Export is Ready", "Plain text should contain title")
        assertTrue(text.lines().isNotEmpty(), "Plain text should have multiple lines")
    }

    @Test
    fun `HTML templates should have inline CSS for email client compatibility`() {
        val resetUrl = "https://app.trailglass.com/reset-password?token=abc123"
        val html = EmailTemplates.passwordResetEmail(resetUrl)

        assertContains(html, "style=", "HTML should contain inline styles")
        assertTrue(html.contains("padding:") || html.contains("padding :"), "Should have padding styles")
        assertTrue(html.contains("color:") || html.contains("color :"), "Should have color styles")
    }

    @Test
    fun `HTML templates should use table-based layout for email compatibility`() {
        val resetUrl = "https://app.trailglass.com/reset-password?token=abc123"
        val html = EmailTemplates.passwordResetEmail(resetUrl)

        assertContains(html, "<table", "HTML should use tables for layout")
        assertContains(html, "role=\"presentation\"", "Tables should have presentation role")
    }
}
