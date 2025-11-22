package com.trailglass.backend.auth

import org.koin.dsl.module

val authModule = module {
    single { JwtProvider(get()) }
    single<PasswordResetTokenRepository> { DefaultPasswordResetTokenRepository(get()) }
    single { PasswordResetTokenCleanupJob(get()) }
    single<AuthService> {
        DefaultAuthService(get(), get(), get()).also { service ->
            service.scheduleTokenCleanup(get(), get())
        }
    }
}
