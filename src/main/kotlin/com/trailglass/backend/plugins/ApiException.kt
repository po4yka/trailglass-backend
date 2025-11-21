package com.trailglass.backend.plugins

import io.ktor.http.HttpStatusCode

class ApiException(
    val status: HttpStatusCode,
    val code: String,
    override val message: String,
    val details: Map<String, String?> = emptyMap(),
) : RuntimeException(message)
