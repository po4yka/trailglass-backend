package com.trailglass.backend.trip

import com.trailglass.backend.DatabaseTestFactory
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class TripServiceTest {
    private val database = DatabaseTestFactory.inMemory()
    private val service = ExposedTripService(database)

    @Test
    fun `create update and delete trip`() = runBlocking {
        val userId = UUID.randomUUID()
        val tripId = UUID.randomUUID()
        val initial = TripRecord(
            id = tripId,
            userId = userId,
            deviceId = UUID.randomUUID(),
            name = "Test Trip",
            startDate = Instant.now(),
            endDate = null,
            updatedAt = Instant.now(),
            deletedAt = null,
            serverVersion = 0,
        )

        val created = service.upsertTrip(TripUpsertRequest(initial))
        assertTrue(created.serverVersion > 0)

        val updated = service.upsertTrip(
            TripUpsertRequest(initial.copy(name = "Updated", updatedAt = Instant.now().plusSeconds(10)))
        )
        assertEquals("Updated", updated.name)

        val deleted = service.deleteTrip(userId, tripId)
        assertTrue(deleted.deletedAt != null)
    }

    @Test
    fun `get trip by id returns correct trip`() = runBlocking {
        val userId = UUID.randomUUID()
        val tripId = UUID.randomUUID()
        val trip = TripRecord(
            id = tripId,
            userId = userId,
            deviceId = UUID.randomUUID(),
            name = "Test Trip",
            startDate = Instant.now(),
            endDate = null,
            updatedAt = Instant.now(),
            deletedAt = null,
            serverVersion = 0,
        )

        service.upsertTrip(TripUpsertRequest(trip))
        val fetched = service.getTrip(userId, tripId)

        assertEquals(tripId, fetched.id)
        assertEquals(userId, fetched.userId)
        assertEquals("Test Trip", fetched.name)
    }

    @Test
    fun `get trip fails for wrong user`() = runBlocking {
        val userId1 = UUID.randomUUID()
        val userId2 = UUID.randomUUID()
        val tripId = UUID.randomUUID()
        val trip = TripRecord(
            id = tripId,
            userId = userId1,
            deviceId = UUID.randomUUID(),
            name = "Test Trip",
            startDate = Instant.now(),
            endDate = null,
            updatedAt = Instant.now(),
            deletedAt = null,
            serverVersion = 0,
        )

        service.upsertTrip(TripUpsertRequest(trip))

        val exception = org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            service.getTrip(userId2, tripId)
        }
        assertEquals("Trip not found for user", exception.message)
    }
}
