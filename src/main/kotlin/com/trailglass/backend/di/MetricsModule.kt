package com.trailglass.backend.di

import com.trailglass.backend.config.AppConfig
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.koin.dsl.module

val metricsModule = module {
    single<MeterRegistry> {
        val config = get<AppConfig>()
        if (config.enableMetrics) {
            PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        } else {
            SimpleMeterRegistry()
        }
    }
}
