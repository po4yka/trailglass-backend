package com.trailglass.backend.settings

import com.trailglass.backend.persistence.ExposedRepository
import com.trailglass.backend.persistence.UserSettingsTable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class ExposedSettingsService(database: Database) : ExposedRepository(database), SettingsService {
    private val json = Json

    init {
        ensureTables(UserSettingsTable)
    }

    override suspend fun getSettings(userId: UUID): UserSettings = tx {
        val existing = UserSettingsTable.select { UserSettingsTable.id eq userId }.singleOrNull()
        if (existing == null) {
            val now = Instant.now()
            val defaults = UserSettings(
                userId = userId,
                preferences = emptyMap(),
                updatedAt = now,
                serverVersion = now.toEpochMilli(),
            )
            UserSettingsTable.insert { row ->
                row[id] = userId
                row[preferences] = json.encodeToString(MapSerializer, defaults.preferences)
                row[updatedAt] = defaults.updatedAt
                row[serverVersion] = defaults.serverVersion
                row[deletedAt] = null
            }
            defaults
        } else {
            UserSettings(
                userId = existing[UserSettingsTable.id].value,
                preferences = json.decodeFromString(MapSerializer, existing[UserSettingsTable.preferences]),
                updatedAt = existing[UserSettingsTable.updatedAt].toInstant(),
                serverVersion = existing[UserSettingsTable.serverVersion],
            )
        }
    }

    override suspend fun updateSettings(request: SettingsUpdateRequest): UserSettings = tx {
        val now = Instant.now()
        val version = now.toEpochMilli()
        UserSettingsTable.select { UserSettingsTable.id eq request.userId }.singleOrNull()
            ?: UserSettingsTable.insert { row ->
                row[id] = request.userId
                row[preferences] = json.encodeToString(MapSerializer, request.preferences)
                row[updatedAt] = now
                row[serverVersion] = version
                row[deletedAt] = null
            }

        UserSettingsTable.update({ UserSettingsTable.id eq request.userId }) { row ->
            row[preferences] = json.encodeToString(MapSerializer, request.preferences)
            row[updatedAt] = now
            row[serverVersion] = version
            row[deletedAt] = null
        }

        UserSettings(
            userId = request.userId,
            preferences = request.preferences,
            updatedAt = now,
            serverVersion = version,
        )
    }

    private object MapSerializer : kotlinx.serialization.KSerializer<Map<String, String>> by
        kotlinx.serialization.builtins.MapSerializer(String.serializer(), String.serializer())
}
