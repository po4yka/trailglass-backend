package com.trailglass.backend.auth

import com.trailglass.backend.persistence.ExposedUserRepository
import org.koin.dsl.module

val authModule = module {
    single { JwtProvider(get()) }
    single<PasswordResetTokenRepository> { DefaultPasswordResetTokenRepository(get()) }
    single { PasswordResetTokenCleanupJob(get()) }
    single<UserRepository> { ExposedUserRepository(get()) }
    single<AuthService> {
        val config = get<com.trailglass.backend.config.AppConfig>()
        DefaultAuthService(get(), get(), get(), get(), config.argon2).also { service ->
            service.scheduleTokenCleanup(get(), get())
        }
    }
}
