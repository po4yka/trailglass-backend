package com.trailglass.backend.persistence.schema

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object SyncVersions : UUIDTable("sync_versions") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val deviceId = reference("device_id", Devices, onDelete = ReferenceOption.CASCADE)
    val entityType = text("entity_type")
    val serverVersion = long("server_version").default(0)
    val updatedAt = timestampWithTimeZone("updated_at").defaultExpression(CurrentTimestamp())

    init {
        uniqueIndex("ux_sync_versions_user_device_entity", userId, deviceId, entityType)
        index("idx_sync_versions_user_entity", false, userId, entityType)
    }
}
