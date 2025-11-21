package com.trailglass.backend.di

import com.trailglass.backend.email.DefaultEmailService
import com.trailglass.backend.email.EmailService
import com.trailglass.backend.email.LoggingEmailSender
import com.trailglass.backend.email.SendGridEmailSender
import com.trailglass.backend.email.SesEmailSender
import com.trailglass.backend.email.SmtpEmailSender
import com.trailglass.backend.export.DefaultExportService
import com.trailglass.backend.export.ExportHousekeepingJob
import com.trailglass.backend.export.ExportJobRepository
import com.trailglass.backend.export.ExportMetrics
import com.trailglass.backend.export.ExportService
import com.trailglass.backend.export.InMemoryExportJobRepository
import com.trailglass.backend.location.ExposedLocationService
import com.trailglass.backend.location.LocationService
import com.trailglass.backend.photo.PhotoService
import com.trailglass.backend.settings.ExposedSettingsService
import com.trailglass.backend.settings.SettingsService
import com.trailglass.backend.stubs.StubPhotoService
import com.trailglass.backend.sync.SyncServiceImpl
import com.trailglass.backend.sync.SyncService
import com.trailglass.backend.trip.ExposedTripService
import com.trailglass.backend.trip.TripService
import com.trailglass.backend.user.ExposedUserProfileService
import com.trailglass.backend.user.UserProfileService
import com.trailglass.backend.visit.ExposedPlaceVisitService
import com.trailglass.backend.visit.PlaceVisitService
import com.trailglass.backend.scheduler.RecurringTaskScheduler
import com.trailglass.backend.storage.InMemoryObjectStorageService
import com.trailglass.backend.storage.ObjectStorageService
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

val domainModule = module {
    single<SyncService> { SyncServiceImpl(get()) }
    single<LocationService> { ExposedLocationService(get()) }
    single<TripService> { ExposedTripService(get()) }
    single<PhotoService> { StubPhotoService() }
    single<SettingsService> { ExposedSettingsService(get()) }
    single<PlaceVisitService> { ExposedPlaceVisitService(get()) }
    single<UserProfileService> { ExposedUserProfileService(get()) }

    single<EmailService> {
        DefaultEmailService(
            listOf(
                SendGridEmailSender(),
                SesEmailSender(),
                SmtpEmailSender(),
                LoggingEmailSender(),
            ),
        )
    }

    single<ObjectStorageService> { InMemoryObjectStorageService() }
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single { RecurringTaskScheduler(get()) }
    single { ExportMetrics(SimpleMeterRegistry()) }
    single<ExportJobRepository> { InMemoryExportJobRepository() }
    single { ExportHousekeepingJob(get(), get(), get()) }
    single<ExportService> {
        DefaultExportService(get(), get(), get(), get(), get(), get()).also { service ->
            service.scheduleHousekeeping(get())
        }
    }
}
