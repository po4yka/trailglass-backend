package com.trailglass.backend.plugins

import com.trailglass.backend.auth.JwtProvider
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import org.koin.ktor.ext.inject

const val AuthJwt = "auth-jwt"

fun Application.configureAuthentication() {
    val jwtProvider by inject<JwtProvider>()

    install(Authentication) {
        jwt(AuthJwt) {
            verifier(jwtProvider.verifier())
            validate { credential ->
                val hasAudience = credential.payload.audience.contains(jwtProvider.audience)
                if (hasAudience) JWTPrincipal(credential.payload) else null
            }
        }
    }
}
