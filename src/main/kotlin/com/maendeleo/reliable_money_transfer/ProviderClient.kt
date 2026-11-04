package com.maendeleo.reliable_money_transfer


/** REST adapter. HTTP details stay outside the orchestration layer. */
interface ProviderClient {
    /** POST /transfers with the same idempotency key for every retry. */
    fun submit(request: TransferRequest, idempotencyKey: String): ProviderTransferResult

    /** Read-after-unknown check. This must also be safe and idempotent. */
    fun findByIdempotencyKey(idempotencyKey: String): ProviderTransferResult?
}

sealed class ProviderFailure(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    class Timeout(cause: Throwable? = null) : ProviderFailure("Provider request timed out", cause)
    class Network(cause: Throwable? = null) : ProviderFailure("Provider network failure", cause)
    class Server(val statusCode: Int, cause: Throwable? = null) : ProviderFailure("Provider returned $statusCode", cause)
    class Client(val statusCode: Int, cause: Throwable? = null) : ProviderFailure("Provider rejected request with $statusCode", cause)
}

