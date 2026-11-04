package com.maendeleo.reliable_money_transfer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TransferServiceTest {
    @Test
    fun `timeout followed by retry uses one idempotency key and succeeds`() {
        val provider = RecordingProvider(ProviderFailure.Timeout())
        val result = TransferService(InMemoryTransferRepository(), provider, sleep = {}).send(request())
        assertEquals(TransferStatus.SUCCEEDED, result.status)
        assertEquals(2, provider.keys.size)
        assertEquals(provider.keys.first(), provider.keys.last())
    }

    @Test
    fun `client error stops without retry`() {
        val provider = RecordingProvider(ProviderFailure.Client(400))
        val result = TransferService(InMemoryTransferRepository(), provider, sleep = {}).send(request())
        assertEquals(TransferStatus.FAILED, result.status)
        assertEquals(1, provider.keys.size)
    }

    @Test
    fun `duplicate logical request returns the original result`() {
        val provider = RecordingProvider()
        val service = TransferService(InMemoryTransferRepository(), provider, sleep = {})
        val first = service.send(request())
        val second = service.send(request())
        assertEquals(first, second)
        assertEquals(1, provider.keys.size)
        assertNotEquals(null, first.providerTransferId)
    }

    private fun request() = TransferRequest("client-ref", "source", "destination", 100, "TZS")
}

private class RecordingProvider(private val failFirstWith: ProviderFailure? = null) : ProviderClient {
    val keys = mutableListOf<String>()
    private var calls = 0
    override fun submit(request: TransferRequest, idempotencyKey: String): ProviderTransferResult {
        keys += idempotencyKey
        calls++
        if (calls == 1 && failFirstWith != null) throw failFirstWith
        return ProviderTransferResult("provider-${request.clientReference}")
    }
    override fun findByIdempotencyKey(idempotencyKey: String): ProviderTransferResult? = null
}

