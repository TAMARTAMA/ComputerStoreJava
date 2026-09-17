# Computer Store Management API

Spring Boot 4 backend for managing a computer store: MySQL persistence (Flyway), stateless JWT
security, product catalog, transactional orders, OpenAPI/Swagger, Actuator health probes, and CI.

See [ARCHITECTURE.md](ARCHITECTURE.md) for layering, JWT flow, and order transactions.

**Demo data is fictional.** Seeded names, emails, phones, and addresses are reserved demo values
(for example `noa.levi@example.test`). They are not real people or credentials and never run unless
you explicitly enable them under the `local` profile.

## Stack

- Java 21 / Spring Boot 4.1.1
- Maven Wrapper
- MySQL 8.4 (Docker Compose)
- Flyway + Spring Data JPA
- Spring Security 7 (HS256 JWT via Nimbus / `spring-security-oauth2-jose`)
- springdoc-openapi 3.1.1 (Swagger UI)
- Spring Boot Actuator (health + probes only)
- Testcontainers + GitHub Actions CI

## Architecture and request flow

```text
Client → Controllers → Services (@PreAuthorize, @Transactional) → Repositories → MySQL
         ↑
   Bearer JWT (JwtAuthenticationFilter)
```

1. Register or login to receive a JWT.
2. Send `Authorization: Bearer <token>` on protected routes.
3. Services enforce roles and business rules; repositories persist; Flyway owns schema.

## Prerequisites

- JDK 21
- Docker Desktop (or Docker Engine + Compose)
- Git

## Setup

```powershell
git checkout feature/spring-boot-api
Copy-Item .env.example .env
```

Generate a JWT signing secret (required; no fallback) and set `JWT_SECRET` in `.env`
(at least 32 characters for HS256):

```powershell
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
# or: openssl rand -base64 48
java -version
```

## Database (Docker)

```powershell
docker compose config
docker compose up -d          # MySQL only (default)
docker compose ps
docker compose down
```

## Run the API

Spring Boot imports optional `.env` via `spring.config.import`. Never commit a real `.env`.

```powershell
.\mvnw.cmd spring-boot:run
```

### Local fictional demo data

Off by default. Activates only when **all** are true: profile `local`,
`APP_DEMO_DATA_ENABLED=true`, and `DEMO_CUSTOMER_PASSWORD` set (≥ 12 chars). All seeded people and
contact data are **fictional**.

```env
APP_DEMO_DATA_ENABLED=true
DEMO_CUSTOMER_PASSWORD=<strong-local-only-password>
LOCAL_ADMIN_EMAIL=admin@example.com
LOCAL_ADMIN_PASSWORD=<strong-local-only-password>
```

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

## Swagger / OpenAPI

| Resource | URL |
| --- | --- |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |

1. Call **POST /api/auth/login** (or register) from Swagger or curl.
2. Click **Authorize** and paste the `accessToken` (Bearer JWT once for the session).
3. Try protected product/order endpoints.

Only OpenAPI/Swagger metadata paths are public; business `/api/**` routes stay protected.

## Health URLs

| Endpoint | Access | Notes |
| --- | --- | --- |
| `GET /api/health` | public | Legacy JSON health (backward compatible) |
| `GET /actuator/health` | public | Actuator aggregate health (`show-details: never`) |
| `GET /actuator/health/liveness` | public | Liveness probe |
| `GET /actuator/health/readiness` | public | Readiness probe |

No other Actuator endpoints are exposed (no `env`, `beans`, `configprops`, `heapdump`, metrics that
require secrets, etc.).

```powershell
curl http://localhost:8080/api/health
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/health/liveness
curl http://localhost:8080/actuator/health/readiness
```

## API authorization matrix

| Endpoint | Access |
| --- | --- |
| `GET /api/health` | public |
| `GET /actuator/health`, `/liveness`, `/readiness` | public |
| `GET /v3/api-docs`, `/swagger-ui/**` | public (docs only) |
| `POST /api/auth/register` | public → creates `CUSTOMER` |
| `POST /api/auth/login` | public → JWT |
| `GET /api/auth/me` | any valid JWT |
| `GET /api/products`, `GET /api/products/{id}` | `CUSTOMER`, `ADMIN` |
| `POST` / `PUT` / `DELETE` `/api/products` | `ADMIN` |
| `POST /api/orders`, `GET /api/orders/me` | `CUSTOMER` |
| `GET /api/orders/{id}` | `CUSTOMER` (own only; else 404), `ADMIN` |
| `GET /api/orders`, `PATCH /api/orders/{id}/status` | `ADMIN` |

