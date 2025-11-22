package com.trailglass.backend.di

import com.trailglass.backend.health.HealthCheckService
import com.trailglass.backend.storage.ObjectStorageService
import org.koin.dsl.module
import javax.sql.DataSource

val healthModule = module {
    single {
        val config = get<com.trailglass.backend.config.AppConfig>()
        HealthCheckService(
            dataSource = get<DataSource>(),
            storageService = get<ObjectStorageService>(),
            emailEnabled = config.email.enabled
        )
    }
}
