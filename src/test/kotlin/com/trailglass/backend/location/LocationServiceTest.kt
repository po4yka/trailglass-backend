package com.trailglass.backend.location

import com.trailglass.backend.DatabaseTestFactory
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class LocationServiceTest {
    private val database = DatabaseTestFactory.inMemory()
    private val service = ExposedLocationService(database)

    @Test
    fun `batch upsert and delete updates server version`() = runBlocking {
        val userId = UUID.randomUUID()
        val deviceId = UUID.randomUUID()
        val sample = LocationSample(
            id = UUID.randomUUID(),
            userId = userId,
            deviceId = deviceId,
            latitude = 10.0,
            longitude = 20.0,
            accuracy = 5f,
            recordedAt = Instant.now(),
            updatedAt = Instant.now(),
            deletedAt = null,
            serverVersion = 0,
        )

        val result = service.upsertBatch(LocationBatchRequest(userId, deviceId, listOf(sample)))
        assertEquals(1, result.appliedCount)

        val listed = service.getLocations(userId, null, 10)
        assertEquals(1, listed.size)
        assertTrue(listed.first().serverVersion >= result.serverVersion)

        val deleteResult = service.deleteLocations(userId, listOf(sample.id))
        assertEquals(1, deleteResult.appliedCount)
        val deleted = service.getLocations(userId, null, 10).first()
        assertTrue(deleted.deletedAt != null)
    }
}
