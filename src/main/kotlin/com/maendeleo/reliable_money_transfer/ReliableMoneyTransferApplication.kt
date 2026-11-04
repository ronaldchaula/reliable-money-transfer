package com.maendeleo.reliable_money_transfer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.client.RestClient

@SpringBootApplication
class ReliableMoneyTransferApplication {
    /** Wires the reliable transfer service into the existing Spring application. */
    @Bean
    fun transferRepository(): TransferRepository = InMemoryTransferRepository()

    @Bean
    fun retryPolicy(): RetryPolicy = RetryPolicy()

    /** Provides the HTTP client builder used by ExternalProviderRestClient. */
    @Bean
    fun restClientBuilder(): RestClient.Builder = RestClient.builder()

    @Bean
    fun transferService(
        repository: TransferRepository,
        provider: ProviderClient,
        policy: RetryPolicy,
    ): TransferService = TransferService(repository, provider, policy)
}

fun main(args: Array<String>) {
	runApplication<ReliableMoneyTransferApplication>(*args)
}
