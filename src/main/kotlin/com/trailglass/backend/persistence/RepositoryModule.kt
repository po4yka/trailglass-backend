package com.trailglass.backend.persistence

import org.jetbrains.exposed.sql.Database
import org.koin.core.module.Module
import org.koin.dsl.module
import javax.sql.DataSource

fun persistenceModule(dataSource: DataSource): Module = module {
    single<DataSource> { dataSource }
    single<Database> { DatabaseFactory.connect(get()) }
    single { PhotoRepository(get()) }
}
