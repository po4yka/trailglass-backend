package com.trailglass.backend.auth

import java.time.Instant
import java.util.UUID

interface UserRepository {
    suspend fun create(user: User): User
    suspend fun findByEmail(email: String): User?
    suspend fun findById(id: UUID): User?
    suspend fun updatePassword(userId: UUID, passwordHash: String): Boolean
    suspend fun updateLastSync(userId: UUID, timestamp: Instant): Boolean
}

data class User(
    val id: UUID,
    val email: String,
    val displayName: String,
    val passwordHash: String,
    val createdAt: Instant,
    val lastSyncTimestamp: Instant?,
)
