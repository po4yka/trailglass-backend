package com.trailglass.backend.settings

import com.trailglass.backend.DatabaseTestFactory
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.UUID

class SettingsServiceTest {
    private val database = DatabaseTestFactory.inMemory()
    private val service = ExposedSettingsService(database)

    @Test
    fun `update settings increments version`() = runBlocking {
        val userId = UUID.randomUUID()
        val deviceId = UUID.randomUUID()
        val updated = service.updateSettings(SettingsUpdateRequest(userId, deviceId, mapOf("theme" to "dark")))
        assertEquals("dark", updated.preferences["theme"])
        assertNotNull(updated.serverVersion)

        val fetched = service.getSettings(userId)
        assertEquals(updated.preferences, fetched.preferences)
    }
}
