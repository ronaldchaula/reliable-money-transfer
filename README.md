# Reliable Money Transfer

Reference Spring Boot/Kotlin application for sending money-transfer requests to an unreliable external REST API.

The solution uses:

- Stable `clientReference` to prevent duplicate logical requests from the mobile app.
- Stable `Idempotency-Key` for every provider retry.
- Exponential backoff with jitter between attempts.
- `UNKNOWN_OUTCOME` when a timeout or network failure means the provider may have processed the transfer.
- Provider status lookup before retrying an unknown request.
- A local Python mock provider for testing on port `8081`.

All Kotlin classes use the project root package:

```text
com.maendeleo.reliable_money_transfer
```

## Requirements

- Java 25
- Gradle 9.x through the project wrapper
- Python 3 for the local mock provider

Verify Java:

```bash
java -version
javac -version
```

If Java 25 is installed but not selected on Ubuntu:

```bash
sudo update-alternatives --config java
sudo update-alternatives --config javac
```

The project wrapper is configured in `gradle/wrapper/gradle-wrapper.properties`. Use `./gradlew`, not the system `gradle` command.

## Build and test

```bash
./gradlew clean test
```

If the wrapper is not executable:

```bash
chmod +x gradlew
```

## Run the application

Start the Spring Boot application in one terminal:

```bash
./gradlew bootRun
```

The application listens on `http://localhost:8080`.

## Run the external mock provider

The transfer application calls the provider at `http://localhost:8081` by default. Start the mock provider in a second terminal:

```bash
cd ~/work/Pesalock/projects/Ndani/reliable-money-transfer
python3 mock_provider.py
```

The mock provider supports:

```text
POST /transfers
GET  /transfers/by-idempotency-key/{key}
```

It stores every `Idempotency-Key` in memory and returns the same provider result for duplicate requests.

### Simulate provider failures

To make the first provider `POST` return HTTP `503`, then allow the next retry to succeed:

```bash
Ctrl+C
MOCK_FAIL_FIRST=true python3 mock_provider.py
```

Submit a new transfer using a new `clientReference`:

```bash
curl -X POST http://localhost:8080/api/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "clientReference": "failure-test-001",
    "sourceAccount": "wallet-A",
    "destinationAccount": "wallet-B",
    "amountMinor": 2500,
    "currency": "TZS"
  }'
```

Expected behavior:

1. The mock returns `503` on the first provider request.
2. The application waits according to the retry policy.
3. The application retries with the same `Idempotency-Key`.
4. The mock accepts the retry and the application returns `SUCCEEDED`.

To make every provider `POST` fail with HTTP `503`:

```bash
Ctrl+C
MOCK_ALWAYS_FAIL=true python3 mock_provider.py
```

The application will retry until the configured maximum attempt count and then return the current non-success state. Use `Ctrl+C` to stop the mock provider and restart it normally:

```bash
Ctrl+C
python3 mock_provider.py
```

Use a new `clientReference` for a new test. Reusing a reference returns the existing logical transfer record.

## Check the services

Check the provider:

```bash
curl -v http://localhost:8081/transfers/by-idempotency-key/test-key
```

A `404` means the provider is running but the transfer does not exist. A connection-refused error means nothing is listening on port `8081`.

Check the application:

```bash
curl http://localhost:8080/api/transfers/
```

Expected response:

```json
{"service":"reliable-money-transfer","status":"UP"}
```

## Submit a transfer

Use a new `clientReference` for a new logical transfer:

```bash
curl -X POST http://localhost:8080/api/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "clientReference": "order-124",
    "sourceAccount": "wallet-A",
    "destinationAccount": "wallet-B",
    "amountMinor": 2500,
    "currency": "TZS"
  }'
```

Successful response:

```json
{
  "status": "SUCCEEDED",
  "providerTransferId": "mock-<idempotency-key>",
  "message": "accepted by local mock provider"
}
```

## Why `UNKNOWN_OUTCOME` is returned

If the provider times out or the network connection fails, the application cannot know whether the provider already processed the transfer. It therefore returns or stores:

```json
{"status":"UNKNOWN_OUTCOME","providerTransferId":null,"message":"Provider network failure"}
```

This is intentional. The application keeps the same idempotency key and checks the provider status before submitting again. It must never generate a new key for the same logical transfer.

## Duplicate request behavior

Submitting the same `clientReference` again returns the existing transfer record instead of creating a second transfer. For testing a new transfer, change the reference, for example `order-125`.

## Port problems

If port `8080` is already in use:

```bash
sudo lsof -i :8080
kill <PID>
```

Or run the application on another port:

```bash
./gradlew bootRun --args='--server.port=8082'
```

If port `8081` is already in use, run the mock on another port and point the application to it:

```bash
MOCK_PROVIDER_PORT=8082 python3 mock_provider.py
./gradlew bootRun --args='--provider.base-url=http://localhost:8082'
```

## Git setup

```bash
git init
git branch -M production
git remote add origin https://github.com/ronaldchaula/reliable-money-transfer.git
git add .
git commit -m "Implement reliable money transfer flow"
git push -u origin production
```

The first push must include a commit. `git fetch origin production` fails until the remote branch exists.

## Production considerations

The mock provider and in-memory repository are for local testing only. Production should use a durable database with a unique constraint, a transactional outbox, provider-side idempotency storage, HTTPS/TLS, authentication, authorization, audit logs, reconciliation, limits, monitoring, and encrypted mobile persistence.

