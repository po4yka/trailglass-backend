package com.trailglass.backend.persistence.schema

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object Users : UUIDTable("users") {
    val email = text("email").uniqueIndex()
    val fullName = text("full_name").nullable()
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestampWithTimeZone("updated_at").defaultExpression(CurrentTimestamp())
    val deletedAt = timestampWithTimeZone("deleted_at").nullable()
}

object UserCredentials : UUIDTable("user_credentials") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val passwordHash = text("password_hash")
    val passwordUpdatedAt = timestampWithTimeZone("password_updated_at").defaultExpression(CurrentTimestamp())

    init {
        uniqueIndex("ux_user_credentials_user_id", userId)
    }
}

object Devices : UUIDTable("devices") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val label = text("label").default("Device")
    val appVersion = text("app_version").nullable()
    val platform = text("platform").nullable()
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestampWithTimeZone("updated_at").defaultExpression(CurrentTimestamp())
    val lastSeenAt = timestampWithTimeZone("last_seen_at").nullable()
    val deletedAt = timestampWithTimeZone("deleted_at").nullable()

    init {
        index("idx_devices_user_id", false, userId)
    }
}

object RefreshTokens : UUIDTable("refresh_tokens") {
    val token = text("token").uniqueIndex()
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val deviceId = reference("device_id", Devices, onDelete = ReferenceOption.CASCADE)
    val expiresAt = timestampWithTimeZone("expires_at")
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestamp())
    val lastUsedAt = timestampWithTimeZone("last_used_at").nullable()
    val revokedAt = timestampWithTimeZone("revoked_at").nullable()

    init {
        uniqueIndex("ux_refresh_tokens_user_device_token", userId, deviceId, token)
        index("idx_refresh_tokens_user_id", false, userId)
        index("idx_refresh_tokens_device_id", false, deviceId)
    }
}

object PasswordResetTokens : UUIDTable("password_reset_tokens") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val token = text("token").uniqueIndex()
    val expiresAt = timestampWithTimeZone("expires_at")
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestamp())
    val consumedAt = timestampWithTimeZone("consumed_at").nullable()

    init {
        index("idx_password_reset_tokens_user_id", false, userId)
    }
}
