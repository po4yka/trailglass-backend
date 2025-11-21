package com.trailglass.backend.email

import org.slf4j.LoggerFactory

interface EmailSender {
    val name: String
    suspend fun send(to: String, subject: String, body: String)
}

class LoggingEmailSender : EmailSender {
    override val name: String = "logging"
    private val logger = LoggerFactory.getLogger(LoggingEmailSender::class.java)

    override suspend fun send(to: String, subject: String, body: String) {
        logger.info("[email:{}] to={} subject={} body={}", name, to, subject, body)
    }
}

class SendGridEmailSender : EmailSender {
    override val name: String = "sendgrid"
    private val logger = LoggerFactory.getLogger(SendGridEmailSender::class.java)

    override suspend fun send(to: String, subject: String, body: String) {
        logger.info("[email:{}] to={} subject={} body={}", name, to, subject, body)
    }
}

class SesEmailSender : EmailSender {
    override val name: String = "ses"
    private val logger = LoggerFactory.getLogger(SesEmailSender::class.java)

    override suspend fun send(to: String, subject: String, body: String) {
        logger.info("[email:{}] to={} subject={} body={}", name, to, subject, body)
    }
}

class SmtpEmailSender : EmailSender {
    override val name: String = "smtp"
    private val logger = LoggerFactory.getLogger(SmtpEmailSender::class.java)

    override suspend fun send(to: String, subject: String, body: String) {
        logger.info("[email:{}] to={} subject={} body={}", name, to, subject, body)
    }
}

class DefaultEmailService(private val senders: List<EmailSender>) : EmailService {
    private val logger = LoggerFactory.getLogger(DefaultEmailService::class.java)

    override suspend fun sendPasswordReset(to: String, resetLink: String) {
        dispatch(to, "Reset your password", "Click here to reset your password: $resetLink")
    }

    override suspend fun sendExportReady(to: String, downloadLink: String) {
        dispatch(to, "Your data export is ready", "Download your export: $downloadLink")
    }

    private suspend fun dispatch(to: String, subject: String, body: String) {
        val iterator = senders.iterator()
        var lastError: Exception? = null

        while (iterator.hasNext()) {
            val sender = iterator.next()
            try {
                sender.send(to, subject, body)
                return
            } catch (ex: Exception) {
                lastError = ex
                logger.warn("Email sender {} failed, attempting fallback", sender.name, ex)
            }
        }

        if (lastError != null) {
            throw lastError
        }
    }
}
