package com.trailglass.backend.auth

import com.trailglass.backend.plugins.AuthJwt
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.authRoutes() {
    val authService by inject<AuthService>()

    route("/auth") {
        post("/register") {
            val request = call.receive<RegisterRequest>()
            call.respond(authService.register(request))
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            call.respond(authService.login(request))
        }

        post("/refresh") {
            val request = call.receive<RefreshRequest>()
            call.respond(authService.refresh(request))
        }

        authenticate(AuthJwt) {
            post("/logout") {
                val request = call.receive<RefreshRequest>()
                authService.logout(request.refreshToken, request.deviceId)
                call.respond(HttpStatusCode.NoContent)
            }
        }

        post("/reset-password-request") {
            val request = call.receive<PasswordResetRequest>()
            authService.requestPasswordReset(request.email)
            call.respond(HttpStatusCode.Accepted)
        }

        post("/reset-password") {
            val request = call.receive<PasswordResetConfirm>()
            authService.resetPassword(request.token, request.newPassword)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
