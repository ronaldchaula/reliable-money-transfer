package com.maendeleo.reliable_money_transfer


private class FlakyProvider : ProviderClient {
    private var postCalls = 0
    private var committed: ProviderTransferResult? = null
    override fun submit(request: TransferRequest, idempotencyKey: String): ProviderTransferResult {
        postCalls++
        if (postCalls == 1) throw ProviderFailure.Timeout() // the server may still have received it
        return committed ?: ProviderTransferResult("provider-${request.clientReference}").also { committed = it }
    }
    override fun findByIdempotencyKey(idempotencyKey: String): ProviderTransferResult? = committed
}

fun main() {
    val service = TransferService(InMemoryTransferRepository(), FlakyProvider(), sleep = { })
    println(service.send(TransferRequest("order-123", "wallet-A", "wallet-B", 2_500, "TZS")))
}

