package com.trailglass.backend.di

import com.trailglass.backend.auth.authModule
import com.trailglass.backend.config.AppConfig
import com.trailglass.backend.email.emailModule
import com.trailglass.backend.persistence.persistenceModule
import org.flywaydb.core.Flyway
import org.koin.dsl.module
import javax.sql.DataSource

fun appModule(config: AppConfig, dataSource: DataSource, flyway: Flyway) =
    module {
        single { config }
        single { flyway }
    } + persistenceModule(dataSource) + authModule + storageModule + metricsModule + domainModule + emailModule
