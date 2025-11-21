package com.trailglass.backend.visit

import com.trailglass.backend.DatabaseTestFactory
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class PlaceVisitServiceTest {
    private val database = DatabaseTestFactory.inMemory()
    private val service = ExposedPlaceVisitService(database)

    @Test
    fun `upsert list and delete place visit`() = runBlocking {
        val userId = UUID.randomUUID()
        val visit = PlaceVisit(
            id = UUID.randomUUID(),
            userId = userId,
            deviceId = UUID.randomUUID(),
            latitude = 1.0,
            longitude = 2.0,
            arrivedAt = Instant.now(),
            departedAt = null,
            updatedAt = Instant.now(),
            deletedAt = null,
            serverVersion = 0,
        )

        val result = service.upsertVisits(listOf(visit))
        assertEquals(1, result.appliedCount)

        val listed = service.listVisits(userId, null, 10)
        assertEquals(1, listed.size)

        val deleted = service.deleteVisits(userId, listOf(visit.id))
        assertEquals(1, deleted.appliedCount)
        assertTrue(service.listVisits(userId, null, 10).first().deletedAt != null)
    }
}
