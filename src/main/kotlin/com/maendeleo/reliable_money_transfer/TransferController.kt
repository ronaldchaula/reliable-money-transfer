package com.maendeleo.reliable_money_transfer

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** HTTP API for the mobile client. The service owns retry and idempotency behavior. */
@RestController
@RequestMapping("/api/transfers")
class TransferController(private val transferService: TransferService) {

    /** Simple endpoint for checking that the application is running. */
    @GetMapping("/", "")
    fun health() = mapOf(
        "service" to "reliable-money-transfer",
        "status" to "UP",
    )

    /**
     * Submit one logical transfer. Reusing the same clientReference returns the existing result,
     * which protects against double taps and mobile retries.
     */
    @PostMapping
    fun send(@RequestBody request: TransferRequest): ResponseEntity<TransferResult> {
        val result = transferService.send(request)
        val status = when (result.status) {
            TransferStatus.SUCCEEDED -> HttpStatus.OK
            TransferStatus.FAILED -> HttpStatus.UNPROCESSABLE_ENTITY
            TransferStatus.PENDING, TransferStatus.UNKNOWN_OUTCOME -> HttpStatus.ACCEPTED
        }
        return ResponseEntity.status(status).body(result)
    }
}

