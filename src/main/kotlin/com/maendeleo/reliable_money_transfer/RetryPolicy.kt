package com.maendeleo.reliable_money_transfer

import kotlin.math.min
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class RetryDecision(val shouldRetry: Boolean, val delay: Duration)

/** Exponential backoff plus jitter avoids retry storms. */
class RetryPolicy(
    private val maxAttempts: Int = 8,
    private val baseDelay: Duration = 1.seconds,
    private val maxDelay: Duration = 60.seconds,
    private val random: Random = Random.Default,
) {
    fun next(attemptThatFailed: Int): RetryDecision {
        if (attemptThatFailed >= maxAttempts) return RetryDecision(false, Duration.ZERO)
        val exponentialSeconds = baseDelay.inWholeSeconds * (1L shl min(attemptThatFailed - 1, 6))
        val cappedSeconds = min(exponentialSeconds, maxDelay.inWholeSeconds)
        val jitterSeconds = if (cappedSeconds == 0L) 0L else random.nextLong(cappedSeconds + 1)
        return RetryDecision(true, (cappedSeconds + jitterSeconds).seconds)
    }
}

