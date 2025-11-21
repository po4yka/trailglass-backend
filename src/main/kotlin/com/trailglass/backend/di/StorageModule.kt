package com.trailglass.backend.di

import com.trailglass.backend.config.AppConfig
import com.trailglass.backend.config.StorageBackend
import com.trailglass.backend.storage.InlineUrlSigner
import com.trailglass.backend.storage.ObjectStorageService
import com.trailglass.backend.storage.PostgresObjectStorageService
import com.trailglass.backend.storage.S3ObjectStorageService
import org.koin.dsl.module

val storageModule = module {
    single { get<AppConfig>().storage }
    single { InlineUrlSigner(get<AppConfig>().storage.signingSecret ?: get<AppConfig>().jwtSecret!!) }

    single<ObjectStorageService> {
        val config = get<AppConfig>().storage
        when (config.backend) {
            StorageBackend.S3 -> S3ObjectStorageService(config)
            StorageBackend.DATABASE -> PostgresObjectStorageService(get(), get())
        }
    }
}
