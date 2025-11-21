package com.trailglass.backend.location

import com.trailglass.backend.persistence.LocationsTable
import com.trailglass.backend.persistence.ExposedRepository
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.greaterEq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class ExposedLocationService(database: Database) : ExposedRepository(database), LocationService {
    init {
        ensureTables(LocationsTable)
    }

    override suspend fun upsertBatch(request: LocationBatchRequest): LocationBatchResult = tx {
        validateBatch(request)
        var applied = 0
        val serverVersion = nextVersion()

        request.samples.forEach { sample ->
            val existing = LocationsTable.select { LocationsTable.id eq sample.id }.singleOrNull()
            if (existing == null || existing[LocationsTable.updatedAt] < sample.updatedAt) {
                if (existing == null) {
                    LocationsTable.insert { row ->
                        row[id] = sample.id
                        row[LocationsTable.userId] = sample.userId
                        row[deviceId] = sample.deviceId
                        row[latitude] = sample.latitude
                        row[longitude] = sample.longitude
                        row[accuracy] = sample.accuracy
                        row[recordedAt] = sample.recordedAt
                        row[updatedAt] = sample.updatedAt
                        row[deletedAt] = sample.deletedAt
                        row[serverVersion] = serverVersion
                    }
                } else {
                    LocationsTable.update({ LocationsTable.id eq sample.id }) { row ->
                        row[latitude] = sample.latitude
                        row[longitude] = sample.longitude
                        row[accuracy] = sample.accuracy
                        row[recordedAt] = sample.recordedAt
                        row[updatedAt] = sample.updatedAt
                        row[deletedAt] = sample.deletedAt
                        row[serverVersion] = serverVersion
                    }
                }
                applied++
            }
        }

        LocationBatchResult(appliedCount = applied, serverVersion = serverVersion)
    }

    override suspend fun getLocations(userId: UUID, since: Instant?, limit: Int): List<LocationSample> = tx {
        val query = LocationsTable
            .select { LocationsTable.userId eq userId }
            .let { base -> since?.let { base.andWhere { LocationsTable.updatedAt greaterEq it } } ?: base }
            .orderBy(LocationsTable.updatedAt to false)
            .limit(limit)

        query.map { row ->
            LocationSample(
                id = row[LocationsTable.id].value,
                userId = row[LocationsTable.userId],
                deviceId = row[LocationsTable.deviceId],
                latitude = row[LocationsTable.latitude],
                longitude = row[LocationsTable.longitude],
                accuracy = row[LocationsTable.accuracy],
                recordedAt = row[LocationsTable.recordedAt],
                updatedAt = row[LocationsTable.updatedAt],
                deletedAt = row[LocationsTable.deletedAt],
                serverVersion = row[LocationsTable.serverVersion],
            )
        }
    }

    override suspend fun deleteLocations(userId: UUID, ids: List<UUID>): LocationBatchResult = tx {
        val serverVersion = nextVersion()
        var applied = 0
        ids.forEach { id ->
            val updated = LocationsTable.update({ (LocationsTable.id eq id) and (LocationsTable.userId eq userId) }) { row ->
                row[deletedAt] = Instant.now()
                row[updatedAt] = Instant.now()
                row[serverVersion] = serverVersion
            }
            applied += updated
        }
        LocationBatchResult(appliedCount = applied, serverVersion = serverVersion)
    }

    private fun validateBatch(request: LocationBatchRequest) {
        require(request.samples.all { it.userId == request.userId }) { "All samples must match request userId" }
        require(request.samples.all { it.deviceId == request.deviceId }) { "All samples must match request deviceId" }
        request.samples.forEach { sample ->
            require(sample.latitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
            require(sample.longitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
        }
    }

    private fun nextVersion(): Long = Instant.now().toEpochMilli()
}
