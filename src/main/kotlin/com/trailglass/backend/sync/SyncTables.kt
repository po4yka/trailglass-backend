package com.trailglass.backend.sync

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone
import java.time.Instant
import java.util.UUID

object UsersTable : Table("users") {
    val id = uuid("id")
    val email = text("email")
    val passwordHash = text("password_hash")
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestampWithTimeZone("updated_at").defaultExpression(CurrentTimestamp())

    override val primaryKey = PrimaryKey(id)
}

object DevicesTable : Table("devices") {
    val id = uuid("id")
    val userId = reference("user_id", UsersTable.id)
    val label = text("label").nullable()
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestampWithTimeZone("updated_at").defaultExpression(CurrentTimestamp())

    override val primaryKey = PrimaryKey(id)
}

object SyncVersionCountersTable : Table("sync_version_counters") {
    val userId = reference("user_id", UsersTable.id)
    val currentVersion = long("current_version").default(0)
    val updatedAt = timestampWithTimeZone("updated_at").defaultExpression(CurrentTimestamp())

    override val primaryKey = PrimaryKey(userId)
}

abstract class SyncPayloadTable(name: String) : Table(name) {
    abstract val id: Column<UUID>
    abstract val userId: Column<UUID>
    abstract val deviceId: Column<UUID>
    abstract val payload: Column<String>
    abstract val updatedAt: Column<Instant>
    abstract val deletedAt: Column<Instant?>
    abstract val serverVersion: Column<Long>
}

object SyncDataTable : SyncPayloadTable("sync_data") {
    override val id = uuid("id")
    override val userId = reference("user_id", UsersTable.id)
    override val deviceId = reference("device_id", DevicesTable.id)
    override val payload = text("payload")
    override val updatedAt = timestampWithTimeZone("updated_at")
    override val deletedAt = timestampWithTimeZone("deleted_at").nullable()
    override val serverVersion = long("server_version")
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestamp())

    override val primaryKey = PrimaryKey(id)
}

object EncryptedSyncDataTable : SyncPayloadTable("encrypted_sync_data") {
    override val id = uuid("id")
    override val userId = reference("user_id", UsersTable.id)
    override val deviceId = reference("device_id", DevicesTable.id)
    override val payload = text("payload")
    override val updatedAt = timestampWithTimeZone("updated_at")
    override val deletedAt = timestampWithTimeZone("deleted_at").nullable()
    override val serverVersion = long("server_version")
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestamp())

    override val primaryKey = PrimaryKey(id)
}

object SyncConflictsTable : Table("sync_conflicts") {
    val id = uuid("id")
    val entityId = uuid("entity_id")
    val userId = reference("user_id", UsersTable.id)
    val deviceId = reference("device_id", DevicesTable.id)
    val serverVersion = long("server_version")
    val deviceVersion = long("device_version")
    val serverPayload = text("server_payload")
    val devicePayload = text("device_payload")
    val isEncrypted = bool("is_encrypted").default(false)
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestamp())
    val resolvedAt = timestampWithTimeZone("resolved_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

object DeviceSyncStateTable : Table("device_sync_state") {
    val deviceId = reference("device_id", DevicesTable.id)
    val userId = reference("user_id", UsersTable.id)
    val lastSyncAt = timestampWithTimeZone("last_sync_at").nullable()
    val updatedAt = timestampWithTimeZone("updated_at").defaultExpression(CurrentTimestamp())

    override val primaryKey = PrimaryKey(deviceId)
}
