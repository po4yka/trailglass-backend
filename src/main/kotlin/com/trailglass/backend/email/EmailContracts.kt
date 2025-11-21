package com.trailglass.backend.email

interface EmailService {
    suspend fun sendPasswordReset(to: String, resetLink: String)
    suspend fun sendExportReady(to: String, downloadLink: String)
}
