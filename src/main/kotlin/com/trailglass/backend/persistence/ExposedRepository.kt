package com.trailglass.backend.persistence

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

abstract class ExposedRepository(protected val database: Database) {
    protected fun <T> tx(block: Transaction.() -> T): T = transaction(database) { block() }

    protected suspend fun <T> suspendTx(block: Transaction.() -> T): T =
        newSuspendedTransaction(Dispatchers.IO, db = database) { block() }

    protected fun ensureTables(vararg tables: org.jetbrains.exposed.sql.Table) {
        tx { SchemaUtils.create(*tables) }
    }
}
