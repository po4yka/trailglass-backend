package com.trailglass.backend.email

import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

/**
 * Email service implementation using SMTP.
 * Supports TLS/SSL and sends both HTML and plain text versions of emails.
 */
class SmtpEmailService(
    private val host: String,
    private val port: Int,
    private val username: String,
    private val password: String,
    private val fromEmail: String,
    private val fromName: String,
    private val useTls: Boolean
) : EmailService {
    private val logger = LoggerFactory.getLogger(SmtpEmailService::class.java)

    private val session: Session by lazy {
        val props = Properties().apply {
            put("mail.smtp.host", host)
            put("mail.smtp.port", port.toString())
            put("mail.smtp.auth", "true")

            if (useTls) {
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.starttls.required", "true")
            }

            // Security settings
            put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3")
            put("mail.smtp.connectiontimeout", "10000")
            put("mail.smtp.timeout", "10000")
            put("mail.smtp.writetimeout", "10000")
        }

        Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(username, password)
            }
        })
    }

    override suspend fun sendPasswordResetEmail(
        email: String,
        resetToken: String,
        resetUrl: String
    ): Boolean {
        return try {
            logger.info("Sending password reset email to: $email")

            val htmlBody = EmailTemplates.passwordResetEmail(resetUrl)
            val plainTextBody = EmailTemplates.passwordResetEmailPlainText(resetUrl)

            sendEmail(
                to = email,
                subject = "Reset Your Password - Trailglass",
                htmlBody = htmlBody,
                plainTextBody = plainTextBody
            )

            logger.info("Password reset email sent successfully to: $email")
            true
        } catch (e: Exception) {
            logger.error("Failed to send password reset email to: $email", e)
            false
        }
    }

    override suspend fun sendExportReadyEmail(
        email: String,
        exportUrl: String,
        expiresAt: Instant
    ): Boolean {
        return try {
            logger.info("Sending export ready email to: $email")

            val htmlBody = EmailTemplates.exportReadyEmail(exportUrl, expiresAt)
            val plainTextBody = EmailTemplates.exportReadyEmailPlainText(exportUrl, expiresAt)

            sendEmail(
                to = email,
                subject = "Your Export is Ready - Trailglass",
                htmlBody = htmlBody,
                plainTextBody = plainTextBody
            )

            logger.info("Export ready email sent successfully to: $email")
            true
        } catch (e: Exception) {
            logger.error("Failed to send export ready email to: $email", e)
            false
        }
    }

    /**
     * Sends an email with both HTML and plain text alternatives.
     * This ensures maximum compatibility across email clients.
     */
    private suspend fun sendEmail(
        to: String,
        subject: String,
        htmlBody: String,
        plainTextBody: String
    ) = withContext(Dispatchers.IO) {
        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(fromEmail, fromName))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
            setSubject(subject)
            sentDate = Date()

            // Create multipart message with both plain text and HTML
            val multipart = MimeMultipart("alternative")

            // Add plain text version (should be first for fallback)
            val textPart = MimeBodyPart().apply {
                setText(plainTextBody, "UTF-8")
            }
            multipart.addBodyPart(textPart)

            // Add HTML version (preferred if client supports it)
            val htmlPart = MimeBodyPart().apply {
                setContent(htmlBody, "text/html; charset=utf-8")
            }
            multipart.addBodyPart(htmlPart)

            setContent(multipart)
        }

        Transport.send(message)
        logger.debug("Email sent successfully - To: $to, Subject: $subject")
    }
}
