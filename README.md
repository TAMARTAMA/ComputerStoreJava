# Computer Store Management API

Spring Boot backend for managing a computer store. The MySQL domain schema, the JPA persistence layer, and stateless JWT authentication are in place; the product and order endpoints are not built yet.

## Stack

- Java 21
- Spring Boot 4.1.1
- Maven (Wrapper)
- MySQL 8.4 via Docker Compose
- Flyway
- Spring Security 7 with HS256 JWTs (Nimbus JOSE via `spring-security-oauth2-jose`)
- Lombok
- Testcontainers (persistence and authentication tests)

## Prerequisites

- JDK 21
- Docker Desktop (or Docker Engine + Compose)
- Git

## Setup

1. Clone the repository and switch to this branch:

```powershell
git checkout feature/spring-boot-api
```

2. Copy the environment template and adjust values if needed:

```powershell
Copy-Item .env.example .env
```

3. Generate a JWT signing secret and put it in `.env` as `JWT_SECRET`. There is no fallback in
   source or in `.env.example`, so the application refuses to start without one. It must be at
   least 32 characters (256 bits) for HS256:

```powershell
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
```

   Or, with OpenSSL available:

```powershell
openssl rand -base64 48
```

4. Confirm Java 21 is active:

```powershell
java -version
```

## Database (Docker)

Validate Compose configuration:

```powershell
docker compose config
```

Start MySQL in the background:

```powershell
docker compose up -d
```

Wait until the container reports healthy:

```powershell
docker compose ps
```

Stop MySQL when finished:

```powershell
docker compose down
```

## Run the API

`.env` is consumed by Docker Compose, not by Spring Boot, so export the application variables in
the shell that starts the API:

```powershell
$env:JWT_SECRET = "<the secret you generated>"
$env:JWT_EXPIRATION = "PT1H"
.\mvnw.cmd spring-boot:run
```

Health check (proves the server is up):

```powershell
curl http://localhost:8080/api/health
```

Expected JSON shape:

```json
{
  "status": "UP",
  "application": "Computer Store Management API",
  "timestamp": "..."
}
```

## Authentication

The API is stateless: there is no session and no login page. Clients register or log in once,
then send the returned token as `Authorization: Bearer <token>` on every later request.

| Endpoint | Access |
| --- | --- |
| `GET /api/health` | public |
| `POST /api/auth/register` | public, creates a `CUSTOMER` account plus its customer profile |
| `POST /api/auth/login` | public, returns a signed JWT |
| `GET /api/auth/me` | requires a valid JWT |
| everything else under `/api/**` | requires a valid JWT |

Register and receive a token:

```powershell
curl -X POST http://localhost:8080/api/auth/register `
  -H "Content-Type: application/json" `
  -d '{"email":"dana@example.com","password":"Str0ng-Passw0rd!","firstName":"Dana","lastName":"Levi"}'
```

Call a protected endpoint:

```powershell
curl http://localhost:8080/api/auth/me -H "Authorization: Bearer <accessToken>"
```

Notes:

- Passwords are stored only as BCrypt hashes (strength 12) and never appear in a response.
- Login answers every failure with the same `401` body, so it cannot be used to discover which
  addresses are registered.
- Unauthenticated and forbidden requests return JSON `401` and `403` envelopes, never HTML.
- CSRF protection is disabled because the API accepts only bearer tokens, which a browser never
  attaches automatically.

### Development admin account

There is no admin in any migration and no admin password in the repository. To create one locally,
run the application with the `local` profile and supply both variables:

```powershell
$env:LOCAL_ADMIN_EMAIL = "admin@example.com"
$env:LOCAL_ADMIN_PASSWORD = "<a local-only password>"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

The bootstrap is idempotent: it skips silently when either variable is empty and leaves an existing
account with that email untouched.

## Domain schema

Flyway owns the schema (`V1__baseline.sql`, `V2__create_core_schema.sql`). The core tables are:

- `user_accounts` — login identity with a unique email and an `ADMIN`/`CUSTOMER` role
- `customers` — customer profile, one-to-one with a user account (admins need no profile)
- `products` — unique SKU, `DECIMAL` price, stock quantity, `HARDWARE`/`SOFTWARE` category, and a `version` column for optimistic locking
- `purchase_orders` — belongs to a customer, with `PENDING`/`COMPLETED`/`CANCELLED` status and a `DECIMAL` total
- `order_items` — order lines referencing a product, with a positive quantity and a `DECIMAL` unit price

Money is always `BigDecimal`/`DECIMAL`, timestamps are stored as UTC `DATETIME(6)`, and Spring Data JPA
repositories exist for the four aggregates.

## Tests

```powershell
.\mvnw.cmd test
```

Persistence and authentication tests start a throwaway MySQL 8.4 container via Testcontainers, so
Docker must be running. They never touch the Compose database. Surefire injects a throwaway
`JWT_SECRET` into the test JVM, so no local secret is needed to run the suite.

## Project layout

```text
src/main/java/com/tamar/computerstore/
  config/
  controller/
  dto/
  entity/
  exception/
  repository/
  security/
  service/
src/main/resources/
  application.yml
  db/migration/
docker-compose.yml
.env.example
pom.xml
```

## Configuration notes

- MySQL host, port, database, username, and password are read from environment variables (`MYSQL_*`).
- `docker-compose.yml` supplies local defaults via `.env` / Compose substitution.
- `.env` is gitignored — do not commit real secrets.
- Hibernate `ddl-auto` is `none`; Flyway owns schema evolution.
- `JWT_SECRET` and `JWT_EXPIRATION` are the only sources of JWT configuration; `JWT_SECRET` has no
  default and is validated at startup.
- `LOCAL_ADMIN_EMAIL` and `LOCAL_ADMIN_PASSWORD` apply only under the `local` profile.
