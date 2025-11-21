package com.trailglass.backend.di

import com.trailglass.backend.export.ExportService
import com.trailglass.backend.location.LocationService
import com.trailglass.backend.photo.PhotoService
import com.trailglass.backend.photo.PhotoServiceImpl
import com.trailglass.backend.settings.SettingsService
import com.trailglass.backend.stubs.StubExportService
import com.trailglass.backend.stubs.StubLocationService
import com.trailglass.backend.stubs.StubSettingsService
import com.trailglass.backend.stubs.StubSyncService
import com.trailglass.backend.stubs.StubTripService
import com.trailglass.backend.sync.SyncService
import com.trailglass.backend.trip.TripService
import org.koin.dsl.module

val domainModule = module {
    single<SyncService> { StubSyncService() }
    single<LocationService> { StubLocationService() }
    single<TripService> { StubTripService() }
    single<PhotoService> { PhotoServiceImpl(get(), get(), get()) }
    single<SettingsService> { StubSettingsService() }
    single<ExportService> { StubExportService() }
}
