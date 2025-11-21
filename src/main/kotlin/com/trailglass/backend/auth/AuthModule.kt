package com.trailglass.backend.auth

import org.koin.dsl.module

val authModule = module {
    single { JwtProvider(get()) }
    single<AuthService> { DefaultAuthService(get()) }
}
