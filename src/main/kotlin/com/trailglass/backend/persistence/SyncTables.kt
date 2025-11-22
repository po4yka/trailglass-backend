package com.trailglass.backend.persistence

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.UUID

object LocationsTable : UUIDTable("locations") {
    val userId: Column<UUID> = uuid("user_id")
    val deviceId: Column<UUID> = uuid("device_id")
    val latitude: Column<Double> = double("latitude")
    val longitude: Column<Double> = double("longitude")
    val accuracy: Column<Float?> = float("accuracy").nullable()
    val recordedAt = timestamp("recorded_at")
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
    val deletedAt = timestamp("deleted_at").nullable()
    val serverVersion = long("server_version")

    init {
        index(true, userId, id)
    }
}

object PlaceVisitsTable : UUIDTable("place_visits") {
    val userId: Column<UUID> = uuid("user_id")
    val deviceId: Column<UUID> = uuid("device_id")
    val latitude: Column<Double> = double("latitude")
    val longitude: Column<Double> = double("longitude")
    val arrivedAt = timestamp("arrived_at")
    val departedAt = timestamp("departed_at").nullable()
    val category = varchar("category", 50).nullable()
    val isFavorite = bool("is_favorite").default(false)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
    val deletedAt = timestamp("deleted_at").nullable()
    val serverVersion = long("server_version")
}

object TripsTable : UUIDTable("trips") {
    val userId: Column<UUID> = uuid("user_id")
    val deviceId: Column<UUID> = uuid("device_id")
    val name = varchar("name", 255)
    val startDate = timestamp("start_date").nullable()
    val endDate = timestamp("end_date").nullable()
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
    val deletedAt = timestamp("deleted_at").nullable()
    val serverVersion = long("server_version")
}

object UserSettingsTable : UUIDTable("user_settings") {
    val preferences = text("preferences")
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
    val deletedAt = timestamp("deleted_at").nullable()
    val serverVersion = long("server_version")
}

object UserProfilesTable : UUIDTable("user_profiles") {
    val email = varchar("email", 255)
    val displayName = varchar("display_name", 255)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
    val deletedAt = timestamp("deleted_at").nullable()
    val serverVersion = long("server_version")
}

object DevicesTable : UUIDTable("devices") {
    val userId: Column<UUID> = uuid("user_id")
    val deviceName = varchar("device_name", 255)
    val platform = varchar("platform", 50)
    val osVersion = varchar("os_version", 50)
    val appVersion = varchar("app_version", 50)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp())
    val deletedAt = timestamp("deleted_at").nullable()
    val serverVersion = long("server_version")

    init {
        index(true, userId, id)
    }
}
