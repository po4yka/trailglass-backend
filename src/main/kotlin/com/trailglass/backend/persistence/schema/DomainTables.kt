package com.trailglass.backend.persistence.schema

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object LocationSamples : UUIDTable("location_samples") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val deviceId = reference("device_id", Devices, onDelete = ReferenceOption.CASCADE)
    val recordedAt = timestampWithTimeZone("recorded_at")
    val latitude = double("latitude")
    val longitude = double("longitude")
    val accuracy = double("accuracy").nullable()
    val altitude = double("altitude").nullable()
    val speed = double("speed").nullable()
    val bearing = double("bearing").nullable()
    val serverVersion = long("server_version").default(0)
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestampWithTimeZone("updated_at").defaultExpression(CurrentTimestamp())
    val deletedAt = timestampWithTimeZone("deleted_at").nullable()

    init {
        index("idx_location_samples_user_updated", false, userId, updatedAt)
        index("idx_location_samples_user_version", false, userId, serverVersion)
    }
}

object PlaceVisits : UUIDTable("place_visits") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val deviceId = reference("device_id", Devices, onDelete = ReferenceOption.CASCADE)
    val placeName = text("place_name").nullable()
    val arrivedAt = timestampWithTimeZone("arrived_at").nullable()
    val departedAt = timestampWithTimeZone("departed_at").nullable()
    val latitude = double("latitude").nullable()
    val longitude = double("longitude").nullable()
    val confidence = text("confidence").nullable()
    val serverVersion = long("server_version").default(0)
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestampWithTimeZone("updated_at").defaultExpression(CurrentTimestamp())
    val deletedAt = timestampWithTimeZone("deleted_at").nullable()

    init {
        index("idx_place_visits_user_updated", false, userId, updatedAt)
        index("idx_place_visits_user_version", false, userId, serverVersion)
    }
}

object RouteSegments : UUIDTable("route_segments") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val deviceId = reference("device_id", Devices, onDelete = ReferenceOption.CASCADE)
    val startedAt = timestampWithTimeZone("started_at").nullable()
    val endedAt = timestampWithTimeZone("ended_at").nullable()
    val mode = text("mode").nullable()
    val polyline = text("polyline").nullable()
    val serverVersion = long("server_version").default(0)
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestampWithTimeZone("updated_at").defaultExpression(CurrentTimestamp())
    val deletedAt = timestampWithTimeZone("deleted_at").nullable()

    init {
        index("idx_route_segments_user_updated", false, userId, updatedAt)
        index("idx_route_segments_user_version", false, userId, serverVersion)
    }
}

object Trips : UUIDTable("trips") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val deviceId = reference("device_id", Devices, onDelete = ReferenceOption.CASCADE)
    val title = text("title")
    val startedAt = timestampWithTimeZone("started_at").nullable()
    val endedAt = timestampWithTimeZone("ended_at").nullable()
    val notes = text("notes").nullable()
    val serverVersion = long("server_version").default(0)
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestampWithTimeZone("updated_at").defaultExpression(CurrentTimestamp())
    val deletedAt = timestampWithTimeZone("deleted_at").nullable()

    init {
        index("idx_trips_user_updated", false, userId, updatedAt)
        index("idx_trips_user_version", false, userId, serverVersion)
    }
}

object TripDays : UUIDTable("trip_days") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val deviceId = reference("device_id", Devices, onDelete = ReferenceOption.CASCADE)
    val tripId = reference("trip_id", Trips, onDelete = ReferenceOption.CASCADE)
    val dayDate = date("day_date")
    val notes = text("notes").nullable()
    val serverVersion = long("server_version").default(0)
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestampWithTimeZone("updated_at").defaultExpression(CurrentTimestamp())
    val deletedAt = timestampWithTimeZone("deleted_at").nullable()

    init {
        index("idx_trip_days_user_updated", false, userId, updatedAt)
        index("idx_trip_days_user_version", false, userId, serverVersion)
    }
}

