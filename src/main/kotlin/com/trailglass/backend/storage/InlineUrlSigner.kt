package com.trailglass.backend.storage

import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class InlineUrlSigner(private val secret: String) {
    @OptIn(ExperimentalEncodingApi::class)
    fun sign(key: String, operation: Operation, expiresAt: Instant): String {
        val payload = "${operation.name}:$key:${expiresAt.epochSecond}"
        val mac = Mac.getInstance(ALGORITHM).apply {
            init(SecretKeySpec(secret.toByteArray(), ALGORITHM))
        }
        val signature = mac.doFinal(payload.toByteArray())
        val signatureBase64 = Base64.UrlSafe.encode(signature)
        return "$payload:$signatureBase64"
    }

    fun verify(token: String, expectedKey: String, operation: Operation, now: Instant = Instant.now()): Boolean {
        val parts = token.split(":")
        if (parts.size < 4) return false
        val op = runCatching { Operation.valueOf(parts[0]) }.getOrNull() ?: return false
        val key = parts[1]
        val expires = parts[2].toLongOrNull() ?: return false
        val signature = parts.drop(3).joinToString(":")

        if (op != operation || key != expectedKey) return false
        if (expires < now.epochSecond) return false

        val expectedToken = sign(key, operation, Instant.ofEpochSecond(expires))
        return timingSafeEquals(expectedToken.substringAfterLast(":"), signature)
    }

    private fun timingSafeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }

    enum class Operation { UPLOAD, DOWNLOAD }

    companion object {
        private const val ALGORITHM = "HmacSHA256"
    }
}
