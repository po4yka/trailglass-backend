package com.trailglass.backend.user

import com.trailglass.backend.persistence.DevicesTable
import com.trailglass.backend.persistence.ExposedRepository
import com.trailglass.backend.persistence.UserProfilesTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.greaterEq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class ExposedUserProfileService(database: Database) : ExposedRepository(database), UserProfileService {
    init {
        ensureTables(UserProfilesTable, DevicesTable)
    }

    override suspend fun upsertProfile(profile: UserProfile): UserProfile = tx {
        val version = Instant.now().toEpochMilli()
        val existing = UserProfilesTable.select { UserProfilesTable.id eq profile.id }.singleOrNull()
        if (existing == null) {
            UserProfilesTable.insert { row ->
                row[id] = profile.id
                row[email] = profile.email
                row[displayName] = profile.displayName
                row[updatedAt] = profile.updatedAt
                row[deletedAt] = profile.deletedAt
                row[serverVersion] = version
            }
        } else if (existing[UserProfilesTable.updatedAt] < profile.updatedAt) {
            UserProfilesTable.update({ UserProfilesTable.id eq profile.id }) { row ->
                row[email] = profile.email
                row[displayName] = profile.displayName
                row[updatedAt] = profile.updatedAt
                row[deletedAt] = profile.deletedAt
                row[serverVersion] = version
            }
        }
        profile.copy(serverVersion = version)
    }

    override suspend fun listDevices(userId: UUID, updatedAfter: Instant?, limit: Int): List<DeviceProfile> = tx {
        val query = DevicesTable
            .select { DevicesTable.userId eq userId }
            .let { base -> updatedAfter?.let { base.andWhere { DevicesTable.updatedAt greaterEq it } } ?: base }
            .orderBy(DevicesTable.updatedAt to false)
            .limit(limit)

        query.map { row ->
            DeviceProfile(
                id = row[DevicesTable.id].value,
                userId = row[DevicesTable.userId],
                deviceName = row[DevicesTable.deviceName],
                platform = row[DevicesTable.platform],
                osVersion = row[DevicesTable.osVersion],
                appVersion = row[DevicesTable.appVersion],
                updatedAt = row[DevicesTable.updatedAt],
                deletedAt = row[DevicesTable.deletedAt],
                serverVersion = row[DevicesTable.serverVersion],
            )
        }
    }

    override suspend fun registerDevice(device: DeviceProfile): DeviceProfile = tx {
        val version = Instant.now().toEpochMilli()
        val existing = DevicesTable.select { DevicesTable.id eq device.id }.singleOrNull()
        if (existing == null) {
            DevicesTable.insert { row ->
                row[id] = device.id
                row[userId] = device.userId
                row[deviceName] = device.deviceName
                row[platform] = device.platform
                row[osVersion] = device.osVersion
                row[appVersion] = device.appVersion
                row[updatedAt] = device.updatedAt
                row[deletedAt] = device.deletedAt
                row[serverVersion] = version
            }
        } else if (existing[DevicesTable.updatedAt] < device.updatedAt) {
            DevicesTable.update({ DevicesTable.id eq device.id }) { row ->
                row[deviceName] = device.deviceName
                row[platform] = device.platform
                row[osVersion] = device.osVersion
                row[appVersion] = device.appVersion
                row[updatedAt] = device.updatedAt
                row[deletedAt] = device.deletedAt
                row[serverVersion] = version
            }
        }
        device.copy(serverVersion = version)
    }

    override suspend fun deleteDevice(userId: UUID, deviceId: UUID): DeviceProfile? = tx {
        val existing = DevicesTable.select { (DevicesTable.id eq deviceId) and (DevicesTable.userId eq userId) }.singleOrNull()
            ?: return@tx null
        val now = Instant.now()
        val version = now.toEpochMilli()
        DevicesTable.update({ DevicesTable.id eq deviceId }) { row ->
            row[deletedAt] = now
            row[updatedAt] = now
            row[serverVersion] = version
        }
        DeviceProfile(
            id = existing[DevicesTable.id].value,
            userId = existing[DevicesTable.userId],
            deviceName = existing[DevicesTable.deviceName],
            platform = existing[DevicesTable.platform],
            osVersion = existing[DevicesTable.osVersion],
            appVersion = existing[DevicesTable.appVersion],
            updatedAt = now,
            deletedAt = now,
            serverVersion = version,
        )
    }
}
