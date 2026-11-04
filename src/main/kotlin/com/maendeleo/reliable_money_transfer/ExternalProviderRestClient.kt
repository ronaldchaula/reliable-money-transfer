package com.maendeleo.reliable_money_transfer

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/** Spring REST adapter. It preserves the stable idempotency key on every POST. */
@Component
class ExternalProviderRestClient(
    builder: RestClient.Builder,
    @Value("\${provider.base-url:http://localhost:8081}") baseUrl: String,
) : ProviderClient {
    private val client = builder.baseUrl(baseUrl).build()

    override fun submit(request: TransferRequest, idempotencyKey: String): ProviderTransferResult = try {
        client.post()
            .uri("/transfers")
            // This provider-specific header is not defined by Spring's HttpHeaders constants.
            .header("Idempotency-Key", idempotencyKey)
            .body(request)
            .retrieve()
            .body(ProviderTransferResult::class.java)
            ?: throw ProviderFailure.Network()
    } catch (error: ProviderFailure) {
        throw error
    } catch (error: org.springframework.web.client.ResourceAccessException) {
        throw ProviderFailure.Network(error)
    } catch (error: org.springframework.web.client.HttpServerErrorException) {
        throw ProviderFailure.Server(error.statusCode.value(), error)
    } catch (error: org.springframework.web.client.HttpClientErrorException) {
        // 429 means temporary throttling, so the service should back off and retry it.
        if (error.statusCode.value() == 429) throw ProviderFailure.Server(429, error)
        throw ProviderFailure.Client(error.statusCode.value(), error)
    }

    override fun findByIdempotencyKey(idempotencyKey: String): ProviderTransferResult? = try {
        client.get()
            .uri("/transfers/by-idempotency-key/{key}", idempotencyKey)
            .retrieve()
            .body(ProviderTransferResult::class.java)
    } catch (_: org.springframework.web.client.HttpClientErrorException.NotFound) {
        null
    } catch (error: org.springframework.web.client.ResourceAccessException) {
        throw ProviderFailure.Network(error)
    } catch (error: org.springframework.web.client.HttpServerErrorException) {
        throw ProviderFailure.Server(error.statusCode.value(), error)
    } catch (error: org.springframework.web.client.HttpClientErrorException) {
        throw ProviderFailure.Client(error.statusCode.value(), error)
    }
}

