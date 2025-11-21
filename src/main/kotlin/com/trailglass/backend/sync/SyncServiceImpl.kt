package com.trailglass.backend.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.forUpdate
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Clock
import java.util.UUID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq

class SyncServiceImpl(
    private val database: org.jetbrains.exposed.sql.Database,
    private val clock: Clock = Clock.systemUTC(),
) : SyncService {

    override suspend fun getStatus(deviceId: UUID, userId: UUID): SyncStatus = dbQuery {
        val latestVersion = currentVersion(userId)
        val lastSync = DeviceSyncStateTable
            .select { (DeviceSyncStateTable.deviceId eq deviceId) and (DeviceSyncStateTable.userId eq userId) }
            .singleOrNull()?.get(DeviceSyncStateTable.lastSyncAt)

        SyncStatus(
            deviceId = deviceId,
            userId = userId,
            latestServerVersion = latestVersion,
            lastSyncAt = lastSync,
        )
    }

    override suspend fun applyDelta(request: SyncDeltaRequest): SyncDeltaResponse = dbQuery {
        val applied = mutableListOf<SyncEnvelope>()
        val conflicts = mutableListOf<SyncConflict>()

        request.incoming.forEach { envelope ->
            val targetTable = tableForEnvelope(envelope)
            val existing = targetTable
                .select { (targetTable.id eq envelope.id) and (targetTable.userId eq request.userId) }
                .singleOrNull()

            if (existing != null && envelope.serverVersion < existing[targetTable.serverVersion]) {
                val conflictId = UUID.randomUUID()
                val serverPayload = existing[targetTable.payload]
                val serverVersion = existing[targetTable.serverVersion]

                SyncConflictsTable.insert {
                    it[id] = conflictId
                    it[entityId] = envelope.id
                    it[userId] = request.userId
                    it[deviceId] = request.deviceId
                    it[deviceVersion] = envelope.serverVersion
                    it[serverVersion] = serverVersion
                    it[serverPayload] = serverPayload
                    it[devicePayload] = envelope.payload
                    it[isEncrypted] = envelope.isEncrypted
                }

                conflicts += SyncConflict(
                    conflictId = conflictId,
                    entityId = envelope.id,
                    serverVersion = serverVersion,
                    deviceVersion = envelope.serverVersion,
                    serverPayload = serverPayload,
                    devicePayload = envelope.payload,
                    isEncrypted = envelope.isEncrypted,
                )
            } else {
                val newVersion = nextVersion(request.userId)
                upsertEnvelope(targetTable, envelope, request, newVersion)
                applied += envelope.copy(serverVersion = newVersion)
            }
        }

        val outbound = collectOutbound(request)
        val latestVersion = currentVersion(request.userId)

        upsertDeviceSyncState(request.deviceId, request.userId)

        SyncDeltaResponse(
            serverVersion = latestVersion,
            applied = applied,
            conflicts = conflicts,
            outbound = outbound,
        )
    }

    override suspend fun resolveConflict(request: ConflictResolutionRequest): ConflictResolutionResult = dbQuery {
        val conflict = SyncConflictsTable
            .select { (SyncConflictsTable.id eq request.conflictId) and (SyncConflictsTable.userId eq request.userId) }
            .singleOrNull()
            ?: throw IllegalArgumentException("Conflict not found")

        val targetTable = if (request.isEncrypted) EncryptedSyncDataTable else SyncDataTable
        val resolvedVersion = nextVersion(request.userId)
        val now = clock.instant()

        upsertEnvelope(
            table = targetTable,
            envelope = SyncEnvelope(
                id = request.entityId,
                serverVersion = resolvedVersion,
                updatedAt = now,
                deletedAt = null,
                payload = request.chosenPayload,
                isEncrypted = request.isEncrypted,
                deviceId = request.deviceId,
            ),
            request = SyncDeltaRequest(
                userId = request.userId,
                deviceId = request.deviceId,
                sinceVersion = resolvedVersion,
                incoming = emptyList(),
            ),
            serverVersion = resolvedVersion,
        )

        SyncConflictsTable.update({ SyncConflictsTable.id eq request.conflictId }) {
            it[resolvedAt] = now
        }

        upsertDeviceSyncState(request.deviceId, request.userId)

        ConflictResolutionResult(
            entityId = request.entityId,
            serverVersion = resolvedVersion,
            resolvedAt = now,
        )
    }

    private fun tableForEnvelope(envelope: SyncEnvelope): SyncPayloadTable =
        if (envelope.isEncrypted) EncryptedSyncDataTable else SyncDataTable

    private fun upsertEnvelope(
        table: SyncPayloadTable,
        envelope: SyncEnvelope,
        request: SyncDeltaRequest,
        serverVersion: Long,
    ) {
        val existing = table
            .select { (table.id eq envelope.id) and (table.userId eq request.userId) }
            .singleOrNull()

        val updatedAtValue = envelope.updatedAt

        if (existing == null) {
            table.insert {
                it[id] = envelope.id
                it[userId] = request.userId
                it[deviceId] = request.deviceId
                it[payload] = envelope.payload
                it[deletedAt] = envelope.deletedAt
                it[updatedAt] = updatedAtValue
                it[serverVersion] = serverVersion
            }
        } else {
            table.update({ (table.id eq envelope.id) and (table.userId eq request.userId) }) {
                it[payload] = envelope.payload
                it[deletedAt] = envelope.deletedAt
                it[updatedAt] = updatedAtValue
                it[serverVersion] = serverVersion
                it[deviceId] = request.deviceId
            }
        }

        removeFromOtherTable(table, envelope.id, request.userId)
    }

    private fun removeFromOtherTable(currentTable: SyncPayloadTable, entityId: UUID, userId: UUID) {
        val otherTable = if (currentTable == SyncDataTable) EncryptedSyncDataTable else SyncDataTable
        otherTable.deleteWhere { (otherTable.id eq entityId) and (otherTable.userId eq userId) }
    }

    private fun collectOutbound(request: SyncDeltaRequest): List<SyncEnvelope> {
        val plain = SyncDataTable
            .select {
                (SyncDataTable.userId eq request.userId) and
                    (SyncDataTable.serverVersion greater request.sinceVersion) and
                    (SyncDataTable.deviceId neq request.deviceId)
            }
            .map { it.toEnvelope(SyncDataTable, false) }

        val encrypted = EncryptedSyncDataTable
            .select {
                (EncryptedSyncDataTable.userId eq request.userId) and
                    (EncryptedSyncDataTable.serverVersion greater request.sinceVersion) and
                    (EncryptedSyncDataTable.deviceId neq request.deviceId)
            }
            .map { it.toEnvelope(EncryptedSyncDataTable, true) }

        return (plain + encrypted).sortedBy { it.serverVersion }
    }

    private fun ResultRow.toEnvelope(table: SyncPayloadTable, isEncrypted: Boolean): SyncEnvelope = SyncEnvelope(
        id = this[table.id],
        serverVersion = this[table.serverVersion],
        updatedAt = this[table.updatedAt],
        deletedAt = this[table.deletedAt],
        payload = this[table.payload],
        isEncrypted = isEncrypted,
        deviceId = this[table.deviceId],
    )

    private fun currentVersion(userId: UUID): Long = SyncVersionCountersTable
        .select { SyncVersionCountersTable.userId eq userId }
        .singleOrNull()?.get(SyncVersionCountersTable.currentVersion) ?: 0L

    private fun nextVersion(userId: UUID): Long {
        val existing = SyncVersionCountersTable
            .select { SyncVersionCountersTable.userId eq userId }
            .forUpdate()
            .singleOrNull()

        val next = (existing?.get(SyncVersionCountersTable.currentVersion) ?: 0L) + 1

        if (existing == null) {
            SyncVersionCountersTable.insert {
                it[this.userId] = userId
                it[currentVersion] = next
            }
        } else {
            SyncVersionCountersTable.update({ SyncVersionCountersTable.userId eq userId }) {
                it[currentVersion] = next
                it[updatedAt] = clock.instant()
            }
        }

        return next
    }

    private fun upsertDeviceSyncState(deviceId: UUID, userId: UUID) {
        val existing = DeviceSyncStateTable
            .select { (DeviceSyncStateTable.deviceId eq deviceId) and (DeviceSyncStateTable.userId eq userId) }
            .singleOrNull()

        val now = clock.instant()
        if (existing == null) {
            DeviceSyncStateTable.insert {
                it[this.deviceId] = deviceId
                it[this.userId] = userId
                it[lastSyncAt] = now
            }
        } else {
            DeviceSyncStateTable.update({ (DeviceSyncStateTable.deviceId eq deviceId) and (DeviceSyncStateTable.userId eq userId) }) {
                it[lastSyncAt] = now
                it[updatedAt] = now
            }
        }
    }

    private suspend fun <T> dbQuery(block: () -> T): T = withContext(Dispatchers.IO) {
        transaction(database) { block() }
    }
}
