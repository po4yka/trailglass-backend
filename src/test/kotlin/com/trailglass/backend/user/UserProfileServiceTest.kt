package com.trailglass.backend.user

import com.trailglass.backend.DatabaseTestFactory
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class UserProfileServiceTest {
    private val database = DatabaseTestFactory.inMemory()
    private val service = ExposedUserProfileService(database)

    @Test
    fun `register device and soft delete`() = runBlocking {
        val userId = UUID.randomUUID()
        val profile = UserProfile(userId, "user@example.com", "Test User", Instant.now(), null, 0)
        service.upsertProfile(profile)

        val device = DeviceProfile(UUID.randomUUID(), userId, "Pixel", "Android", "14", "1.0", Instant.now(), null, 0)
        val saved = service.registerDevice(device)
        assertTrue(saved.serverVersion > 0)

        val listed = service.listDevices(userId, null, 10)
        assertEquals(1, listed.size)

        val deleted = service.deleteDevice(userId, device.id)
        assertTrue(deleted?.deletedAt != null)
    }
}
