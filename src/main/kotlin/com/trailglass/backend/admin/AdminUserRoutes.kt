@file:UseSerializers(UUIDSerializer::class, InstantSerializer::class)

package com.trailglass.backend.admin

import com.trailglass.backend.common.InstantSerializer
import com.trailglass.backend.common.UUIDSerializer
import com.trailglass.backend.persistence.dbQuery
import com.trailglass.backend.persistence.schema.Users
import com.trailglass.backend.plugins.DefaultFeatureRateLimit
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

@Serializable
data class AdminUserResponse(
    val id: UUID,
    val email: String,
    val fullName: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
data class AdminUserListResponse(
    val users: List<AdminUserResponse>,
    val pagination: PaginationInfo
)

@Serializable
data class PaginationInfo(
    val total: Int,
    val limit: Int,
    val offset: Int
)

@Serializable
data class UpdateUserRequest(
    val email: String?,
    val fullName: String?
)

private fun ResultRow.toAdminUserResponse() = AdminUserResponse(
    id = this[Users.id].value,
    email = this[Users.email],
    fullName = this[Users.fullName],
    createdAt = this[Users.createdAt],
    updatedAt = this[Users.updatedAt]
)

fun Route.adminUserRoutes() {
    rateLimit(DefaultFeatureRateLimit) {
        route("/users") {
            // List all users with pagination
            get {
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

                val users = dbQuery {
                    Users.selectAll()
                        .where { Users.deletedAt.isNull() }
                        .limit(limit, offset.toLong())
                        .map { it.toAdminUserResponse() }
                }

                val total = dbQuery {
                    Users.selectAll()
                        .where { Users.deletedAt.isNull() }
                        .count()
                        .toInt()
                }

                call.respond(
                    AdminUserListResponse(
                        users = users,
                        pagination = PaginationInfo(
                            total = total,
                            limit = limit,
                            offset = offset
                        )
                    )
                )
            }

            // Get single user by ID
            get("/{id}") {
                val userId = try {
                    call.parameters["id"]?.let { UUID.fromString(it) }
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID format"))
                    return@get
                }

                if (userId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "User ID is required"))
                    return@get
                }

                val user = dbQuery {
                    Users.selectAll()
                        .where { (Users.id eq userId) and Users.deletedAt.isNull() }
                        .map { it.toAdminUserResponse() }
                        .singleOrNull()
                }

                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                } else {
                    call.respond(user)
                }
            }

            // Update user (email, fullName only - no password changes)
            put("/{id}") {
                val userId = try {
                    call.parameters["id"]?.let { UUID.fromString(it) }
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID format"))
                    return@put
                }

                if (userId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "User ID is required"))
                    return@put
                }

                val request = call.receive<UpdateUserRequest>()

                val updated = dbQuery {
                    Users.update({ (Users.id eq userId) and Users.deletedAt.isNull() }) { statement ->
                        if (request.email != null) {
                            statement[email] = request.email
                        }
                        if (request.fullName != null) {
                            statement[fullName] = request.fullName
                        }
                        statement[updatedAt] = Instant.now()
                    }
                }

                if (updated == 0) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                } else {
                    // Fetch and return the updated user
                    val user = dbQuery {
                        Users.selectAll()
                            .where { Users.id eq userId }
                            .map { it.toAdminUserResponse() }
                            .single()
                    }
                    call.respond(user)
                }
            }

            // Create user - delegate to auth/register
            post {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "error" to "User creation not available through admin API",
                        "message" to "Please use /auth/register endpoint to create new users"
                    )
                )
            }

            // Delete user (soft delete)
            delete("/{id}") {
                val userId = try {
                    call.parameters["id"]?.let { UUID.fromString(it) }
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID format"))
                    return@delete
                }

                if (userId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "User ID is required"))
                    return@delete
                }

                val deleted = dbQuery {
                    Users.update({ (Users.id eq userId) and Users.deletedAt.isNull() }) { statement ->
                        statement[deletedAt] = Instant.now()
                        statement[updatedAt] = Instant.now()
                    }
                }

                if (deleted == 0) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                } else {
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}
