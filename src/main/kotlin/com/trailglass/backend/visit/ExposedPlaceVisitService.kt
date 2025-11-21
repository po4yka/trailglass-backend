package com.trailglass.backend.visit

import com.trailglass.backend.persistence.ExposedRepository
import com.trailglass.backend.persistence.PlaceVisitsTable
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

class ExposedPlaceVisitService(database: Database) : ExposedRepository(database), PlaceVisitService {
    init {
        ensureTables(PlaceVisitsTable)
    }

    override suspend fun upsertVisits(visits: List<PlaceVisit>): VisitBatchResult = tx {
        var applied = 0
        val version = nextVersion()
        visits.forEach { visit ->
            require(visit.latitude in -90.0..90.0 && visit.longitude in -180.0..180.0) { "Invalid coordinates" }
            val existing = PlaceVisitsTable.select { PlaceVisitsTable.id eq visit.id }.singleOrNull()
            if (existing == null || existing[PlaceVisitsTable.updatedAt] < visit.updatedAt) {
                if (existing == null) {
                    PlaceVisitsTable.insert { row ->
                        row[id] = visit.id
                        row[userId] = visit.userId
                        row[deviceId] = visit.deviceId
                        row[latitude] = visit.latitude
                        row[longitude] = visit.longitude
                        row[arrivedAt] = visit.arrivedAt
                        row[departedAt] = visit.departedAt
                        row[updatedAt] = visit.updatedAt
                        row[deletedAt] = visit.deletedAt
                        row[serverVersion] = version
                    }
                } else {
                    PlaceVisitsTable.update({ PlaceVisitsTable.id eq visit.id }) { row ->
                        row[latitude] = visit.latitude
                        row[longitude] = visit.longitude
                        row[arrivedAt] = visit.arrivedAt
                        row[departedAt] = visit.departedAt
                        row[updatedAt] = visit.updatedAt
                        row[deletedAt] = visit.deletedAt
                        row[serverVersion] = version
                    }
                }
                applied++
            }
        }

        VisitBatchResult(appliedCount = applied, serverVersion = version)
    }

    override suspend fun listVisits(userId: UUID, updatedAfter: Instant?, limit: Int): List<PlaceVisit> = tx {
        val query = PlaceVisitsTable
            .select { PlaceVisitsTable.userId eq userId }
            .let { base -> updatedAfter?.let { base.andWhere { PlaceVisitsTable.updatedAt greaterEq it } } ?: base }
            .orderBy(PlaceVisitsTable.updatedAt to false)
            .limit(limit)

        query.map { row ->
            PlaceVisit(
                id = row[PlaceVisitsTable.id].value,
                userId = row[PlaceVisitsTable.userId],
                deviceId = row[PlaceVisitsTable.deviceId],
                latitude = row[PlaceVisitsTable.latitude],
                longitude = row[PlaceVisitsTable.longitude],
                arrivedAt = row[PlaceVisitsTable.arrivedAt],
                departedAt = row[PlaceVisitsTable.departedAt],
                updatedAt = row[PlaceVisitsTable.updatedAt],
                deletedAt = row[PlaceVisitsTable.deletedAt],
                serverVersion = row[PlaceVisitsTable.serverVersion],
            )
        }
    }

    override suspend fun deleteVisits(userId: UUID, ids: List<UUID>): VisitBatchResult = tx {
        val now = Instant.now()
        val version = now.toEpochMilli()
        var applied = 0
        ids.forEach { id ->
            val updated = PlaceVisitsTable.update({ (PlaceVisitsTable.id eq id) and (PlaceVisitsTable.userId eq userId) }) { row ->
                row[deletedAt] = now
                row[updatedAt] = now
                row[serverVersion] = version
            }
            applied += updated
        }
        VisitBatchResult(appliedCount = applied, serverVersion = version)
    }

    private fun nextVersion(): Long = Instant.now().toEpochMilli()
}
