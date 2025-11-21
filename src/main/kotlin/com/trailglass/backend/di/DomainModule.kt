package com.trailglass.backend.di

import com.trailglass.backend.export.ExportService
import com.trailglass.backend.location.ExposedLocationService
import com.trailglass.backend.location.LocationService
import com.trailglass.backend.photo.PhotoService
import com.trailglass.backend.settings.ExposedSettingsService
import com.trailglass.backend.settings.SettingsService
import com.trailglass.backend.stubs.StubExportService
import com.trailglass.backend.stubs.StubPhotoService
import com.trailglass.backend.sync.SyncServiceImpl
import com.trailglass.backend.sync.SyncService
import com.trailglass.backend.trip.ExposedTripService
import com.trailglass.backend.trip.TripService
import com.trailglass.backend.user.ExposedUserProfileService
import com.trailglass.backend.user.UserProfileService
import com.trailglass.backend.visit.ExposedPlaceVisitService
import com.trailglass.backend.visit.PlaceVisitService
import org.koin.dsl.module

val domainModule = module {
    single<SyncService> { SyncServiceImpl(get()) }
    single<LocationService> { ExposedLocationService(get()) }
    single<TripService> { ExposedTripService(get()) }
    single<PhotoService> { StubPhotoService() }
    single<SettingsService> { ExposedSettingsService(get()) }
    single<ExportService> { StubExportService() }
    single<PlaceVisitService> { ExposedPlaceVisitService(get()) }
    single<UserProfileService> { ExposedUserProfileService(get()) }
}
