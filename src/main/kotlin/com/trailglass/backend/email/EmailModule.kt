package com.trailglass.backend.email

import com.trailglass.backend.config.AppConfig
import com.trailglass.backend.config.EmailProvider
import org.koin.dsl.module
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("EmailModule")

val emailModule = module {
    single<EmailService> {
        val config = get<AppConfig>()
        createEmailService(config)
    }
}

/**
 * Creates the appropriate EmailService implementation based on configuration.
 */
private fun createEmailService(config: AppConfig): EmailService {
    if (!config.email.enabled) {
        logger.info("Email sending is disabled (EMAIL_ENABLED=false). Using ConsoleEmailService.")
        return ConsoleEmailService()
    }

    return when (config.email.provider) {
        EmailProvider.SMTP -> {
            val smtp = config.email.smtp
                ?: error("SMTP configuration is required when EMAIL_PROVIDER=smtp")

            logger.info(
                "Initializing SMTP email service: host=${smtp.host}, port=${smtp.port}, " +
                        "from=${smtp.fromEmail}, tls=${smtp.useTls}"
            )

            SmtpEmailService(
                host = smtp.host,
                port = smtp.port,
                username = smtp.username,
                password = smtp.password,
                fromEmail = smtp.fromEmail,
                fromName = smtp.fromName,
                useTls = smtp.useTls
            )
        }

        EmailProvider.CONSOLE -> {
            logger.info("Using ConsoleEmailService (EMAIL_PROVIDER=console)")
            ConsoleEmailService()
        }

        EmailProvider.SENDGRID -> {
            logger.warn("SendGrid email provider is not yet implemented. Falling back to ConsoleEmailService.")
            ConsoleEmailService()
        }

        EmailProvider.SES -> {
            logger.warn("AWS SES email provider is not yet implemented. Falling back to ConsoleEmailService.")
            ConsoleEmailService()
        }
    }
}
