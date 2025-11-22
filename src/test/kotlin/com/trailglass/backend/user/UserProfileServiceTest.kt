package com.trailglass.backend.user

import com.trailglass.backend.DatabaseTestFactory
import com.trailglass.backend.persistence.Photos
import com.trailglass.backend.persistence.schema.CountryVisits
import com.trailglass.backend.persistence.schema.Devices
import com.trailglass.backend.persistence.schema.LocationSamples
import com.trailglass.backend.persistence.schema.PhotoAttachments
import com.trailglass.backend.persistence.schema.PlaceVisits
import com.trailglass.backend.persistence.schema.Trips
import com.trailglass.backend.persistence.schema.Users
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
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

    @Test
    fun `get profile returns existing profile`() = runBlocking {
        val userId = UUID.randomUUID()
        val profile = UserProfile(userId, "test@example.com", "Test User", Instant.now(), null, 0)
        service.upsertProfile(profile)

        val retrieved = service.getProfile(userId)
        assertEquals(profile.id, retrieved?.id)
        assertEquals(profile.email, retrieved?.email)
        assertEquals(profile.displayName, retrieved?.displayName)
    }

    @Test
    fun `get profile returns null for non-existent user`() = runBlocking {
        val userId = UUID.randomUUID()
        val retrieved = service.getProfile(userId)
        assertEquals(null, retrieved)
    }

    @Test
    fun `getUserStatistics returns zeros for user with no data`() = runBlocking {
        val userId = UUID.randomUUID()
        val profile = UserProfile(userId, "stats@example.com", "Stats User", Instant.now(), null, 0)
        service.upsertProfile(profile)

        val statistics = service.getUserStatistics(userId)
        assertEquals(0, statistics.totalLocations)
        assertEquals(0, statistics.totalPlaceVisits)
        assertEquals(0, statistics.totalTrips)
        assertEquals(0, statistics.totalPhotos)
        assertEquals(0, statistics.countriesVisited)
        assertEquals(0.0, statistics.totalDistance)
    }

    @Test
    fun `getUserStatistics calculates correct counts`() = runBlocking {
        val userId = UUID.randomUUID()
        val deviceId = UUID.randomUUID()
        val now = OffsetDateTime.now(ZoneOffset.UTC)

        // Setup database tables and test data
        transaction(database) {
            SchemaUtils.create(Users, Devices, LocationSamples, PlaceVisits, Trips, Photos, PhotoAttachments, CountryVisits)

            // Create user and device
            Users.insert {
                it[id] = userId
                it[email] = "test-stats@example.com"
                it[fullName] = "Stats Test User"
                it[createdAt] = now
                it[updatedAt] = now
            }

            Devices.insert {
                it[id] = deviceId
                it[this.userId] = userId
                it[label] = "Test Device"
                it[createdAt] = now
                it[updatedAt] = now
            }

            // Insert 3 location samples
            repeat(3) { i ->
                LocationSamples.insert {
                    it[id] = UUID.randomUUID()
                    it[this.userId] = userId
                    it[this.deviceId] = deviceId
                    it[recordedAt] = now
                    it[latitude] = 37.7749 + i * 0.01
                    it[longitude] = -122.4194 + i * 0.01
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }

            // Insert 2 place visits
            repeat(2) { i ->
                PlaceVisits.insert {
                    it[id] = UUID.randomUUID()
                    it[this.userId] = userId
                    it[this.deviceId] = deviceId
                    it[placeName] = "Place $i"
                    it[arrivedAt] = now
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }

            // Insert 2 trips
            repeat(2) { i ->
                Trips.insert {
                    it[id] = UUID.randomUUID()
                    it[this.userId] = userId
                    it[this.deviceId] = deviceId
                    it[title] = "Trip $i"
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }

            // Insert 3 photos
            repeat(3) { i ->
                Photos.insert {
                    it[id] = UUID.randomUUID()
                    it[this.userId] = userId
                    it[this.deviceId] = deviceId
                    it[fileName] = "photo$i.jpg"
                    it[mimeType] = "image/jpeg"
                    it[sizeBytes] = 1024L
                    it[storageKey] = "key$i"
                    it[storageBackend] = "s3"
                    it[updatedAt] = Instant.now()
                }
            }

            // Insert 2 photo attachments
            repeat(2) { i ->
                PhotoAttachments.insert {
                    it[id] = UUID.randomUUID()
                    it[this.userId] = userId
                    it[this.deviceId] = deviceId
                    it[storageKey] = "attachment$i"
                    it[mimeType] = "image/jpeg"
                    it[sizeBytes] = 2048L
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }

            // Insert 3 country visits (2 unique countries)
            CountryVisits.insert {
                it[id] = UUID.randomUUID()
                it[this.userId] = userId
                it[this.deviceId] = deviceId
                it[countryCode] = "US"
                it[createdAt] = now
                it[updatedAt] = now
            }
            CountryVisits.insert {
                it[id] = UUID.randomUUID()
                it[this.userId] = userId
                it[this.deviceId] = deviceId
                it[countryCode] = "CA"
                it[createdAt] = now
                it[updatedAt] = now
            }
            CountryVisits.insert {
                it[id] = UUID.randomUUID()
                it[this.userId] = userId
                it[this.deviceId] = deviceId
                it[countryCode] = "US"
                it[createdAt] = now
                it[updatedAt] = now
            }
        }

        val statistics = service.getUserStatistics(userId)
        assertEquals(3, statistics.totalLocations)
        assertEquals(2, statistics.totalPlaceVisits)
        assertEquals(2, statistics.totalTrips)
        assertEquals(5, statistics.totalPhotos) // 3 from Photos + 2 from PhotoAttachments
        assertEquals(2, statistics.countriesVisited) // 2 distinct countries: US and CA
        assertEquals(0.0, statistics.totalDistance)
    }

    @Test
    fun `getUserStatistics excludes soft-deleted records`() = runBlocking {
        val userId = UUID.randomUUID()
        val deviceId = UUID.randomUUID()
        val now = OffsetDateTime.now(ZoneOffset.UTC)

        transaction(database) {
            SchemaUtils.create(Users, Devices, LocationSamples, PlaceVisits, Trips, Photos)

            Users.insert {
                it[id] = userId
                it[email] = "delete-test@example.com"
                it[fullName] = "Delete Test"
                it[createdAt] = now
                it[updatedAt] = now
            }

            Devices.insert {
                it[id] = deviceId
                it[this.userId] = userId
                it[label] = "Test Device"
                it[createdAt] = now
                it[updatedAt] = now
            }

            // Insert 2 locations, 1 deleted
            LocationSamples.insert {
                it[id] = UUID.randomUUID()
                it[this.userId] = userId
                it[this.deviceId] = deviceId
                it[recordedAt] = now
                it[latitude] = 37.7749
                it[longitude] = -122.4194
                it[createdAt] = now
                it[updatedAt] = now
            }
            LocationSamples.insert {
                it[id] = UUID.randomUUID()
                it[this.userId] = userId
                it[this.deviceId] = deviceId
                it[recordedAt] = now
                it[latitude] = 37.7750
                it[longitude] = -122.4195
                it[deletedAt] = now // Soft-deleted
                it[createdAt] = now
                it[updatedAt] = now
            }

            // Insert 2 trips, 1 deleted
            Trips.insert {
                it[id] = UUID.randomUUID()
                it[this.userId] = userId
                it[this.deviceId] = deviceId
                it[title] = "Active Trip"
                it[createdAt] = now
                it[updatedAt] = now
            }
            Trips.insert {
                it[id] = UUID.randomUUID()
                it[this.userId] = userId
                it[this.deviceId] = deviceId
                it[title] = "Deleted Trip"
                it[deletedAt] = now
                it[createdAt] = now
                it[updatedAt] = now
            }
        }

        val statistics = service.getUserStatistics(userId)
        assertEquals(1, statistics.totalLocations)
        assertEquals(1, statistics.totalTrips)
    }
}