object Memories : UUIDTable("memories") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val deviceId = reference("device_id", Devices, onDelete = ReferenceOption.CASCADE)
    val tripId = reference("trip_id", Trips, onDelete = ReferenceOption.CASCADE).nullable()
    val title = text("title").nullable()
    val body = text("body").nullable()
    val serverVersion = long("server_version").default(0)
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestampWithTimeZone("updated_at").defaultExpression(CurrentTimestamp())
    val deletedAt = timestampWithTimeZone("deleted_at").nullable()

    init {
        index("idx_memories_user_updated", false, userId, updatedAt)
        index("idx_memories_user_version", false, userId, serverVersion)
    }
}

object JournalEntries : UUIDTable("journal_entries") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val deviceId = reference("device_id", Devices, onDelete = ReferenceOption.CASCADE)
    val tripDayId = reference("trip_day_id", TripDays, onDelete = ReferenceOption.CASCADE).nullable()
    val title = text("title").nullable()
    val body = text("body").nullable()
    val serverVersion = long("server_version").default(0)
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestampWithTimeZone("updated_at").defaultExpression(CurrentTimestamp())
    val deletedAt = timestampWithTimeZone("deleted_at").nullable()

    init {
        index("idx_journal_entries_user_updated", false, userId, updatedAt)
        index("idx_journal_entries_user_version", false, userId, serverVersion)
    }
}

object PhotoAttachments : UUIDTable("photo_attachments") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val deviceId = reference("device_id", Devices, onDelete = ReferenceOption.CASCADE)
    val memoryId = reference("memory_id", Memories, onDelete = ReferenceOption.CASCADE).nullable()
    val journalEntryId = reference("journal_entry_id", JournalEntries, onDelete = ReferenceOption.CASCADE).nullable()
    val storageKey = text("storage_key").nullable()
    val mimeType = text("mime_type").nullable()
    val sizeBytes = long("size_bytes").nullable()
    val capturedAt = timestampWithTimeZone("captured_at").nullable()
    val serverVersion = long("server_version").default(0)
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestampWithTimeZone("updated_at").defaultExpression(CurrentTimestamp())
    val deletedAt = timestampWithTimeZone("deleted_at").nullable()

    init {
        index("idx_photo_attachments_user_updated", false, userId, updatedAt)
        index("idx_photo_attachments_user_version", false, userId, serverVersion)
    }
}

object Memorabilia : UUIDTable("memorabilia") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val deviceId = reference("device_id", Devices, onDelete = ReferenceOption.CASCADE)
    val tripId = reference("trip_id", Trips, onDelete = ReferenceOption.CASCADE).nullable()
    val title = text("title").nullable()
    val description = text("description").nullable()
    val serverVersion = long("server_version").default(0)
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestampWithTimeZone("updated_at").defaultExpression(CurrentTimestamp())
    val deletedAt = timestampWithTimeZone("deleted_at").nullable()

    init {
        index("idx_memorabilia_user_updated", false, userId, updatedAt)
        index("idx_memorabilia_user_version", false, userId, serverVersion)
    }
}

object CountryVisits : UUIDTable("country_visits") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val deviceId = reference("device_id", Devices, onDelete = ReferenceOption.CASCADE)
    val tripId = reference("trip_id", Trips, onDelete = ReferenceOption.CASCADE).nullable()
    val countryCode = text("country_code")
    val visitedAt = date("visited_at").nullable()
    val serverVersion = long("server_version").default(0)
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(CurrentTimestamp())
    val updatedAt = timestampWithTimeZone("updated_at").defaultExpression(CurrentTimestamp())
    val deletedAt = timestampWithTimeZone("deleted_at").nullable()

    init {
        index("idx_country_visits_user_updated", false, userId, updatedAt)
        index("idx_country_visits_user_version", false, userId, serverVersion)
    }
}
