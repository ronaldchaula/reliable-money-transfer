package com.maendeleo.reliable_money_transfer


/** Replace with a transactionally updated SQL/Room/SQLite implementation. */
interface TransferRepository {
    fun findByClientReference(clientReference: String): TransferRecord?
    fun findByIdempotencyKey(idempotencyKey: String): TransferRecord?
    fun save(record: TransferRecord)
}

