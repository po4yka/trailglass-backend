package com.trailglass.backend.persistence

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction

abstract class ExposedRepository(private val database: Database) {
    protected fun <T> tx(block: Transaction.() -> T): T = transaction(database) { block() }

    protected fun ensureTables(vararg tables: org.jetbrains.exposed.sql.Table) {
        tx { SchemaUtils.create(*tables) }
    }
}
