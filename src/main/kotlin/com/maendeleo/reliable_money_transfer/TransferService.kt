package com.maendeleo.reliable_money_transfer

import java.time.Clock
import java.time.Instant
import java.util.UUID

/** UNKNOWN_OUTCOME tells the UI to show processing, not failure. */
/** A safe UI-facing outcome. UNKNOWN_OUTCOME means the app must continue reconciliation. */
data class TransferResult(val status: TransferStatus, val providerTransferId: String? = null, val message: String? = null)

/**
 * Coordinates durable state, provider calls, and retry policy for one logical transfer.
 * The service never creates a new idempotency key while resuming a transfer.
 */
class TransferService(
    private val repository: TransferRepository,
    private val provider: ProviderClient,
    private val retryPolicy: RetryPolicy = RetryPolicy(),
    private val clock: Clock = Clock.systemUTC(),
    private val sleep: (Long) -> Unit = Thread::sleep,
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
) {
    /** Starts a transfer, or returns the existing record for a duplicate client reference. */
    fun send(request: TransferRequest): TransferResult {
        // A client reference makes a double tap return the existing logical operation.
        repository.findByClientReference(request.clientReference)?.let { return it.toResult() }
        val initial = TransferRecord(idGenerator(), request, TransferStatus.PENDING, updatedAt = clock.instant())
        repository.save(initial)
        return resume(initial)
    }

    /** Safe to call again from a background worker after process death. */
    fun resume(record: TransferRecord): TransferResult {
        var current = record
        while (current.status == TransferStatus.PENDING || current.status == TransferStatus.UNKNOWN_OUTCOME) {
            if (current.status == TransferStatus.UNKNOWN_OUTCOME) {
                // A timeout may mean the provider committed. Resolve before any new POST.
                try {
                    provider.findByIdempotencyKey(current.idempotencyKey)?.let { result ->
                        return save(current.copy(status = TransferStatus.SUCCEEDED, providerTransferId = result.providerTransferId, providerMessage = result.message)).toResult()
                    }
                } catch (failure: ProviderFailure) {
                    // The provider is still unreachable. Keep the operation resumable instead of returning 500.
                    current = save(current.copy(providerMessage = failure.message))
                    val decision = retryPolicy.next(maxOf(1, current.attemptCount))
                    if (!decision.shouldRetry) return current.toResult()
                    sleep(decision.delay.inWholeMilliseconds)
                    continue
                }
                current = save(current.copy(status = TransferStatus.PENDING))
            }
            val attempt = current.attemptCount + 1
            try {
                val result = provider.submit(current.request, current.idempotencyKey)
                // The provider acknowledged success, so stop immediately.
                return save(current.copy(status = TransferStatus.SUCCEEDED, providerTransferId = result.providerTransferId, providerMessage = result.message, attemptCount = attempt)).toResult()
            } catch (failure: ProviderFailure) {
                when (failure) {
                    is ProviderFailure.Client -> return save(current.copy(status = TransferStatus.FAILED, providerMessage = failure.message, attemptCount = attempt)).toResult()
                    is ProviderFailure.Timeout, is ProviderFailure.Network -> {
                        // Never blindly replay a request whose outcome is unknown.
                        current = save(current.copy(status = TransferStatus.UNKNOWN_OUTCOME, attemptCount = attempt, providerMessage = failure.message))
                    }
                    is ProviderFailure.Server -> current = save(current.copy(attemptCount = attempt, providerMessage = failure.message))
                }
            }
            val decision = retryPolicy.next(current.attemptCount)
            if (!decision.shouldRetry) return current.toResult()
            sleep(decision.delay.inWholeMilliseconds)
        }
        return current.toResult()
    }

    private fun save(record: TransferRecord): TransferRecord = record.copy(updatedAt = Instant.now(clock)).also(repository::save)
    private fun TransferRecord.toResult() = TransferResult(status, providerTransferId, providerMessage)
}

