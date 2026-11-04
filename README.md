# Reliable money transfer client

This reference implementation shows how a bank or MNO mobile app can submit a payment request to an unreliable external REST API without accidentally charging the customer twice.

## The important guarantee

The client can retry transport failures, timeouts, and retryable server errors. It cannot know whether a timed-out request reached the external server, so every attempt carries the same `Idempotency-Key`. The external API must persist that key and return the original result for duplicate submissions.

When the outcome is unknown, the client stores `UNKNOWN_OUTCOME` and keeps polling the provider's status endpoint. It does not create a new transfer or generate a new key.

## Run

```bash
./gradlew test
./gradlew run
```

## Root package files

All reference classes use the project's existing root package, `com.maendeleo.reliable_money_transfer`. Responsibilities are separated by file: `TransferModels.kt`, `ProviderClient.kt`, `ExternalProviderRestClient.kt`, `TransferRepository.kt`, `RetryPolicy.kt`, and `TransferService.kt`.

## Production notes

The in-memory repository and fake provider are intentionally small examples. A production app should replace them with encrypted local storage and a server-side ledger/outbox. The provider contract should support `POST /transfers` with `Idempotency-Key` and `GET /transfers/{id}` or `GET /transfers/by-idempotency-key/{key}`. Use TLS, certificate pinning where appropriate, device/user authentication, server-side authorization, audit logs, rate limits, and reconciliation.

