package com.trailglass.backend.trip

import com.trailglass.backend.persistence.ExposedRepository
import com.trailglass.backend.persistence.TripsTable
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

class ExposedTripService(database: Database) : ExposedRepository(database), TripService {
    init {
        ensureTables(TripsTable)
    }

    override suspend fun upsertTrip(request: TripUpsertRequest): TripRecord = tx {
        validateTrip(request.trip)
        val version = nextVersion()
        val record = request.trip.copy(serverVersion = version)

        val existing = TripsTable.select { TripsTable.id eq record.id }.singleOrNull()
        if (existing == null) {
            TripsTable.insert { row ->
                row[id] = record.id
                row[userId] = record.userId
                row[deviceId] = record.deviceId
                row[name] = record.name
                row[startDate] = record.startDate
                row[endDate] = record.endDate
                row[updatedAt] = record.updatedAt
                row[deletedAt] = record.deletedAt
                row[serverVersion] = version
            }
        } else if (existing[TripsTable.updatedAt] < record.updatedAt) {
            TripsTable.update({ TripsTable.id eq record.id }) { row ->
                row[name] = record.name
                row[startDate] = record.startDate
                row[endDate] = record.endDate
                row[updatedAt] = record.updatedAt
                row[deletedAt] = record.deletedAt
                row[serverVersion] = version
            }
        }
        record
    }

    override suspend fun listTrips(userId: UUID, updatedAfter: Instant?, limit: Int): List<TripRecord> = tx {
        val query = TripsTable
            .select { TripsTable.userId eq userId }
            .let { base -> updatedAfter?.let { base.andWhere { TripsTable.updatedAt greaterEq it } } ?: base }
            .orderBy(TripsTable.updatedAt to false)
            .limit(limit)

        query.map { row ->
            TripRecord(
                id = row[TripsTable.id].value,
                userId = row[TripsTable.userId],
                deviceId = row[TripsTable.deviceId],
                name = row[TripsTable.name],
                startDate = row[TripsTable.startDate],
                endDate = row[TripsTable.endDate],
                updatedAt = row[TripsTable.updatedAt],
                deletedAt = row[TripsTable.deletedAt],
                serverVersion = row[TripsTable.serverVersion],
            )
        }
    }

    override suspend fun getTrip(userId: UUID, tripId: UUID): TripRecord = tx {
        val row = TripsTable
            .select { (TripsTable.id eq tripId) and (TripsTable.userId eq userId) }
            .singleOrNull()
            ?: throw IllegalArgumentException("Trip not found for user")

        TripRecord(
            id = row[TripsTable.id].value,
            userId = row[TripsTable.userId],
            deviceId = row[TripsTable.deviceId],
            name = row[TripsTable.name],
            startDate = row[TripsTable.startDate],
            endDate = row[TripsTable.endDate],
            updatedAt = row[TripsTable.updatedAt],
            deletedAt = row[TripsTable.deletedAt],
            serverVersion = row[TripsTable.serverVersion],
        )
    }

    override suspend fun deleteTrip(userId: UUID, tripId: UUID): TripRecord = tx {
        val version = nextVersion()
        val now = Instant.now()
        val existing = TripsTable.select { (TripsTable.id eq tripId) and (TripsTable.userId eq userId) }.single()
        TripsTable.update({ TripsTable.id eq tripId }) { row ->
            row[deletedAt] = now
            row[updatedAt] = now
            row[serverVersion] = version
        }
        TripRecord(
            id = existing[TripsTable.id].value,
            userId = existing[TripsTable.userId],
            deviceId = existing[TripsTable.deviceId],
            name = existing[TripsTable.name],
            startDate = existing[TripsTable.startDate],
            endDate = existing[TripsTable.endDate],
            updatedAt = now,
            deletedAt = now,
            serverVersion = version,
        )
    }

    private fun validateTrip(trip: TripRecord) {
        require(trip.name.isNotBlank()) { "Trip name is required" }
    }

    private fun nextVersion(): Long = Instant.now().toEpochMilli()
}
