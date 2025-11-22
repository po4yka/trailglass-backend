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

    @Test
    fun `get location by id returns correct location`() = runBlocking {
        val userId = UUID.randomUUID()
        val deviceId = UUID.randomUUID()
        val locationId = UUID.randomUUID()
        val sample = LocationSample(
            id = locationId,
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

        service.upsertBatch(LocationBatchRequest(userId, deviceId, listOf(sample)))
        val fetched = service.getLocation(userId, locationId)

        assertEquals(locationId, fetched.id)
        assertEquals(userId, fetched.userId)
        assertEquals(10.0, fetched.latitude)
        assertEquals(20.0, fetched.longitude)
        assertEquals(5f, fetched.accuracy)
    }

    @Test
    fun `get location fails for wrong user`() = runBlocking {
        val userId1 = UUID.randomUUID()
        val userId2 = UUID.randomUUID()
        val deviceId = UUID.randomUUID()
        val locationId = UUID.randomUUID()
        val sample = LocationSample(
            id = locationId,
            userId = userId1,
            deviceId = deviceId,
            latitude = 10.0,
            longitude = 20.0,
            accuracy = 5f,
            recordedAt = Instant.now(),
            updatedAt = Instant.now(),
            deletedAt = null,
            serverVersion = 0,
        )

        service.upsertBatch(LocationBatchRequest(userId1, deviceId, listOf(sample)))

        val exception = org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            service.getLocation(userId2, locationId)
        }
        assertEquals("Location not found for user", exception.message)
    }
}
