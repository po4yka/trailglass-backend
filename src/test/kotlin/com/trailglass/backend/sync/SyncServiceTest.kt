package com.trailglass.backend.sync

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SyncServiceTest {
    private lateinit var database: Database
    private lateinit var service: SyncService

    private val userId = UUID.randomUUID()
    private val deviceA = UUID.randomUUID()
    private val deviceB = UUID.randomUUID()
    private val clock: Clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC)

    @BeforeTest
    fun setUp() {
        database = Database.connect(
            url = "jdbc:h2:mem:sync;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
            driver = "org.h2.Driver",
        )

        transaction(database) {
            SchemaUtils.create(
                UsersTable,
                DevicesTable,
                SyncVersionCountersTable,
                SyncDataTable,
                EncryptedSyncDataTable,
                SyncConflictsTable,
                DeviceSyncStateTable,
            )

            UsersTable.insert {
                it[id] = userId
                it[email] = "user@example.com"
                it[passwordHash] = "secret"
                it[createdAt] = clock.instant()
                it[updatedAt] = clock.instant()
            }

            DevicesTable.insert {
                it[id] = deviceA
                it[userId] = userId
                it[label] = "Device A"
                it[createdAt] = clock.instant()
                it[updatedAt] = clock.instant()
            }

            DevicesTable.insert {
                it[id] = deviceB
                it[userId] = userId
                it[label] = "Device B"
                it[createdAt] = clock.instant()
                it[updatedAt] = clock.instant()
            }
        }

        service = SyncServiceImpl(database, clock)
    }

    @Test
    fun `applyDelta stores data and returns remote outbound changes`() = runBlocking {
        val entityId = UUID.randomUUID()
        val firstResponse = service.applyDelta(
            SyncDeltaRequest(
                userId = userId,
                deviceId = deviceA,
                sinceVersion = 0,
                incoming = listOf(
                    SyncEnvelope(
                        id = entityId,
                        serverVersion = 0,
                        updatedAt = clock.instant(),
                        deletedAt = null,
                        payload = "payload-a",
                        isEncrypted = false,
                        deviceId = deviceA,
                    ),
                ),
            ),
        )

        assertEquals(1, firstResponse.applied.size)
        assertEquals(0, firstResponse.conflicts.size)

        val outboundResponse = service.applyDelta(
            SyncDeltaRequest(
                userId = userId,
                deviceId = deviceB,
                sinceVersion = 0,
                incoming = emptyList(),
            ),
        )

        assertEquals(1, outboundResponse.outbound.size)
        val outbound = outboundResponse.outbound.first()
        assertEquals(entityId, outbound.id)
        assertEquals(deviceA, outbound.deviceId)
        assertTrue(outbound.serverVersion > 0)
    }

    @Test
    fun `applyDelta records conflicts when incoming version is behind`() = runBlocking {
        val entityId = UUID.randomUUID()
        val serverVersion = 2L

        transaction(database) {
            SyncVersionCountersTable.insert {
                it[userId] = userId
                it[currentVersion] = serverVersion
            }

            SyncDataTable.insert {
                it[id] = entityId
                it[userId] = userId
                it[deviceId] = deviceB
                it[payload] = "server-payload"
                it[updatedAt] = clock.instant()
                it[deletedAt] = null
                it[serverVersion] = serverVersion
            }
        }

        val response = service.applyDelta(
            SyncDeltaRequest(
                userId = userId,
                deviceId = deviceA,
                sinceVersion = 0,
                incoming = listOf(
                    SyncEnvelope(
                        id = entityId,
                        serverVersion = 1,
                        updatedAt = clock.instant(),
                        deletedAt = null,
                        payload = "client-payload",
                        isEncrypted = false,
                        deviceId = deviceA,
                    ),
                ),
            ),
        )

        assertEquals(0, response.applied.size)
        assertEquals(1, response.conflicts.size)
        val conflict = response.conflicts.first()
        assertEquals(serverVersion, conflict.serverVersion)
        assertEquals(1, conflict.deviceVersion)
        assertEquals("server-payload", conflict.serverPayload)
        assertEquals("client-payload", conflict.devicePayload)
        assertNotNull(conflict.conflictId)
    }

    @Test
    fun `resolveConflict moves data to encrypted table when requested`() = runBlocking {
        val entityId = UUID.randomUUID()
        val conflictResponse = service.applyDelta(
            SyncDeltaRequest(
                userId = userId,
                deviceId = deviceA,
                sinceVersion = 0,
                incoming = listOf(
                    SyncEnvelope(
                        id = entityId,
                        serverVersion = 0,
                        updatedAt = clock.instant(),
                        deletedAt = null,
                        payload = "original",
                        isEncrypted = false,
                        deviceId = deviceA,
                    ),
                ),
            ),
        )

        val newerResponse = service.applyDelta(
            SyncDeltaRequest(
                userId = userId,
                deviceId = deviceB,
                sinceVersion = conflictResponse.serverVersion,
                incoming = listOf(
                    SyncEnvelope(
                        id = entityId,
                        serverVersion = conflictResponse.serverVersion,
                        updatedAt = clock.instant(),
                        deletedAt = null,
                        payload = "server-edited",
                        isEncrypted = false,
                        deviceId = deviceB,
                    ),
                ),
            ),
        )

        val conflict = newerResponse.conflicts.firstOrNull()
            ?: error("Expected a conflict for resolution test")

        val resolution = service.resolveConflict(
            ConflictResolutionRequest(
                conflictId = conflict.conflictId,
                entityId = entityId,
                chosenPayload = "encrypted-version",
                isEncrypted = true,
                userId = userId,
                deviceId = deviceA,
            ),
        )

        assertTrue(resolution.serverVersion > conflict.serverVersion)

        val outboundResponse = service.applyDelta(
            SyncDeltaRequest(
                userId = userId,
                deviceId = deviceB,
                sinceVersion = conflict.serverVersion,
                incoming = emptyList(),
            ),
        )

        assertEquals(1, outboundResponse.outbound.size)
        assertTrue(outboundResponse.outbound.first().isEncrypted)
    }
}
