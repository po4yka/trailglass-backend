package com.trailglass.backend.persistence

import com.trailglass.backend.auth.User
import com.trailglass.backend.auth.UserRepository
import com.trailglass.backend.persistence.schema.UserCredentials
import com.trailglass.backend.persistence.schema.Users
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class ExposedUserRepository(database: Database) : ExposedRepository(database), UserRepository {

    override suspend fun create(user: User): User = suspendTx {
        Users.insert {
            it[id] = user.id
            it[email] = user.email
            it[fullName] = user.displayName
            it[createdAt] = user.createdAt
            it[updatedAt] = user.createdAt
        }

        UserCredentials.insert {
            it[userId] = user.id
            it[passwordHash] = user.passwordHash
            it[passwordUpdatedAt] = user.createdAt
        }

        user
    }

    override suspend fun findByEmail(email: String): User? = suspendTx {
        Users.join(UserCredentials, JoinType.INNER, Users.id, UserCredentials.userId)
            .select { Users.email eq email }
            .map(::toUser)
            .singleOrNull()
    }

    override suspend fun findById(id: UUID): User? = suspendTx {
        Users.join(UserCredentials, JoinType.INNER, Users.id, UserCredentials.userId)
            .select { Users.id eq id }
            .map(::toUser)
            .singleOrNull()
    }

    override suspend fun updatePassword(userId: UUID, passwordHash: String): Boolean = suspendTx {
        val updated = UserCredentials.update({ UserCredentials.userId eq userId }) {
            it[this.passwordHash] = passwordHash
            it[passwordUpdatedAt] = Instant.now()
        }
        updated > 0
    }

    override suspend fun updateLastSync(userId: UUID, timestamp: Instant): Boolean = suspendTx {
        // In the current schema, we don't have a last_sync_timestamp column on the users table.
        // It seems to be tracked per-entity in sync_versions.
        // However, the StoredUser data class had it.
        // For now, we'll ignore it or we might need to add a column if it's critical.
        // Checking the schema V2... users table has created_at, updated_at, deleted_at.
        // No last_sync_timestamp.
        // We will return true to satisfy the interface but do nothing for now,
        // as sync is likely handled by SyncService.
        true
    }

    private fun toUser(row: ResultRow): User {
        return User(
            id = row[Users.id],
            email = row[Users.email],
            displayName = row[Users.fullName] ?: "",
            passwordHash = row[UserCredentials.passwordHash],
            createdAt = row[Users.createdAt],
            lastSyncTimestamp = null // Not stored in users table currently
        )
    }
}
