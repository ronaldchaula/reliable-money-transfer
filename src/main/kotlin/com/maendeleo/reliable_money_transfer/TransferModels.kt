package com.maendeleo.reliable_money_transfer

import java.time.Instant

/** The business request. The client creates one idempotency key for this logical transfer. */
data class TransferRequest(
    val clientReference: String,
    val sourceAccount: String,
    val destinationAccount: String,
    val amountMinor: Long,
    val currency: String,
)

enum class TransferStatus { PENDING, UNKNOWN_OUTCOME, SUCCEEDED, FAILED }

/** Durable record: it survives process death and lets a worker resume safely. */
data class TransferRecord(
    val idempotencyKey: String,
    val request: TransferRequest,
    val status: TransferStatus,
    val providerTransferId: String? = null,
    val providerMessage: String? = null,
    val attemptCount: Int = 0,
    val updatedAt: Instant = Instant.now(),
)

data class ProviderTransferResult(val providerTransferId: String, val message: String? = null)

