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
            category = "OUTDOOR",
            isFavorite = false,
            updatedAt = Instant.now(),
            deletedAt = null,
            serverVersion = 0,
        )

        val result = service.upsertVisits(listOf(visit))
        assertEquals(1, result.appliedCount)

        val listed = service.listVisits(userId, null, null, null, 10)
        assertEquals(1, listed.size)

        val deleted = service.deleteVisits(userId, listOf(visit.id))
        assertEquals(1, deleted.appliedCount)
        assertTrue(service.listVisits(userId, null, null, null, 10).first().deletedAt != null)
    }

    @Test
    fun `filter visits by category`() = runBlocking {
        val userId = UUID.randomUUID()
        val deviceId = UUID.randomUUID()
        val now = Instant.now()

        val outdoorVisit = PlaceVisit(
            id = UUID.randomUUID(),
            userId = userId,
            deviceId = deviceId,
            latitude = 1.0,
            longitude = 2.0,
            arrivedAt = now,
            departedAt = null,
            category = "OUTDOOR",
            isFavorite = false,
            updatedAt = now,
            deletedAt = null,
            serverVersion = 0,
        )

        val restaurantVisit = PlaceVisit(
            id = UUID.randomUUID(),
            userId = userId,
            deviceId = deviceId,
            latitude = 1.0,
            longitude = 2.0,
            arrivedAt = now,
            departedAt = null,
            category = "RESTAURANT",
            isFavorite = false,
            updatedAt = now,
            deletedAt = null,
            serverVersion = 0,
        )

        service.upsertVisits(listOf(outdoorVisit, restaurantVisit))

        val outdoorOnly = service.listVisits(userId, null, "OUTDOOR", null, 10)
        assertEquals(1, outdoorOnly.size)
        assertEquals("OUTDOOR", outdoorOnly.first().category)

        val restaurantOnly = service.listVisits(userId, null, "RESTAURANT", null, 10)
        assertEquals(1, restaurantOnly.size)
        assertEquals("RESTAURANT", restaurantOnly.first().category)
    }

    @Test
    fun `filter visits by favorite status`() = runBlocking {
        val userId = UUID.randomUUID()
        val deviceId = UUID.randomUUID()
        val now = Instant.now()

        val favoriteVisit = PlaceVisit(
            id = UUID.randomUUID(),
            userId = userId,
            deviceId = deviceId,
            latitude = 1.0,
            longitude = 2.0,
            arrivedAt = now,
            departedAt = null,
            category = "OUTDOOR",
            isFavorite = true,
            updatedAt = now,
            deletedAt = null,
            serverVersion = 0,
        )

        val normalVisit = PlaceVisit(
            id = UUID.randomUUID(),
            userId = userId,
            deviceId = deviceId,
            latitude = 1.0,
            longitude = 2.0,
            arrivedAt = now,
            departedAt = null,
            category = "RESTAURANT",
            isFavorite = false,
            updatedAt = now,
            deletedAt = null,
            serverVersion = 0,
        )

        service.upsertVisits(listOf(favoriteVisit, normalVisit))

        val favoritesOnly = service.listVisits(userId, null, null, true, 10)
        assertEquals(1, favoritesOnly.size)
        assertTrue(favoritesOnly.first().isFavorite)
    }

    @Test
    fun `get place visit by id returns correct visit`() = runBlocking {
        val userId = UUID.randomUUID()
        val visitId = UUID.randomUUID()
        val visit = PlaceVisit(
            id = visitId,
            userId = userId,
            deviceId = UUID.randomUUID(),
            latitude = 1.0,
            longitude = 2.0,
            arrivedAt = Instant.now(),
            departedAt = null,
            category = "OUTDOOR",
            isFavorite = false,
            updatedAt = Instant.now(),
            deletedAt = null,
            serverVersion = 0,
        )

        service.upsertVisits(listOf(visit))
        val fetched = service.getVisit(userId, visitId)

        assertEquals(visitId, fetched.id)
        assertEquals(userId, fetched.userId)
        assertEquals(1.0, fetched.latitude)
        assertEquals(2.0, fetched.longitude)
        assertEquals("OUTDOOR", fetched.category)
    }

    @Test
    fun `get place visit fails for wrong user`() = runBlocking {
        val userId1 = UUID.randomUUID()
        val userId2 = UUID.randomUUID()
        val visitId = UUID.randomUUID()
        val visit = PlaceVisit(
            id = visitId,
            userId = userId1,
            deviceId = UUID.randomUUID(),
            latitude = 1.0,
            longitude = 2.0,
            arrivedAt = Instant.now(),
            departedAt = null,
            category = "OUTDOOR",
            isFavorite = false,
            updatedAt = Instant.now(),
            deletedAt = null,
            serverVersion = 0,
        )

        service.upsertVisits(listOf(visit))

        val exception = org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            service.getVisit(userId2, visitId)
        }
        assertEquals("Place visit not found for user", exception.message)
    }
}
