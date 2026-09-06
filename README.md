# Bank Card Management API

REST API for managing bank cards: card lifecycle administration, cardholder self-service, and transfers between a user's own cards. Built with Java 21 and Spring Boot 3.2, secured with JWT, backed by PostgreSQL with Flyway migrations.

## Features

**Administrator**
- Create, update and delete cards
- Block and activate any card
- Register users
- View and search all cards

**Cardholder**
- View own cards with pagination and filtering (by status and last four digits)
- Request a block or unblock of an own card
- Transfer money between own cards
- View balances

## Security model

The security-relevant design decisions, which are the parts worth reading:

| Concern | Approach |
|---|---|
| Card number at rest | AES-256-GCM with a fresh random IV per row (`CardNumberCryptoConverter`), applied as a JPA `AttributeConverter` so the plaintext never reaches the database |
| Duplicate detection | Because GCM ciphertext is non-deterministic, equality lookups use a separate keyed HMAC-SHA256 column (`number_hash`) with a unique constraint — duplicates are detected without deterministic (and therefore weaker) encryption |
| Card number in responses | Never returned in full; `CardResponse` serializes it as `**** **** **** 1234` |
| Transfers | Both card rows are locked with `SELECT … FOR UPDATE` in a fixed id order before any balance is read, so concurrent transfers cannot overdraw a card and cannot deadlock. Amounts are limited to two decimals, matching the `NUMERIC(38,2)` column, so rounding cannot create money |
| Search by card number | A dedicated `last4` column, since the encrypted column cannot be queried. It exposes only what the masked representation already shows |
| Passwords | BCrypt hashed on registration, verified with `PasswordEncoder.matches`; never included in any response DTO |
| Request binding | Controllers bind to request DTOs, never to JPA entities, so clients cannot set `id`, `status`, `balance` or other server-controlled fields |
| Authorization | Role rules in `SecurityConfig` plus ownership checks in `CardService` — users can only reach their own cards, admins can reach any. Roles are stored per user; the configured administrator is created as an ordinary row on first start, so there is one credential path and admins can create further admins |
| Referential integrity | `card.holder_id` is a foreign key. The service rejects an unknown holder with 400 before the constraint is reached, and a PostgreSQL-backed test proves both halves, because the H2 schema used by the rest of the suite has no foreign keys |
| Secrets | Supplied via environment variables with no defaults; the application refuses to start if they are missing |

Card status changes go through an explicit state machine (`CardStateMachine`) rather than ad-hoc conditionals, so invalid transitions (for example activating an expired card) are rejected in one place.

## Tech stack

Java 21, Spring Boot 3.2.4, Spring Security, Spring Data JPA, PostgreSQL 16, Flyway, JWT (jjwt), springdoc-openapi, Docker Compose, JUnit 5 + MockMvc, H2 for tests.

## Running with Docker Compose

Requires Docker and Docker Compose.

```bash
cp .env.example .env     # then edit .env and set real secrets
docker compose up -d --build
```

The API starts on `http://localhost:8080`, PostgreSQL on `5432`. Flyway applies the migrations in `src/main/resources/db/migration` at startup. Database data is kept in the named volume `db-data` and survives restarts.

```bash
docker compose down      # stop
docker compose down -v   # stop and delete the database volume
```

### Configuration

All configuration is environment-driven (see `.env.example`):

| Variable | Required | Description |
|---|---|---|
| `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | defaults provided | PostgreSQL connection |
| `JWT_SECRET` | **yes** | JWT signing key, minimum 32 bytes |
| `CARD_ENCRYPTION_SECRET` | **yes** | Key for card-number encryption and the lookup HMAC |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | **yes** | Bootstrap administrator credentials |
| `JWT_EXPIRATION` | defaults to 24h | Token lifetime in milliseconds |

There are deliberately no defaults for the secrets: `docker compose` aborts and the application fails to start if they are unset, so an instance can never run with a key that is public in this repository. Generate them with `openssl rand -base64 48`.

Changing `CARD_ENCRYPTION_SECRET` makes existing encrypted card numbers unreadable — rotate it only together with a re-encryption of the data.

## Running tests

```bash
./mvnw test
```

Most tests run against in-memory H2 for fast feedback and need no setup; the PostgreSQL-specific tests below need a running Docker daemon. Each test method runs in a transaction that is rolled back, so the suite is isolated without rebuilding the Spring context between tests. The build requires JDK 21; `./mvnw` locates a JDK 21 automatically when one is installed, so setting `JAVA_HOME` by hand is usually unnecessary.

**Docker is required for the full suite.** H2 cannot show PostgreSQL-only behaviour — foreign keys, check constraints, unique constraints, row locking and `NUMERIC` rounding are all absent or different in the Hibernate-generated test schema. Those guarantees are covered by two classes that run against a disposable PostgreSQL 16 container started automatically by Testcontainers:

| Test | Covers |
|---|---|
| `PostgresSchemaIntegrationTest` | Flyway migrations build a schema the entities validate against; `holder_id` foreign key; card status check constraint; unique `number_hash`; unique username; `NUMERIC(38,2)` rounding |
| `PostgresLockingIntegrationTest` | `SELECT … FOR UPDATE` really blocks a competing transaction; `@Version` rejects a stale write; concurrent transfers conserve money |

The container is started once per JVM, shared by both classes, given a random port, and removed automatically when the run ends — it never touches a locally installed PostgreSQL. It uses the same image as `docker-compose.yml`, so tests and production run the same PostgreSQL version.

The tests fail rather than silently skip if Docker is unavailable, so a green build always means the PostgreSQL guarantees were actually checked. On Docker Engine 25+ this needs Testcontainers 1.21+ (pinned in `pom.xml`); older versions negotiate an API version that recent daemons reject.

## API

Interactive documentation is served by the running application at `http://localhost:8080/swagger-ui.html` (use **Authorize** with a token from `/auth/login`). A generated snapshot of the specification is committed at [`docs/openapi.yaml`](docs/openapi.yaml).

| Method | Endpoint | Access |
|---|---|---|
| `POST` | `/auth/login` | public |
| `POST` | `/users/register` | admin |
| `POST` | `/cards` | admin |
| `GET` | `/cards?page=&size=&status=&last4=` | authenticated (admins see all cards, users only their own) |
| `GET` | `/cards/{id}` | owner or admin |
| `PUT` | `/cards/{id}` | admin |
| `DELETE` | `/cards/{id}` | admin |
| `POST` | `/cards/transfer` | owner of both cards |
| `PATCH` | `/cards/{id}/block` | admin |
| `PATCH` | `/cards/{id}/activate` | admin |
| `PATCH` | `/cards/{id}/block-request` | owner or admin |
| `PATCH` | `/cards/{id}/unblock-request` | owner or admin |

Errors are returned as RFC 7807 `application/problem+json`, with per-field details for validation failures:

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "detail": "Request validation failed",
  "errors": { "number": "Card number must be exactly 16 digits" }
}
```

## Known limitations

Deliberate scope choices, not oversights:

- Card expiry is evaluated when a card is read rather than by a scheduled job, so a card that is never fetched keeps its stored status.
- JWTs cannot be revoked before they expire; there is no refresh-token flow, and a role change only takes effect once the current token expires.
- Listing returns a plain array rather than page metadata, so clients cannot see the total number of matches.
- User management is limited to registration; there is no endpoint to list, update or delete users.
