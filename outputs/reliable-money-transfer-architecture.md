# Reliable transfer architecture

The mobile client owns the user experience and a resumable local job. The bank/MNO backend owns authorization, the ledger, and the durable outbox. The external provider must implement idempotency server-side.

```mermaid
flowchart LR
  A[Mobile app\nKotlin / Compose] -->|HTTPS + OAuth2/DPoP| B[Transfer API\nKtor or Spring Boot]
  B --> C[(Transfer DB\nPostgreSQL)
  B --> D[Outbox worker\nKafka/SQS]
  D -->|POST /transfers\nIdempotency-Key| E[External payment API]
  E --> F[(Provider idempotency store)]
  D -->|GET status after unknown outcome| E
  B --> G[Risk / limits / AML]
  B --> H[Audit + observability\nOpenTelemetry / SIEM]
  A --> I[(Encrypted local job store\nRoom/SQLite)]
```

## Protocol and technology

- Transport: HTTPS/TLS 1.3, JSON REST, short connect/read timeouts.
- Auth: OAuth 2.0 access token plus device binding or DPoP where supported; never put secrets in the mobile binary.
- Submit: `POST /transfers` with `Idempotency-Key: <stable-key>`.
- Resolve: `GET /transfers/by-idempotency-key/<key>` or provider status query.
- Retry: retry timeouts, connection failures, and HTTP 5xx/429 after exponential backoff with jitter. Do not retry validation/auth errors.
- Exactly-once business effect: the provider stores the idempotency key with the final result and returns that result on duplicate POSTs. At-least-once delivery plus provider idempotency gives one money movement.
- Mobile durability: Room/SQLite job record with `PENDING`, `UNKNOWN_OUTCOME`, `SUCCEEDED`, and `FAILED`; WorkManager resumes jobs after process death.
- Backend reliability: transactional outbox, deduplication constraint on `idempotency_key`, ledger transaction, reconciliation, and dead-letter handling.

