package com.trailglass.backend.plugins

import io.ktor.server.plugins.ratelimit.RateLimitName

val GlobalRateLimit = RateLimitName("global")
val AuthRateLimit = RateLimitName("auth")
val SyncRateLimit = RateLimitName("sync")
val LocationBatchRateLimit = RateLimitName("locations")
val PhotoUploadRateLimit = RateLimitName("photos")
val DefaultFeatureRateLimit = RateLimitName("default")
