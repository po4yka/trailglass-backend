package com.trailglass.backend.email

import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * HTML email templates for transactional emails.
 * Uses inline CSS for maximum email client compatibility.
 */
object EmailTemplates {

    /**
     * Generates an HTML email for password reset requests.
     *
     * @param resetUrl The full URL where the user can reset their password
     * @return HTML string for the email body
     */
    fun passwordResetEmail(resetUrl: String): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Reset Your Password - Trailglass</title>
</head>
<body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f5f5f5;">
    <table role="presentation" style="width: 100%; border-collapse: collapse;">
        <tr>
            <td style="padding: 40px 0; text-align: center;">
                <table role="presentation" style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                    <!-- Header -->
                    <tr>
                        <td style="padding: 40px 40px 20px; text-align: center; border-bottom: 1px solid #e5e5e5;">
                            <h1 style="margin: 0; font-size: 24px; font-weight: 600; color: #1a1a1a;">Trailglass</h1>
                        </td>
                    </tr>

                    <!-- Content -->
                    <tr>
                        <td style="padding: 40px;">
                            <h2 style="margin: 0 0 20px; font-size: 20px; font-weight: 600; color: #1a1a1a;">Reset Your Password</h2>

                            <p style="margin: 0 0 20px; font-size: 16px; line-height: 24px; color: #404040;">
                                We received a request to reset your password for your Trailglass account.
                                Click the button below to create a new password.
                            </p>

                            <!-- CTA Button -->
                            <table role="presentation" style="margin: 30px 0;">
                                <tr>
                                    <td style="border-radius: 6px; background-color: #007aff;">
                                        <a href="${resetUrl}"
                                           style="display: inline-block; padding: 14px 28px; font-size: 16px; font-weight: 600; color: #ffffff; text-decoration: none; border-radius: 6px;">
                                            Reset Password
                                        </a>
                                    </td>
                                </tr>
                            </table>

                            <p style="margin: 20px 0 0; font-size: 14px; line-height: 20px; color: #666666;">
                                If the button doesn't work, copy and paste this link into your browser:
                            </p>

                            <p style="margin: 10px 0 0; font-size: 14px; line-height: 20px; color: #007aff; word-break: break-all;">
                                ${resetUrl}
                            </p>

                            <div style="margin: 30px 0 0; padding: 20px; background-color: #f9f9f9; border-radius: 6px;">
                                <p style="margin: 0; font-size: 14px; line-height: 20px; color: #666666;">
                                    <strong style="color: #1a1a1a;">Didn't request a password reset?</strong><br>
                                    You can safely ignore this email. Your password will remain unchanged.
                                </p>
                            </div>
                        </td>
                    </tr>

                    <!-- Footer -->
                    <tr>
                        <td style="padding: 20px 40px 40px; text-align: center; border-top: 1px solid #e5e5e5;">
                            <p style="margin: 0; font-size: 12px; line-height: 18px; color: #999999;">
                                This is an automated message from Trailglass. Please do not reply to this email.
                            </p>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</body>
</html>
        """.trimIndent()
    }

    /**
     * Generates an HTML email for export ready notifications.
     *
     * @param exportUrl The URL where the export can be downloaded
     * @param expiresAt When the export link expires
     * @return HTML string for the email body
     */
    fun exportReadyEmail(exportUrl: String, expiresAt: Instant): String {
        val formattedDate = DateTimeFormatter.ISO_INSTANT.format(expiresAt)

        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Your Export is Ready - Trailglass</title>
</head>
<body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f5f5f5;">
    <table role="presentation" style="width: 100%; border-collapse: collapse;">
        <tr>
            <td style="padding: 40px 0; text-align: center;">
                <table role="presentation" style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                    <!-- Header -->
                    <tr>
                        <td style="padding: 40px 40px 20px; text-align: center; border-bottom: 1px solid #e5e5e5;">
                            <h1 style="margin: 0; font-size: 24px; font-weight: 600; color: #1a1a1a;">Trailglass</h1>
                        </td>
                    </tr>

                    <!-- Content -->
                    <tr>
                        <td style="padding: 40px;">
                            <h2 style="margin: 0 0 20px; font-size: 20px; font-weight: 600; color: #1a1a1a;">Your Export is Ready</h2>

                            <p style="margin: 0 0 20px; font-size: 16px; line-height: 24px; color: #404040;">
                                Your data export has been prepared and is ready for download.
                            </p>

                            <!-- CTA Button -->
                            <table role="presentation" style="margin: 30px 0;">
                                <tr>
                                    <td style="border-radius: 6px; background-color: #007aff;">
                                        <a href="${exportUrl}"
                                           style="display: inline-block; padding: 14px 28px; font-size: 16px; font-weight: 600; color: #ffffff; text-decoration: none; border-radius: 6px;">
                                            Download Export
                                        </a>
                                    </td>
                                </tr>
                            </table>

                            <div style="margin: 30px 0 0; padding: 20px; background-color: #fff9e6; border-radius: 6px; border-left: 4px solid #ffcc00;">
                                <p style="margin: 0; font-size: 14px; line-height: 20px; color: #666666;">
                                    <strong style="color: #1a1a1a;">Important:</strong><br>
                                    This link will expire on ${formattedDate}. Please download your export before then.
                                </p>
                            </div>
                        </td>
                    </tr>

                    <!-- Footer -->
                    <tr>
                        <td style="padding: 20px 40px 40px; text-align: center; border-top: 1px solid #e5e5e5;">
                            <p style="margin: 0; font-size: 12px; line-height: 18px; color: #999999;">
                                This is an automated message from Trailglass. Please do not reply to this email.
                            </p>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</body>
</html>
        """.trimIndent()
    }

    /**
     * Generates a plain text version of the password reset email.
     * Used as fallback for email clients that don't support HTML.
     */
    fun passwordResetEmailPlainText(resetUrl: String): String {
        return """
Trailglass - Reset Your Password

We received a request to reset your password for your Trailglass account.

To reset your password, visit this link:
${resetUrl}

If you didn't request a password reset, you can safely ignore this email.
Your password will remain unchanged.

---
This is an automated message from Trailglass. Please do not reply to this email.
        """.trimIndent()
    }

    /**
     * Generates a plain text version of the export ready email.
     */
    fun exportReadyEmailPlainText(exportUrl: String, expiresAt: Instant): String {
        val formattedDate = DateTimeFormatter.ISO_INSTANT.format(expiresAt)

        return """
Trailglass - Your Export is Ready

Your data export has been prepared and is ready for download.

Download your export here:
${exportUrl}

IMPORTANT: This link will expire on ${formattedDate}.
Please download your export before then.

---
This is an automated message from Trailglass. Please do not reply to this email.
        """.trimIndent()
    }
}