### Quick auth examples

```powershell
curl -X POST http://localhost:8080/api/auth/register `
  -H "Content-Type: application/json" `
  -d '{"email":"dana@example.com","password":"Str0ng-Passw0rd!","firstName":"Dana","lastName":"Levi"}'

curl http://localhost:8080/api/auth/me -H "Authorization: Bearer <accessToken>"
```

Notes: passwords are BCrypt (strength 12); login failures share one `401` body; CSRF is off because
only bearer tokens are used.

### Development admin

No admin in migrations. With profile `local` and `LOCAL_ADMIN_EMAIL` / `LOCAL_ADMIN_PASSWORD` set,
bootstrap creates an ADMIN if missing (idempotent).

## Products and orders

Product reads require authentication; writes are ADMIN with optimistic locking (`version` → `409` on
stale). Orders are one `@Transactional` placement: server prices lines, decrements stock, starts
`PENDING`. Allowed transitions: `PENDING → COMPLETED` or `PENDING → CANCELLED` (cancel restores
stock). See [ARCHITECTURE.md](ARCHITECTURE.md).

## Build JAR and Docker image

### Executable JAR

```powershell
.\mvnw.cmd -DskipTests package
java -jar target\computerstore-0.0.1-SNAPSHOT.jar
```

Provide `JWT_SECRET` and MySQL settings via environment or `.env`.

### Docker image (no secrets in the image)

```powershell
docker build -t computerstore-api:local .
docker run --rm -p 8080:8080 `
  -e JWT_SECRET=<your-secret> `
  -e MYSQL_HOST=host.docker.internal `
  -e MYSQL_USER=computerstore `
  -e MYSQL_PASSWORD=computerstore `
  -e MYSQL_DATABASE=computerstore `
  computerstore-api:local
```

### Optional API + MySQL via Compose profile

Default Compose remains MySQL-only. To also run the API container (secrets from `.env`):

```powershell
# Ensure JWT_SECRET is set in .env first
docker compose --profile api up -d --build
```

## Tests and CI

```powershell
.\mvnw.cmd clean verify
```

Integration tests start throwaway MySQL 8.4 via Testcontainers (Docker required). Surefire injects a
throwaway `JWT_SECRET`; no local `.env` is required for the suite.

GitHub Actions (`.github/workflows/ci.yml`) runs on every push and pull request: JDK 21, Maven
dependency cache, `./mvnw clean verify` on `ubuntu-latest` (runner Docker for Testcontainers). No
production credentials or external services.

## Domain schema

Flyway: `V1__baseline.sql`, `V2__create_core_schema.sql`.

- `user_accounts` — email + `ADMIN` / `CUSTOMER`
- `customers` — profile (1:1 with user; admins need none)
- `products` — SKU, price, stock, category, optimistic `version`
- `purchase_orders` — customer, status, total
- `order_items` — product, quantity, unit price

## Project layout

```text
src/main/java/com/tamar/computerstore/
  config/          # security, OpenAPI, demo bootstrap
  controller/
  dto/
  entity/
  exception/
  mapper/
  repository/
  security/
  service/
  validation/
src/main/resources/
  application.yml
  db/migration/
.github/workflows/ci.yml
Dockerfile
docker-compose.yml
ARCHITECTURE.md
.env.example
pom.xml
```

## Configuration notes

- `MYSQL_*`, `JWT_SECRET`, `JWT_EXPIRATION` from environment / optional `.env`
- Hibernate `ddl-auto: none`; Flyway owns schema
- Actuator: `management.endpoints.web.exposure.include=health` only
- `LOCAL_ADMIN_*`, `APP_DEMO_DATA_ENABLED`, `DEMO_CUSTOMER_PASSWORD` apply only under `local`
