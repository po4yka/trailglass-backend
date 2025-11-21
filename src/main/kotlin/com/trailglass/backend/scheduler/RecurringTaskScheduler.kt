package com.trailglass.backend.scheduler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

class RecurringTaskScheduler(private val scope: CoroutineScope) {
    private val logger = LoggerFactory.getLogger(RecurringTaskScheduler::class.java)
    private val tasks = ConcurrentHashMap<String, Job>()

    fun schedule(
        name: String,
        interval: Duration,
        initialDelay: Duration = Duration.ZERO,
        block: suspend CoroutineScope.() -> Unit,
    ) {
        if (tasks.containsKey(name)) {
            logger.warn("Task {} is already scheduled; ignoring duplicate registration", name)
            return
        }

        tasks[name] = scope.launch {
            if (!initialDelay.isZero) {
                delay(initialDelay)
            }

            while (isActive) {
                val startedAt = Instant.now()
                try {
                    block()
                } catch (ex: Exception) {
                    logger.error("Recurring task {} failed", name, ex)
                }

                val elapsedMillis = Duration.between(startedAt, Instant.now()).toMillis()
                val sleepMillis = max(0, interval.toMillis() - elapsedMillis)
                delay(Duration.ofMillis(sleepMillis))
            }
        }
    }

    suspend fun stop(name: String) {
        tasks.remove(name)?.cancelAndJoin()
    }

    suspend fun shutdown() {
        tasks.keys.toList().forEach { stop(it) }
    }
}
