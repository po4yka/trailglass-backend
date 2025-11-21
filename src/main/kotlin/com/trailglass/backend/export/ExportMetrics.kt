package com.trailglass.backend.export

import io.micrometer.core.instrument.MeterRegistry

class ExportMetrics(registry: MeterRegistry) {
    private val requested = registry.counter("exports.requested")
    private val started = registry.counter("exports.started")
    private val completed = registry.counter("exports.completed")
    private val failed = registry.counter("exports.failed")
    private val expired = registry.counter("exports.expired")

    fun markRequested() = requested.increment()
    fun markStarted() = started.increment()
    fun markCompleted() = completed.increment()
    fun markFailed() = failed.increment()
    fun markExpired(count: Int) { repeat(count) { expired.increment() } }
}
