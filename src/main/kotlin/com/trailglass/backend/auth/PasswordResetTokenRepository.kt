package com.trailglass.backend.auth

import com.trailglass.backend.persistence.ExposedRepository
import com.trailglass.backend.persistence.schema.PasswordResetTokens
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

interface PasswordResetTokenRepository {
    suspend fun create(userId: UUID, token: String, expiresAt: Instant): UUID
    suspend fun findByToken(token: String): PasswordResetToken?
    suspend fun markAsConsumed(tokenId: UUID): Boolean
    suspend fun deleteExpired(): Int
}

data class PasswordResetToken(
    val id: UUID,
    val userId: UUID,
    val token: String,
    val expiresAt: Instant,
    val createdAt: Instant,
    val consumedAt: Instant?
) {
    fun isExpired(now: Instant): Boolean = now.isAfter(expiresAt)
    fun isConsumed(): Boolean = consumedAt != null
}

class DefaultPasswordResetTokenRepository(
    database: Database
) : ExposedRepository(database), PasswordResetTokenRepository {

    override suspend fun create(userId: UUID, token: String, expiresAt: Instant): UUID = suspendTx {
        val tokenId = UUID.randomUUID()
        PasswordResetTokens.insert {
            it[id] = tokenId
            it[PasswordResetTokens.userId] = userId
            it[PasswordResetTokens.token] = token
            it[PasswordResetTokens.expiresAt] = expiresAt
        }
        tokenId
    }

    override suspend fun findByToken(token: String): PasswordResetToken? = suspendTx {
        PasswordResetTokens.select { PasswordResetTokens.token eq token }
            .limit(1)
            .map { row ->
                PasswordResetToken(
                    id = row[PasswordResetTokens.id].value,
                    userId = row[PasswordResetTokens.userId],
                    token = row[PasswordResetTokens.token],
                    expiresAt = row[PasswordResetTokens.expiresAt],
                    createdAt = row[PasswordResetTokens.createdAt],
                    consumedAt = row[PasswordResetTokens.consumedAt]
                )
            }
            .firstOrNull()
    }

    override suspend fun markAsConsumed(tokenId: UUID): Boolean = suspendTx {
        val updated = PasswordResetTokens.update({ PasswordResetTokens.id eq tokenId }) {
            it[consumedAt] = Instant.now()
        }
        updated > 0
    }

    override suspend fun deleteExpired(): Int = suspendTx {
        val now = Instant.now()
        PasswordResetTokens.deleteWhere {
            (expiresAt less now) and consumedAt.isNotNull()
        }
    }
}
