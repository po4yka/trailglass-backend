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
import com.trailglass.backend.location.LocationService
import com.trailglass.backend.photo.PhotoService
import com.trailglass.backend.settings.SettingsService
import com.trailglass.backend.stubs.StubLocationService
import com.trailglass.backend.stubs.StubPhotoService
import com.trailglass.backend.stubs.StubSettingsService
import com.trailglass.backend.stubs.StubSyncService
import com.trailglass.backend.stubs.StubTripService
import com.trailglass.backend.sync.SyncService
import com.trailglass.backend.trip.TripService
import com.trailglass.backend.scheduler.RecurringTaskScheduler
import com.trailglass.backend.storage.InMemoryObjectStorageService
import com.trailglass.backend.storage.ObjectStorageService
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

val domainModule = module {
    single<SyncService> { StubSyncService() }
    single<LocationService> { StubLocationService() }
    single<TripService> { StubTripService() }
    single<PhotoService> { StubPhotoService() }
    single<SettingsService> { StubSettingsService() }
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
