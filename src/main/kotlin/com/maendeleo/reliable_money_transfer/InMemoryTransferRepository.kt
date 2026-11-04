package com.maendeleo.reliable_money_transfer

import java.util.concurrent.ConcurrentHashMap

/** Demo repository. Production code should encrypt and durably commit this state. */
class InMemoryTransferRepository : TransferRepository {
    private val records = ConcurrentHashMap<String, TransferRecord>()
    override fun findByClientReference(clientReference: String): TransferRecord? = records.values.firstOrNull { it.request.clientReference == clientReference }
    override fun findByIdempotencyKey(idempotencyKey: String): TransferRecord? = records[idempotencyKey]
    override fun save(record: TransferRecord) { records[record.idempotencyKey] = record }
}

