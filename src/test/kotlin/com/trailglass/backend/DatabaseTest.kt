package com.trailglass.backend

import org.jetbrains.exposed.sql.Database

object DatabaseTestFactory {
    fun inMemory(): Database = Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
}
